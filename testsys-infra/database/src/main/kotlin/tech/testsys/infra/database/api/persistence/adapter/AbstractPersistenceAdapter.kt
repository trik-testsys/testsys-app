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
 * Base of persistence adapters: implements finding, loading, removing and the list overloads of [EntityRepository];
 * subclasses provide [save], [update] and [assemble]. Overloads that need non-default [Transactional] settings
 * (e.g. [Propagation.REQUIRES_NEW]) must be overridden together (Spring AOP self-invocation).
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
abstract class AbstractPersistenceAdapter<Data, Id : DomainId, Entity : DomainEntity<Id>, JpaEntity : SequenceJpaEntity>(
    protected val jpaEntityRepository: SequenceJpaEntityRepository<JpaEntity>,
) : EntityRepository<Data, Id, Entity> {

    @Transactional(readOnly = true)
    override fun findById(id: Id) = jpaEntityRepository.findByIdOrNull(id.value)?.let { assemble(it) }

    @Transactional(readOnly = true)
    override fun findByIds(ids: List<Id>) = jpaEntityRepository.findAllById(ids.map { it.value }).map { assemble(it) }

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
     * Assembles an [Entity] from its [jpaEntity] row.
     */
    protected abstract fun assemble(jpaEntity: JpaEntity): Entity
}
