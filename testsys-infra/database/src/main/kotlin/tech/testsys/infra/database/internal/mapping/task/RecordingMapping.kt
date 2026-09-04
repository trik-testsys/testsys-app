package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.recording
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.task.Recording
import tech.testsys.domain.model.task.RecordingData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.RecordingJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields

/**
 * Mapping between [Recording] and [RecordingJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object RecordingMapping : EntityMapping<Recording, RecordingJpaEntity> {

    /**
     * Assembles a [Recording] from [jpaEntity] and its loaded file [uploadedFilename] and [content].
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(jpaEntity: RecordingJpaEntity, uploadedFilename: String, content: ByteArray) = recording {
        populateFields(jpaEntity)

        data {
            file(uploadedFilename, content)
        }
    }

    /**
     * Creates a new [RecordingJpaEntity] row from [data] referencing the stored file [fileDataId].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(data: RecordingData, fileDataId: Long) = RecordingJpaEntity(
        fileDataId = fileDataId,
    )

    /**
     * Creates the [RecordingJpaEntity] row replacing [current] from [entity] and file [fileDataId], keeping `createdAt` and `version`.
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(entity: Recording, current: RecordingJpaEntity, fileDataId: Long) = RecordingJpaEntity(
        fileDataId = fileDataId,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = current.version
    }
}
