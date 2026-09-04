package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.logs
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.task.Logs
import tech.testsys.domain.model.task.LogsData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.LogsJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields

/**
 * Mapping between [Logs] and [LogsJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object LogsMapping : EntityMapping<Logs, LogsJpaEntity> {

    /**
     * Assembles a [Logs] from [jpaEntity] and its loaded file [uploadedFilename] and [content].
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(jpaEntity: LogsJpaEntity, uploadedFilename: String, content: ByteArray) = logs {
        populateFields(jpaEntity)

        data {
            file(uploadedFilename, content)
        }
    }

    /**
     * Creates a new [LogsJpaEntity] row from [data] referencing the stored file [fileDataId].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(data: LogsData, fileDataId: Long) = LogsJpaEntity(
        fileDataId = fileDataId,
    )

    /**
     * Creates the [LogsJpaEntity] row replacing [current] from [entity] and file [fileDataId], keeping `createdAt` and `version`.
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(entity: Logs, current: LogsJpaEntity, fileDataId: Long) = LogsJpaEntity(
        fileDataId = fileDataId,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = entity.version.value
    }
}
