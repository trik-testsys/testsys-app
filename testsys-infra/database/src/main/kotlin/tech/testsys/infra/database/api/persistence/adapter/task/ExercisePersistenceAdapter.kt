package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.ExerciseRepository
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseData
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.infra.database.api.persistence.FileDataStorage
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.ExerciseJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.ExerciseJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.ExerciseMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError

/**
 * Persistence adapter of [Exercise] entities backed by [ExerciseJpaEntity].
 * The exercise file is stored through [FileDataStorage] in the version bucket of the exercise.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class ExercisePersistenceAdapter(
    jpaEntityRepository: ExerciseJpaEntityRepository,
    private val fileDataStorage: FileDataStorage,
) : AbstractPersistenceAdapter<ExerciseData, ExerciseId, Exercise, ExerciseJpaEntity>(jpaEntityRepository),
    ExerciseRepository {

    @Transactional
    override fun save(data: ExerciseData): Exercise {
        val fileDataId = fileDataStorage.store(data.file, data.versionBucket)
        val savedJpaEntity = jpaEntityRepository.save(ExerciseMapping.toJpaEntity(data, fileDataId))

        val domainEntity = ExerciseMapping.toDomain(savedJpaEntity, data.file.uploadedFilename, data.file.content)
        return domainEntity
    }

    @Transactional
    override fun update(entity: Exercise): Exercise {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        val newFileDataId = fileDataStorage.storeIfChanged(
            currentJpaEntity.fileDataId,
            entity.data.file,
            entity.data.versionBucket,
        )
        val updatedJpaEntity = jpaEntityRepository.saveAndFlush(
            ExerciseMapping.toJpaEntity(entity, currentJpaEntity, newFileDataId),
        )

        val domainEntity = ExerciseMapping.toDomain(
            updatedJpaEntity,
            entity.data.file.uploadedFilename,
            entity.data.file.content,
        )
        return domainEntity
    }

    override fun assemble(jpaEntity: ExerciseJpaEntity): Exercise {
        val file = fileDataStorage.load(jpaEntity.fileDataId)
        val domainEntity = ExerciseMapping.toDomain(jpaEntity, file.uploadedFilename, file.content)
        return domainEntity
    }
}
