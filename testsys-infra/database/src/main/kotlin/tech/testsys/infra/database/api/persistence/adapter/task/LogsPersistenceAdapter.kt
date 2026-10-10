package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.LogsRepository
import tech.testsys.domain.model.task.FileStorageKind
import tech.testsys.domain.model.task.Logs
import tech.testsys.domain.model.task.LogsData
import tech.testsys.domain.model.task.LogsId
import tech.testsys.infra.database.api.persistence.FileDataStorage
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.LogsJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.LogsJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.LogsMapping

/**
 * Persistence adapter of [Logs] entities backed by [LogsJpaEntity].
 * The logs file is stored through [FileDataStorage] in [FileStorageKind.Logs];
 * logs are fixed on creation, so [update] always fails.
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
        val fileDataId = fileDataStorage.store(data.file, FileStorageKind.Logs)
        val savedJpaEntity = jpaEntityRepository.save(LogsMapping.toJpaEntity(data, fileDataId))

        return assemble(savedJpaEntity)
    }

    override fun update(entity: Logs): Logs = throw UnsupportedOperationException(
        "logs ${entity.id.value} cannot be updated: every field of logs is fixed on creation",
    )

    override fun assembleAll(rows: List<LogsJpaEntity>): List<Logs> {
        val files = fileDataStorage.loadAll(rows.map { row -> row.fileDataId }, FileStorageKind.Logs)
        return rows.map { row -> LogsMapping.toDomain(row, files.getValue(row.fileDataId)) }
    }
}
