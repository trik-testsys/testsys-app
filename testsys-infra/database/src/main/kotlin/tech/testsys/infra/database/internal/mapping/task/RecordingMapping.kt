package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.recording
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.task.Recording
import tech.testsys.domain.model.task.RecordingData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.RecordingJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields

@InternalDatabaseApi
object RecordingMapping : EntityMapping<Recording, RecordingJpaEntity> {

    fun toDomain(jpaEntity: RecordingJpaEntity, uploadedFilename: String, content: ByteArray) = recording {
        populateFields(jpaEntity)

        data {
            file(uploadedFilename, content)
        }
    }

    fun toJpaEntity(data: RecordingData, fileDataId: Long) = RecordingJpaEntity(
        fileDataId = fileDataId,
    )

    fun toJpaEntity(entity: Recording, current: RecordingJpaEntity, fileDataId: Long) = RecordingJpaEntity(
        fileDataId = fileDataId,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = current.version
    }
}
