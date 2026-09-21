package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.SolutionRepository
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionData
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.infra.database.api.persistence.FileDataStorage
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.SolutionJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.SolutionJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.SolutionMapping

/**
 * Persistence adapter of [Solution] entities backed by [SolutionJpaEntity].
 * The solution file is stored through [FileDataStorage] in the version bucket of the solution;
 * a solution is fixed on creation, so [update] always fails.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class SolutionPersistenceAdapter(
    jpaEntityRepository: SolutionJpaEntityRepository,
    private val fileDataStorage: FileDataStorage,
) : AbstractPersistenceAdapter<SolutionData, SolutionId, Solution, SolutionJpaEntity>(jpaEntityRepository),
    SolutionRepository {

    @Transactional
    override fun save(data: SolutionData): Solution {
        val fileDataId = fileDataStorage.store(data.file, data.versionBucket)
        val savedJpaEntity = jpaEntityRepository.save(SolutionMapping.toJpaEntity(data, fileDataId))

        val domainEntity = SolutionMapping.toDomain(savedJpaEntity, data.file.uploadedFilename, data.file.content)
        return domainEntity
    }

    override fun update(entity: Solution): Solution = throw UnsupportedOperationException(
        "solution ${entity.id.value} cannot be updated: every field of a solution is fixed on creation",
    )

    override fun assemble(jpaEntity: SolutionJpaEntity): Solution {
        val file = fileDataStorage.load(jpaEntity.fileDataId)
        val domainEntity = SolutionMapping.toDomain(jpaEntity, file.uploadedFilename, file.content)
        return domainEntity
    }
}
