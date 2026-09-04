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
import tech.testsys.infra.database.internal.utils.findByIdOrError

/**
 * Persistence adapter of [Solution] entities backed by [SolutionJpaEntity].
 * The solution file is stored through [FileDataStorage].
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
        val fileDataId = fileDataStorage.store(data.file)
        val savedJpaEntity = jpaEntityRepository.save(SolutionMapping.toJpaEntity(data, fileDataId))

        val domainEntity = SolutionMapping.toDomain(savedJpaEntity, data.file.uploadedFilename, data.file.content)
        return domainEntity
    }

    @Transactional
    override fun update(entity: Solution): Solution {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        val newFileDataId = fileDataStorage.storeIfChanged(currentJpaEntity.fileDataId, entity.data.file)
        val updatedJpaEntity = jpaEntityRepository.saveAndFlush(
            SolutionMapping.toJpaEntity(entity, currentJpaEntity, newFileDataId),
        )

        val domainEntity = SolutionMapping.toDomain(
            updatedJpaEntity,
            entity.data.file.uploadedFilename,
            entity.data.file.content,
        )
        return domainEntity
    }

    override fun assemble(jpaEntity: SolutionJpaEntity): Solution {
        val file = fileDataStorage.load(jpaEntity.fileDataId)
        val domainEntity = SolutionMapping.toDomain(jpaEntity, file.uploadedFilename, file.content)
        return domainEntity
    }
}
