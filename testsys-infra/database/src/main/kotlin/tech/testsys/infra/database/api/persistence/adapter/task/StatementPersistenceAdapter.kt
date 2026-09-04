package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.model.task.Statement
import tech.testsys.domain.model.task.StatementData
import tech.testsys.domain.model.task.StatementId
import tech.testsys.infra.database.api.persistence.FileDataStorage
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.StatementJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.StatementJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.StatementMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError

/**
 * Persistence adapter of [Statement] entities backed by [StatementJpaEntity].
 * The statement file is stored through [FileDataStorage] in the version bucket of the statement.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class StatementPersistenceAdapter(
    jpaEntityRepository: StatementJpaEntityRepository,
    private val fileDataStorage: FileDataStorage,
) : AbstractPersistenceAdapter<StatementData, StatementId, Statement, StatementJpaEntity>(jpaEntityRepository),
    StatementRepository {

    @Transactional
    override fun save(data: StatementData): Statement {
        val fileDataId = fileDataStorage.store(data.file, data.versionBucket)
        val savedJpaEntity = jpaEntityRepository.save(StatementMapping.toJpaEntity(data, fileDataId))

        val domainEntity = StatementMapping.toDomain(savedJpaEntity, data.file.uploadedFilename, data.file.content)
        return domainEntity
    }

    @Transactional
    override fun update(entity: Statement): Statement {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        val newFileDataId = fileDataStorage.storeIfChanged(
            currentJpaEntity.fileDataId,
            entity.data.file,
            entity.data.versionBucket,
        )
        val updatedJpaEntity = jpaEntityRepository.saveAndFlush(
            StatementMapping.toJpaEntity(entity, currentJpaEntity, newFileDataId),
        )

        val domainEntity = StatementMapping.toDomain(
            updatedJpaEntity,
            entity.data.file.uploadedFilename,
            entity.data.file.content,
        )
        return domainEntity
    }

    override fun assemble(jpaEntity: StatementJpaEntity): Statement {
        val file = fileDataStorage.load(jpaEntity.fileDataId)
        val domainEntity = StatementMapping.toDomain(jpaEntity, file.uploadedFilename, file.content)
        return domainEntity
    }
}
