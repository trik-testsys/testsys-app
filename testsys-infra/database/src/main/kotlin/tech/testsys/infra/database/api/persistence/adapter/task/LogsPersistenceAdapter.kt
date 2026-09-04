package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.LogsRepository
import tech.testsys.domain.model.task.Logs
import tech.testsys.domain.model.task.LogsData
import tech.testsys.domain.model.task.LogsId
import tech.testsys.infra.database.api.persistence.FileDataStorage
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.LogsJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.LogsJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.LogsMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError

/**
 * Persistence adapter of [Logs] entities backed by [LogsJpaEntity].
 * The logs file is stored through [FileDataStorage].
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class LogsPersistenceAdapter(
    jpaEntityRepository: LogsJpaEntityRepository,
    private val fileDataStorage: FileDataStorage,
) : AbstractPersistenceAdapter<LogsData, LogsId, Logs, LogsJpaEntity>(jpaEntityRepository),
    LogsRepository {

    @Transactional
    override fun save(data: LogsData): Logs {
        val fileDataId = fileDataStorage.store(data.file)
        val savedJpaEntity = jpaEntityRepository.save(LogsMapping.toJpaEntity(data, fileDataId))

        val domainEntity = LogsMapping.toDomain(savedJpaEntity, data.file.uploadedFilename, data.file.content)
        return domainEntity
    }

    @Transactional
    override fun update(entity: Logs): Logs {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        val newFileDataId = fileDataStorage.storeIfChanged(currentJpaEntity.fileDataId, entity.data.file)
        val updatedJpaEntity = jpaEntityRepository.saveAndFlush(
            LogsMapping.toJpaEntity(entity, currentJpaEntity, newFileDataId),
        )

        val domainEntity = LogsMapping.toDomain(
            updatedJpaEntity,
            entity.data.file.uploadedFilename,
            entity.data.file.content,
        )
        return domainEntity
    }

    override fun assemble(jpaEntity: LogsJpaEntity): Logs {
        val file = fileDataStorage.load(jpaEntity.fileDataId)
        val domainEntity = LogsMapping.toDomain(jpaEntity, file.uploadedFilename, file.content)
        return domainEntity
    }
}
