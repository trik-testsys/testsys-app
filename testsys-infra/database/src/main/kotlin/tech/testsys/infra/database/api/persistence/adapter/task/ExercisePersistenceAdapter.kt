package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.StoredBlobRef
import tech.testsys.domain.contract.persistence.repository.ExerciseRepository
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseData
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.FileStorageKind
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.infra.database.api.persistence.FileDataStorage
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.ExerciseJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.ExerciseJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TaskJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.VersionBucketToTaskJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.ExerciseMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError
import tech.testsys.infra.database.internal.utils.toJpaEnum
import java.util.UUID

/**
 * Persistence adapter of [Exercise] entities backed by [ExerciseJpaEntity].
 * [update] rejects changes to the file or language with [UnsupportedOperationException].
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class ExercisePersistenceAdapter(
    jpaEntityRepository: ExerciseJpaEntityRepository,
    private val fileDataStorage: FileDataStorage,
    private val fileDataJpaEntityRepository: FileDataJpaEntityRepository,
    private val taskJpaEntityRepository: TaskJpaEntityRepository,
    private val versionBucketToTaskJpaEntityRepository: VersionBucketToTaskJpaEntityRepository,
) : AbstractPersistenceAdapter<ExerciseData, ExerciseId, Exercise, ExerciseJpaEntity>(jpaEntityRepository),
    ExerciseRepository {

    private val resourceVersionRepository: ExerciseJpaEntityRepository = jpaEntityRepository

    @Transactional
    override fun save(data: ExerciseData): Exercise {
        touchTaskOf(data.versionBucket.value)
        val fileDataId = fileDataStorage.store(data.file, FileStorageKind.Exercise)
        val savedJpaEntity = jpaEntityRepository.save(ExerciseMapping.toJpaEntity(data, fileDataId))

        return assemble(savedJpaEntity)
    }

    @Transactional
    override fun update(entity: Exercise): Exercise {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        touchTaskOf(currentJpaEntity.versionBucket)
        fileDataStorage.requireSameFile(entity, entity.data.file, currentJpaEntity, FileStorageKind.Exercise)
        val language = entity.data.language.toJpaEnum()
        entity.requireUnchanged("language", language == currentJpaEntity.language, currentJpaEntity.versionBucket) {
            "from ${currentJpaEntity.language} to $language"
        }
        val updatedJpaEntity = jpaEntityRepository.saveAndFlush(ExerciseMapping.toJpaEntity(entity, currentJpaEntity))

        return assemble(updatedJpaEntity)
    }

    @Transactional(readOnly = true)
    override fun findLatestByVersionBucket(versionBucket: VersionBucket): Exercise? {
        return resourceVersionRepository.findFirstByVersionBucketOrderByCreatedAtDescIdDesc(versionBucket.value)?.let { assemble(it) }
    }

    @Transactional(readOnly = true)
    override fun findVersionsByVersionBucket(versionBucket: VersionBucket): List<Exercise> =
        assembleAll(resourceVersionRepository.findAllByVersionBucket(versionBucket.value))

    @Transactional(readOnly = true)
    override fun existsByVersionBucket(versionBucket: VersionBucket): Boolean =
        resourceVersionRepository.existsByVersionBucket(versionBucket.value)

    @Transactional(readOnly = true)
    override fun findFileRef(versionBucket: VersionBucket, id: ExerciseId): StoredBlobRef? {
        val row = jpaEntityRepository.findByIdOrNull(id.value)?.takeIf { it.versionBucket == versionBucket.value } ?: return null
        return StoredBlobRef(fileDataJpaEntityRepository.findByIdOrError(row.fileDataId).storedFileName)
    }

    override fun assembleAll(rows: List<ExerciseJpaEntity>): List<Exercise> {
        val files = fileDataStorage.loadAll(rows.map { row -> row.fileDataId }, FileStorageKind.Exercise)
        return rows.map { row -> ExerciseMapping.toDomain(row, files.getValue(row.fileDataId)) }
    }

    override fun removeRoot(id: ExerciseId, expectedVersion: Long?) {
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
