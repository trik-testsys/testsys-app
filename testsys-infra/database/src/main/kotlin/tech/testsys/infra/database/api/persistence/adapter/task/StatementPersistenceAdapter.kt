package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.StoredBlobRef
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.model.task.Statement
import tech.testsys.domain.model.task.StatementData
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.infra.database.api.persistence.FileDataStorage
import tech.testsys.infra.database.api.persistence.FileStoragePaths
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.StatementJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.StatementJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TaskJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.VersionBucketToTaskJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.StatementMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError
import java.util.UUID

/**
 * Persistence adapter of [Statement] entities backed by [StatementJpaEntity].
 * The statement file is stored through [FileDataStorage] in [FileStoragePaths.statement]
 * and fixed on creation: [update] with another file throws [UnsupportedOperationException].
 * Writing or removing a version of a chain uploaded to a task increments the task version.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class StatementPersistenceAdapter(
    jpaEntityRepository: StatementJpaEntityRepository,
    private val fileDataStorage: FileDataStorage,
    private val paths: FileStoragePaths,
    private val fileDataJpaEntityRepository: FileDataJpaEntityRepository,
    private val taskJpaEntityRepository: TaskJpaEntityRepository,
    private val versionBucketToTaskJpaEntityRepository: VersionBucketToTaskJpaEntityRepository,
) : AbstractPersistenceAdapter<StatementData, StatementId, Statement, StatementJpaEntity>(jpaEntityRepository),
    StatementRepository {

    private val resourceVersionRepository: StatementJpaEntityRepository = jpaEntityRepository

    @Transactional
    override fun save(data: StatementData): Statement {
        touchTaskOf(data.versionBucket.value)
        val fileDataId = fileDataStorage.store(data.file, paths.statement)
        val savedJpaEntity = jpaEntityRepository.save(StatementMapping.toJpaEntity(data, fileDataId))

        val domainEntity = StatementMapping.toDomain(savedJpaEntity, data.file.uploadedFilename, data.file.content)
        return domainEntity
    }

    @Transactional
    override fun update(entity: Statement): Statement {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        touchTaskOf(currentJpaEntity.versionBucket)
        fileDataStorage.requireSameFile(entity, entity.data.file, currentJpaEntity)
        val updatedJpaEntity = jpaEntityRepository.saveAndFlush(StatementMapping.toJpaEntity(entity, currentJpaEntity))

        val domainEntity = StatementMapping.toDomain(
            updatedJpaEntity,
            entity.data.file.uploadedFilename,
            entity.data.file.content,
        )
        return domainEntity
    }

    @Transactional(readOnly = true)
    override fun findLatestByVersionBucket(versionBucket: VersionBucket): Statement? {
        return resourceVersionRepository.findFirstByVersionBucketOrderByCreatedAtDescIdDesc(versionBucket.value)?.let { assemble(it) }
    }

    @Transactional(readOnly = true)
    override fun findVersionsByVersionBucket(versionBucket: VersionBucket): List<Statement> =
        assembleAll(resourceVersionRepository.findAllByVersionBucket(versionBucket.value))

    @Transactional(readOnly = true)
    override fun existsByVersionBucket(versionBucket: VersionBucket): Boolean =
        resourceVersionRepository.existsByVersionBucket(versionBucket.value)

    @Transactional(readOnly = true)
    override fun findFileRef(versionBucket: VersionBucket, id: StatementId): StoredBlobRef? {
        val row = jpaEntityRepository.findByIdOrNull(id.value)?.takeIf { it.versionBucket == versionBucket.value } ?: return null
        return StoredBlobRef(fileDataJpaEntityRepository.findByIdOrError(row.fileDataId).storedFileName)
    }

    override fun assembleAll(rows: List<StatementJpaEntity>): List<Statement> = rows.map(::assemble)

    override fun assemble(jpaEntity: StatementJpaEntity): Statement {
        val file = fileDataStorage.load(jpaEntity.fileDataId, paths.statement)
        val domainEntity = StatementMapping.toDomain(jpaEntity, file.uploadedFilename, file.content)
        return domainEntity
    }

    override fun removeRoot(id: StatementId, expectedVersion: Long?) {
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
