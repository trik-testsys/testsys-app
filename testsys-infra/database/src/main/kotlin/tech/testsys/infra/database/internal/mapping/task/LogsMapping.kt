package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.logs
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.task.FileData
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
     * Assembles a [Logs] from [jpaEntity] and its [file].
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(jpaEntity: LogsJpaEntity, file: FileData) = logs {
        populateFields(jpaEntity)

        data {
            file(file)
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
}
