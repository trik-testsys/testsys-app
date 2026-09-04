package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.VerdictJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.VerdictJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.VerdictMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError

/**
 * Persistence adapter of [Verdict] entities backed by [VerdictJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class VerdictPersistenceAdapter(
    jpaEntityRepository: VerdictJpaEntityRepository,
) : AbstractPersistenceAdapter<VerdictData, VerdictId, Verdict, VerdictJpaEntity>(jpaEntityRepository),
    VerdictRepository {

    @Transactional
    override fun save(data: VerdictData) = VerdictMapping.toDomain(jpaEntityRepository.save(VerdictMapping.toJpaEntity(data)))

    @Transactional
    override fun update(entity: Verdict): Verdict {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        val updatedJpaEntity = VerdictMapping.toJpaEntity(entity, currentJpaEntity)
        val savedJpaEntity = jpaEntityRepository.save(updatedJpaEntity)

        val domainEntity = VerdictMapping.toDomain(savedJpaEntity)
        return domainEntity
    }

    override fun assemble(jpaEntity: VerdictJpaEntity) = VerdictMapping.toDomain(jpaEntity)
}
