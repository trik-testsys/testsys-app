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
import tech.testsys.infra.database.internal.utils.toJpaEnum

/**
 * Persistence adapter of [Exercise] entities backed by [ExerciseJpaEntity].
 * The exercise file is stored through [FileDataStorage]; the file and the language are fixed on creation:
 * [update] with another one throws [UnsupportedOperationException].
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
        val fileDataId = fileDataStorage.store(data.file)
        val savedJpaEntity = jpaEntityRepository.save(ExerciseMapping.toJpaEntity(data, fileDataId))

        val domainEntity = ExerciseMapping.toDomain(savedJpaEntity, data.file.uploadedFilename, data.file.content)
        return domainEntity
    }

    @Transactional
    override fun update(entity: Exercise): Exercise {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        fileDataStorage.requireSameFile(entity, entity.data.file, currentJpaEntity)
        val language = entity.data.language.toJpaEnum()
        entity.requireUnchanged("language", language == currentJpaEntity.language, currentJpaEntity.versionBucket) {
            "from ${currentJpaEntity.language} to $language"
        }
        val updatedJpaEntity = jpaEntityRepository.saveAndFlush(ExerciseMapping.toJpaEntity(entity, currentJpaEntity))

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
