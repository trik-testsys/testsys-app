package tech.testsys.infra.database.api.persistence.adapter

import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
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
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository
import tech.testsys.infra.database.internal.utils.findByIdOrError
import tech.testsys.infra.database.internal.utils.requireById
import tech.testsys.infra.database.internal.utils.requireVersion

/**
 * Base of persistence adapters: implements finding, loading, removing and the list overloads of [EntityRepository];
 * subclasses provide [save], [update] and [assembleAll] and pass every write to an aggregate through [touchRoot].
 * Every removal goes through [removeRoot] row by row, and [remove] also checks the version token of the entity.
 * Overloads that need non-default [Transactional] settings (e.g. [Propagation.REQUIRES_NEW]) must be overridden
 * together (Spring AOP self-invocation).
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
        val root = rootRepository.findByIdOrError(id)
        val isTokenAccepted = expectedVersion == null || expectedVersion == root.version ||
            (expectedVersion == root.loadedVersion && !root.hasDataChanges)
        if (!isTokenAccepted) throw ObjectOptimisticLockingFailureException(root.javaClass, id)

        writeRoot(root)
        if (root.loadedVersion != null && root.version == root.loadedVersion) {
            entityManager.lock(root, LockModeType.PESSIMISTIC_FORCE_INCREMENT)
        }
        if (changesRootData) root.hasDataChanges = true
        return root
    }

    /**
     * Guard write to many roots of [rootClass] at once: increments the versions of the rows [ids] whose version this
     * transaction has not incremented yet with one [incrementVersions] statement, and moves the version of each such
     * row managed by the persistence context forward without changing its loaded version, as [touchRoot] does.
     */
    protected fun <Root : SnowflakeJpaEntity> touchRoots(
        rootClass: Class<Root>,
        ids: Collection<Long>,
        incrementVersions: (List<Long>) -> Int,
    ) {
        if (ids.isEmpty()) return

        entityManager.flush()
        val session = entityManager.unwrap(SessionImplementor::class.java)
        val persister = session.factory.mappingMetamodel.getEntityDescriptor(rootClass)
        val persistenceContext = session.persistenceContextInternal
        val rows = ids.distinct().sorted().associateWith { id ->
            // Hibernate returns null for a row that the persistence context does not manage.
            val managed: Any? = persistenceContext.getEntity(session.generateEntityKey(id, persister))
            managed?.let(rootClass::cast)
        }
        val pending = rows.filterValues { root -> root == null || (root.loadedVersion != null && root.version == root.loadedVersion) }
        if (pending.isEmpty()) return

        incrementVersions(pending.keys.toList())
        pending.values.filterNotNull().forEach { root -> persistenceContext.getEntry(root).forceLocked(root, root.version + 1) }
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
