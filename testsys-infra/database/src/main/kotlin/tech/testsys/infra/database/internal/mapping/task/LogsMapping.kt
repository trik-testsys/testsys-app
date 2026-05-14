package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.logs
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.task.Logs
import tech.testsys.domain.model.task.LogsData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.LogsJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields

@InternalDatabaseApi
object LogsMapping : EntityMapping<Logs, LogsJpaEntity> {

    fun toDomain(jpaEntity: LogsJpaEntity, uploadedFilename: String, content: ByteArray) = logs {
        populateFields(jpaEntity)

        data {
            file(uploadedFilename, content)
        }
    }

    fun toJpaEntity(data: LogsData, fileDataId: Long) = LogsJpaEntity(
        fileDataId = fileDataId,
    )

    fun toJpaEntity(entity: Logs, current: LogsJpaEntity, fileDataId: Long) = LogsJpaEntity(
        fileDataId = fileDataId,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = current.version
    }
}
