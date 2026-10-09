package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.StoredBlobRef
import tech.testsys.domain.contract.persistence.repository.DeveloperSolutionRepository
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionData
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.DeveloperSolutionJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.DeveloperSolutionJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.SolutionJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TaskJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.VersionBucketToTaskJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.DeveloperSolutionMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError
import java.util.UUID

/**
 * Persistence adapter of [DeveloperSolution] entities backed by [DeveloperSolutionJpaEntity].
 * [update] rejects changes to the solution or expected score with [UnsupportedOperationException].
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class DeveloperSolutionPersistenceAdapter(
    jpaEntityRepository: DeveloperSolutionJpaEntityRepository,
    private val solutionJpaEntityRepository: SolutionJpaEntityRepository,
    private val fileDataJpaEntityRepository: FileDataJpaEntityRepository,
    private val taskJpaEntityRepository: TaskJpaEntityRepository,
    private val versionBucketToTaskJpaEntityRepository: VersionBucketToTaskJpaEntityRepository,
) : AbstractPersistenceAdapter<DeveloperSolutionData, DeveloperSolutionId, DeveloperSolution, DeveloperSolutionJpaEntity>(
    jpaEntityRepository,
),
    DeveloperSolutionRepository {

    private val resourceVersionRepository: DeveloperSolutionJpaEntityRepository = jpaEntityRepository

    @Transactional
    override fun save(data: DeveloperSolutionData): DeveloperSolution {
        touchTaskOf(data.versionBucket.value)
        val jpaEntity = DeveloperSolutionMapping.toJpaEntity(data)
        val savedJpaEntity = jpaEntityRepository.save(jpaEntity)

        val domainEntity = DeveloperSolutionMapping.toDomain(savedJpaEntity)
        return domainEntity
    }

    @Transactional
    override fun update(entity: DeveloperSolution): DeveloperSolution {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        touchTaskOf(currentJpaEntity.versionBucket)
        val solutionId = entity.data.solution.id.value
        entity.requireUnchanged("solution", solutionId == currentJpaEntity.solutionId, currentJpaEntity.versionBucket) {
            "from id=${currentJpaEntity.solutionId} to id=$solutionId"
        }
        val expectedScore = entity.data.expectedScore.value
        entity.requireUnchanged("expectedScore", expectedScore == currentJpaEntity.expectedScore, currentJpaEntity.versionBucket) {
            "from ${currentJpaEntity.expectedScore} to $expectedScore"
        }

        val updatedJpaEntity = DeveloperSolutionMapping.toJpaEntity(entity, currentJpaEntity)
        val savedJpaEntity = jpaEntityRepository.saveAndFlush(updatedJpaEntity)

        val domainEntity = DeveloperSolutionMapping.toDomain(savedJpaEntity)
        return domainEntity
    }

    @Transactional(readOnly = true)
    override fun findLatestByVersionBucket(versionBucket: VersionBucket): DeveloperSolution? {
        return resourceVersionRepository.findFirstByVersionBucketOrderByCreatedAtDescIdDesc(versionBucket.value)?.let { assemble(it) }
    }

    override fun assembleAll(rows: List<DeveloperSolutionJpaEntity>): List<DeveloperSolution> =
        rows.map { row -> DeveloperSolutionMapping.toDomain(row) }

    @Transactional(readOnly = true)
    override fun findVersionsByVersionBucket(versionBucket: VersionBucket): List<DeveloperSolution> =
        assembleAll(resourceVersionRepository.findAllByVersionBucket(versionBucket.value))

    @Transactional(readOnly = true)
    override fun existsByVersionBucket(versionBucket: VersionBucket): Boolean =
        resourceVersionRepository.existsByVersionBucket(versionBucket.value)

    @Transactional(readOnly = true)
    override fun findFileRef(versionBucket: VersionBucket, id: DeveloperSolutionId): StoredBlobRef? {
        val row = jpaEntityRepository.findByIdOrNull(id.value)?.takeIf { it.versionBucket == versionBucket.value } ?: return null
        val solution = solutionJpaEntityRepository.findByIdOrError(row.solutionId)
        return StoredBlobRef(fileDataJpaEntityRepository.findByIdOrError(solution.fileDataId).storedFileName)
    }

    override fun removeRoot(id: DeveloperSolutionId, expectedVersion: Long?) {
        val current = jpaEntityRepository.findByIdOrNull(id.value) ?: return
        touchTaskOf(current.versionBucket)
        super.removeRoot(id, expectedVersion)
    }

    /** Increments the version of the task the resource chain [versionBucket] is uploaded to, if there is one. */
    private fun touchTaskOf(versionBucket: UUID) {
        versionBucketToTaskJpaEntityRepository.findTaskIdByVersionBucket(versionBucket)?.let { taskId ->
            touchRoot(taskJpaEntityRepository, taskId)
        }
    }
}
