package tech.testsys.infra.database.api.persistence.adapter

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.hibernate.engine.spi.SessionImplementor
import org.springframework.data.repository.findByIdOrNull
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.EntityRepository
import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.AggregateVersionTracker
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository
import tech.testsys.infra.database.internal.utils.findAllInChunks
import tech.testsys.infra.database.internal.utils.requireById
import tech.testsys.infra.database.internal.utils.requireId
import tech.testsys.infra.database.internal.utils.requireVersion
import tech.testsys.infra.database.internal.utils.touchAggregateRoot

/**
 * Base of [EntityRepository] adapters whose writes use [touchRoot] and removals use [removeRoot].
 * Override all overloads together when changing [Transactional] settings (such as [Propagation.REQUIRES_NEW]),
 * because Spring AOP does not intercept self-invocation.
 *
 * @param Data the data type a new entity is created from.
 * @param Id the id type of the entity.
 * @param Entity the domain entity type.
 * @param JpaEntity the JPA entity type the entity is stored as.
 * @property jpaEntityRepository the repository of [JpaEntity] rows.
 * @since %CURRENT_VERSION%
 */
@Suppress("CallBeanMethodFromSameClass")
@InternalDatabaseApi
abstract class AbstractPersistenceAdapter<Data, Id : DomainId, Entity : DomainEntity<Id>, JpaEntity : SnowflakeJpaEntity>(
    protected val jpaEntityRepository: SnowflakeJpaEntityRepository<JpaEntity>,
) : EntityRepository<Data, Id, Entity> {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Transactional(readOnly = true)
    override fun findById(id: Id) = jpaEntityRepository.findByIdOrNull(id.value)?.let { assemble(it) }

    @Transactional(readOnly = true)
    override fun findByIds(ids: List<Id>) = assembleAll(jpaEntityRepository.findAllById(ids.map { it.value }))

    @Transactional(readOnly = true)
    override fun load(field: LazyEntity<Id, Entity>) = findById(field.id).requireById(field.id)

    @Transactional(readOnly = true)
    override fun load(list: LazyEntityList<Id, Entity>): List<Entity> {
        val entities = findByIds(list.ids)
        val foundIds = entities.map { it.id.value }.toSet()
        val missingIds = list.ids.filter { it.value !in foundIds }
        require(missingIds.isEmpty()) { "Entities not found by ids=${missingIds.map { it.value }}" }
        return entities
    }

    @Transactional
    override fun save(dataList: List<Data>) = dataList.map { save(it) }

    @Transactional
    override fun update(entityList: List<Entity>) = entityList.map { update(it) }

    @Transactional
    override fun removeById(id: Id) = removeRoot(id, expectedVersion = null)

    @Transactional
    override fun removeByIds(ids: List<Id>) = ids.forEach(::removeById)

    @Transactional
    override fun remove(entity: Entity) = removeRoot(entity.id, entity.requireVersion())

    @Transactional
    override fun remove(entityList: List<Entity>) = entityList.forEach { entity -> remove(entity) }

    /**
     * Removes the aggregate whose root row is [id] if the row exists: increments the root version through [touchRoot],
     * checking [expectedVersion] if the caller has a version token, and deletes the row. An adapter whose aggregate
     * has parts or writes into other aggregates overrides this method, not the public removal methods.
     */
    protected open fun removeRoot(id: Id, expectedVersion: Long?) {
        jpaEntityRepository.findByIdOrNull(id.value) ?: return
        jpaEntityRepository.delete(touchRoot(jpaEntityRepository, id.value, expectedVersion, changesRootData = true))
    }

    /**
     * Assembles [Entity] objects from [rows] in the order of [rows], reading related rows for the whole list at once.
     * An override that delegates to [assemble] must also override [assemble], otherwise the two calls recurse.
     */
    protected abstract fun assembleAll(rows: List<JpaEntity>): List<Entity>

    /**
     * Assembles an [Entity] from its [jpaEntity] row through [assembleAll].
     */
    protected open fun assemble(jpaEntity: JpaEntity): Entity = assembleAll(listOf(jpaEntity)).single()

    /**
     * Registers a write to the aggregate whose root row [id] is stored in [rootRepository] and returns the managed
     * root with the version stored in the database. Checks [expectedVersion] if the caller has a version token,
     * runs [writeRoot] to write the root's own columns, then increments the root version at once, unless this
     * transaction has already done it. Pass [changesRootData] when the write changes the aggregate's data.
     *
     * A token is accepted if it equals the current version, or the version loaded by this transaction while the
     * earlier writes of the transaction were guard writes without data changes; otherwise the call throws
     * [ObjectOptimisticLockingFailureException].
     */
    @Suppress("VERBOSE_DOC")
    protected fun <Root : SnowflakeJpaEntity> touchRoot(
        rootRepository: SnowflakeJpaEntityRepository<Root>,
        id: Long,
        expectedVersion: Long? = null,
        changesRootData: Boolean = false,
        writeRoot: (Root) -> Unit = {},
    ): Root {
        return entityManager.touchAggregateRoot(rootRepository, id, expectedVersion, changesRootData, writeRoot)
    }

    /**
     * Loads [ids] through [rootRepository] in batches and guard-writes roots not yet incremented in this transaction.
     * [incrementVersions] updates their versions in one statement, then the managed rows receive those versions.
     */
    protected fun <Root : SnowflakeJpaEntity> touchRoots(
        rootRepository: SnowflakeJpaEntityRepository<Root>,
        ids: Collection<Long>,
        incrementVersions: (List<Long>) -> Int,
    ) {
        if (ids.isEmpty()) return

        entityManager.flush()
        val rows = findAllInChunks(ids = ids.sorted(), find = rootRepository::findAllById)
        val pending = rows.filter { root ->
            val state = AggregateVersionTracker.state(root)
            state.loadedVersion != null && root.version == state.loadedVersion
        }
        if (pending.isEmpty()) return

        incrementVersions(pending.map { root -> root.requireId() })
        val persistenceContext = entityManager.unwrap(SessionImplementor::class.java).persistenceContextInternal
        pending.forEach { root -> persistenceContext.getEntry(root).forceLocked(root, root.version + 1) }
    }

    /**
     * Updates the own root row [id] through [touchRoot] with [expectedVersion]: writes the row [buildRow] makes from
     * the current one, carrying over the version the current row has in this transaction.
     */
    protected fun updateRoot(id: Long, expectedVersion: Long, buildRow: (JpaEntity) -> JpaEntity): JpaEntity =
        touchRoot(jpaEntityRepository, id, expectedVersion, changesRootData = true) { current ->
            jpaEntityRepository.saveAndFlush(buildRow(current).apply { version = current.version })
        }
}
