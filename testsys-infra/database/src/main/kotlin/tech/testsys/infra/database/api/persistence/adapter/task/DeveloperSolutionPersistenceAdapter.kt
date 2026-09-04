package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.DeveloperSolutionRepository
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionData
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.DeveloperSolutionJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.DeveloperSolutionJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.DeveloperSolutionMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError

/**
 * Persistence adapter of [DeveloperSolution] entities backed by [DeveloperSolutionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class DeveloperSolutionPersistenceAdapter(
    jpaEntityRepository: DeveloperSolutionJpaEntityRepository,
) : AbstractPersistenceAdapter<DeveloperSolutionData, DeveloperSolutionId, DeveloperSolution, DeveloperSolutionJpaEntity>(
    jpaEntityRepository,
),
    DeveloperSolutionRepository {

    @Transactional
    override fun save(data: DeveloperSolutionData): DeveloperSolution {
        val jpaEntity = DeveloperSolutionMapping.toJpaEntity(data)
        val savedJpaEntity = jpaEntityRepository.save(jpaEntity)

        val domainEntity = DeveloperSolutionMapping.toDomain(savedJpaEntity)
        return domainEntity
    }

    @Transactional
    override fun update(entity: DeveloperSolution): DeveloperSolution {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        val updatedJpaEntity = DeveloperSolutionMapping.toJpaEntity(entity, currentJpaEntity)
        val savedJpaEntity = jpaEntityRepository.saveAndFlush(updatedJpaEntity)

        val domainEntity = DeveloperSolutionMapping.toDomain(savedJpaEntity)
        return domainEntity
    }

    override fun assemble(jpaEntity: DeveloperSolutionJpaEntity) = DeveloperSolutionMapping.toDomain(jpaEntity)
}
