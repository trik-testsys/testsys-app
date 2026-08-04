package tech.testsys.infra.database.api.persistence.adapter

import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.EntityRepository
import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SequenceJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.internal.utils.requireById

/**
 * Base abstract class for each persistence adapter. Implements default behavior for next methods of [EntityRepository] contract:
 *
 * - [EntityRepository.findById]
 * - [EntityRepository.findByIds]
 * - [EntityRepository.load]
 * - [EntityRepository.load]
 * - [EntityRepository.save] (overload with list parameter)
 * - [EntityRepository.update] (overload with list parameter)
 * - [EntityRepository.remove] (all overloads)
 * - [EntityRepository.removeById]
 * - [EntityRepository.removeByIds]
 *
 * Each implementation must implement only next methods:
 * - [EntityRepository.save] (overload with [Data] parameter) – method for saving new entity
 * - [EntityRepository.update] (overload with [Entity] parameter) – method for updating existing entity
 * - [AbstractPersistenceAdapter.assemble] – method for assembling [Entity] from [JpaEntity]
 *
 * !CAUTION!
 * If implementation of [EntityRepository.save] or [EntityRepository.update]
 * should have not simple [Transactional] annotation (e.g. with propagation = [Propagation.REQUIRES_NEW])
 * all method overloads must be overridden in the implementation for correct spring AOP working.
 *
 * @param jpaEntityRepository jpa entity repository, used for mapping and persisting domain entity.
 *
 * @param Data domain entity data type
 * @param Id domain entity id type
 * @param Entity domain entity type
 * @param JpaEntity jpa entity type representing domain entity
 *
 * @since %CURRENT_VERSION%
 *
 * @see EntityRepository
 * @see DomainId
 * @see DomainEntity,
 * @see SequenceJpaEntity
 * @see SequenceJpaEntityRepository
 */
@Suppress("CallBeanMethodFromSameClass")
@InternalDatabaseApi
abstract class AbstractPersistenceAdapter<Data, Id : DomainId, Entity : DomainEntity<Id>, JpaEntity : SequenceJpaEntity>(
    protected val jpaEntityRepository: SequenceJpaEntityRepository<JpaEntity>,
) : EntityRepository<Data, Id, Entity> {

    @Transactional(readOnly = true)
    override fun findById(id: Id) = jpaEntityRepository.findByIdOrNull(id.value)?.takeIf { supports(it) }?.let { assemble(it) }

    @Transactional(readOnly = true)
    override fun findByIds(ids: List<Id>) =
        jpaEntityRepository.findAllById(ids.map { it.value }).filter { supports(it) }.map { assemble(it) }

    @Transactional(readOnly = true)
    override fun load(field: LazyEntity<Id, Entity>) = findById(field.id).requireById(field.id)

    @Transactional(readOnly = true)
    override fun load(list: LazyEntityList<Id, Entity>): List<Entity> {
        val entities = findByIds(list.ids)
        val foundIds = entities.map { it.id.value }.toSet()
        val missingIds = list.ids.filter { it.value !in foundIds }
        if (missingIds.isNotEmpty()) error("Entities ${missingIds.map { it.value }} not found") // TODO
        return entities
    }

    @Transactional
    override fun save(dataList: List<Data>) = dataList.map { save(it) }

    @Transactional
    override fun update(entityList: List<Entity>) = entityList.map { update(it) }

    @Transactional
    override fun removeById(id: Id) = jpaEntityRepository.deleteById(id.value)

    @Transactional
    override fun removeByIds(ids: List<Id>) = jpaEntityRepository.deleteAllByIdInBatch(ids.map { it.value })

    @Transactional
    override fun remove(entity: Entity) = removeById(entity.id)

    @Transactional
    override fun remove(entityList: List<Entity>) = removeByIds(entityList.map { it.id })

    /**
     * Tells whether [jpaEntity] belongs to this adapter.
     *
     * Adapters whose [JpaEntity] table is shared by several domain entity kinds
     * (e.g. the user table shared by all user adapters) override this so that
     * [findById] returns `null` and [findByIds] skips rows of a foreign kind
     * instead of failing inside [assemble]. Defaults to `true`.
     */
    protected open fun supports(jpaEntity: JpaEntity): Boolean = true

    /**
     * Method used to assembling [Entity] object from [JpaEntity] object.
     */
    protected abstract fun assemble(jpaEntity: JpaEntity): Entity
}
