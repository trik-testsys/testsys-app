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
import tech.testsys.infra.database.internal.jpa.repository.task.SolutionJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.DeveloperSolutionMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError
import java.util.UUID

/**
 * Persistence adapter of [DeveloperSolution] entities backed by [DeveloperSolutionJpaEntity].
 * A developer solution and its solution must share the version bucket, so that every version of the solution file
 * ends up in one bucket.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class DeveloperSolutionPersistenceAdapter(
    jpaEntityRepository: DeveloperSolutionJpaEntityRepository,
    private val solutionJpaEntityRepository: SolutionJpaEntityRepository,
) : AbstractPersistenceAdapter<DeveloperSolutionData, DeveloperSolutionId, DeveloperSolution, DeveloperSolutionJpaEntity>(
    jpaEntityRepository,
),
    DeveloperSolutionRepository {

    @Transactional
    override fun save(data: DeveloperSolutionData): DeveloperSolution {
        requireSameBucket(data.solution.id.value, data.versionBucket)

        val jpaEntity = DeveloperSolutionMapping.toJpaEntity(data)
        val savedJpaEntity = jpaEntityRepository.save(jpaEntity)

        val domainEntity = DeveloperSolutionMapping.toDomain(savedJpaEntity)
        return domainEntity
    }

    @Transactional
    override fun update(entity: DeveloperSolution): DeveloperSolution {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        requireSameBucket(entity.data.solution.id.value, currentJpaEntity.versionBucket)

        val updatedJpaEntity = DeveloperSolutionMapping.toJpaEntity(entity, currentJpaEntity)
        val savedJpaEntity = jpaEntityRepository.saveAndFlush(updatedJpaEntity)

        val domainEntity = DeveloperSolutionMapping.toDomain(savedJpaEntity)
        return domainEntity
    }

    override fun assemble(jpaEntity: DeveloperSolutionJpaEntity) = DeveloperSolutionMapping.toDomain(jpaEntity)

    private fun requireSameBucket(solutionId: Long, versionBucket: UUID) {
        val solutionBucket = solutionJpaEntityRepository.findByIdOrError(solutionId).versionBucket
        require(solutionBucket == versionBucket) {
            "solution id=$solutionId belongs to version bucket $solutionBucket, " +
                "but developer solution requires $versionBucket"
        }
    }
}
