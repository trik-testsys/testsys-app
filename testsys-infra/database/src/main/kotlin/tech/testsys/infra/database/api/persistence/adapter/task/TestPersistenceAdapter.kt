package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.StoredBlobRef
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.model.task.FileStorageKind
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestData
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.infra.database.api.persistence.FileDataStorage
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.TestJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TaskJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TestJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.VersionBucketToTaskJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.TestMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError
import java.util.UUID

/**
 * Persistence adapter of [Test] entities backed by [TestJpaEntity].
 * [FileDataStorage] stores its file in [FileStorageKind.Test]; [update] rejects replacing it
 * with [UnsupportedOperationException].
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class TestPersistenceAdapter(
    jpaEntityRepository: TestJpaEntityRepository,
    private val fileDataStorage: FileDataStorage,
    private val fileDataJpaEntityRepository: FileDataJpaEntityRepository,
    private val taskJpaEntityRepository: TaskJpaEntityRepository,
    private val versionBucketToTaskJpaEntityRepository: VersionBucketToTaskJpaEntityRepository,
) : AbstractPersistenceAdapter<TestData, TestId, Test, TestJpaEntity>(jpaEntityRepository),
    TestRepository {

    private val resourceVersionRepository: TestJpaEntityRepository = jpaEntityRepository

    @Transactional
    override fun save(data: TestData): Test {
        touchTaskOf(data.versionBucket.value)
        val fileDataId = fileDataStorage.store(data.file, FileStorageKind.Test)
        val savedJpaEntity = jpaEntityRepository.save(TestMapping.toJpaEntity(data, fileDataId))

        return assemble(savedJpaEntity)
    }

    @Transactional
    override fun update(entity: Test): Test {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        touchTaskOf(currentJpaEntity.versionBucket)
        fileDataStorage.requireSameFile(entity, entity.data.file, currentJpaEntity, FileStorageKind.Test)
        val updatedJpaEntity = jpaEntityRepository.saveAndFlush(TestMapping.toJpaEntity(entity, currentJpaEntity))

        return assemble(updatedJpaEntity)
    }

    @Transactional(readOnly = true)
    override fun findLatestByVersionBucket(versionBucket: VersionBucket): Test? {
        return resourceVersionRepository.findFirstByVersionBucketOrderByCreatedAtDescIdDesc(versionBucket.value)?.let { assemble(it) }
    }

    @Transactional(readOnly = true)
    override fun findVersionsByVersionBucket(versionBucket: VersionBucket): List<Test> =
        assembleAll(resourceVersionRepository.findAllByVersionBucket(versionBucket.value))

    @Transactional(readOnly = true)
    override fun existsByVersionBucket(versionBucket: VersionBucket): Boolean =
        resourceVersionRepository.existsByVersionBucket(versionBucket.value)

    @Transactional(readOnly = true)
    override fun findFileRef(versionBucket: VersionBucket, id: TestId): StoredBlobRef? {
        val row = jpaEntityRepository.findByIdOrNull(id.value)?.takeIf { it.versionBucket == versionBucket.value } ?: return null
        return StoredBlobRef(fileDataJpaEntityRepository.findByIdOrError(row.fileDataId).storedFileName)
    }

    override fun assembleAll(rows: List<TestJpaEntity>): List<Test> {
        val files = fileDataStorage.loadAll(rows.map { row -> row.fileDataId }, FileStorageKind.Test)
        return rows.map { row -> TestMapping.toDomain(row, files.getValue(row.fileDataId)) }
    }

    override fun removeRoot(id: TestId, expectedVersion: Long?) {
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
