package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.recording
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Recording
import tech.testsys.domain.model.task.RecordingData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.RecordingJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields

/**
 * Mapping between [Recording] and [RecordingJpaEntity].
 *
 * @since 1.0.0-beta.1
 */
@InternalDatabaseApi
object RecordingMapping : EntityMapping<Recording, RecordingJpaEntity> {

    /**
     * Assembles a [Recording] from [jpaEntity] and its [file].
     *
     * @since 1.0.0-beta.1
     */
    fun toDomain(jpaEntity: RecordingJpaEntity, file: FileData) = recording {
        populateFields(jpaEntity)

        data {
            file(file)
        }
    }

    /**
     * Creates a new [RecordingJpaEntity] row from [data] referencing the stored file [fileDataId].
     *
     * @since 1.0.0-beta.1
     */
    fun toJpaEntity(data: RecordingData, fileDataId: Long) = RecordingJpaEntity(
        fileDataId = fileDataId,
    )
}
