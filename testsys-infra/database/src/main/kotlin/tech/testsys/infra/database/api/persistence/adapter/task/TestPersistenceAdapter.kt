package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestData
import tech.testsys.domain.model.task.TestId
import tech.testsys.infra.database.api.persistence.FileDataStorage
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.TestJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.TestJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.TestMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError

/**
 * Persistence adapter of [Test] entities backed by [TestJpaEntity].
 * The polygon file is stored through [FileDataStorage] and fixed on creation: [update] with another file throws
 * [UnsupportedOperationException].
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class TestPersistenceAdapter(
    jpaEntityRepository: TestJpaEntityRepository,
    private val fileDataStorage: FileDataStorage,
) : AbstractPersistenceAdapter<TestData, TestId, Test, TestJpaEntity>(jpaEntityRepository),
    TestRepository {

    @Transactional
    override fun save(data: TestData): Test {
        val fileDataId = fileDataStorage.store(data.file)
        val savedJpaEntity = jpaEntityRepository.save(TestMapping.toJpaEntity(data, fileDataId))

        val domainEntity = TestMapping.toDomain(savedJpaEntity, data.file.uploadedFilename, data.file.content)
        return domainEntity
    }

    @Transactional
    override fun update(entity: Test): Test {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        fileDataStorage.requireSameFile(entity, entity.data.file, currentJpaEntity)
        val updatedJpaEntity = jpaEntityRepository.saveAndFlush(TestMapping.toJpaEntity(entity, currentJpaEntity))

        val domainEntity = TestMapping.toDomain(
            updatedJpaEntity,
            entity.data.file.uploadedFilename,
            entity.data.file.content,
        )
        return domainEntity
    }

    override fun assemble(jpaEntity: TestJpaEntity): Test {
        val file = fileDataStorage.load(jpaEntity.fileDataId)
        val domainEntity = TestMapping.toDomain(jpaEntity, file.uploadedFilename, file.content)
        return domainEntity
    }
}
