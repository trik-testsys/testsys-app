package tech.testsys.infra.database.api.persistence.adapter.user

import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.UserJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository

/**
 * Base of adapters storing several user kinds in the shared [UserJpaEntity] table: [findById] and [findByIds]
 * skip rows rejected by [supports], so a row of another kind is reported as absent rather than assembled.
 *
 * @param Data the data type a new entity is created from.
 * @param Id the id type of the entity.
 * @param Entity the domain entity type.
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
abstract class AbstractUserPersistenceAdapter<Data, Id : DomainId, Entity : DomainEntity<Id>>(
    jpaEntityRepository: UserJpaEntityRepository,
) : AbstractPersistenceAdapter<Data, Id, Entity, UserJpaEntity>(jpaEntityRepository) {

    @Transactional(readOnly = true)
    override fun findById(id: Id) = jpaEntityRepository.findByIdOrNull(id.value)?.takeIf { supports(it) }?.let { assemble(it) }

    @Transactional(readOnly = true)
    override fun findByIds(ids: List<Id>) =
        jpaEntityRepository.findAllById(ids.map { it.value }).filter { supports(it) }.map { assemble(it) }

    /**
     * Whether [jpaEntity] holds a user of this adapter's kind.
     */
    protected abstract fun supports(jpaEntity: UserJpaEntity): Boolean
}
