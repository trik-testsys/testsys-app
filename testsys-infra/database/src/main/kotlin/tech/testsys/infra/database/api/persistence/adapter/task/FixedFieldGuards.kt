package tech.testsys.infra.database.api.persistence.adapter.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.task.FileData
import tech.testsys.infra.database.api.persistence.FileDataStorage
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.ResourceJpaEntity
import java.util.UUID

/**
 * Throws [UnsupportedOperationException] unless [unchanged]: [field] of this entity is fixed on creation, and a new
 * value needs a new entity saved in [versionBucket]; [change] describes the rejected change for the message.
 */
@InternalDatabaseApi
internal fun DomainEntity<*>.requireUnchanged(field: String, unchanged: Boolean, versionBucket: UUID, change: () -> String) {
    if (!unchanged) {
        val name = this::class.simpleName
        throw UnsupportedOperationException(
            "$name id=${id.value} cannot change its $field ${change()}: " +
                "the $field is fixed on creation, save a new $name in version bucket $versionBucket instead",
        )
    }
}

/**
 * Throws [UnsupportedOperationException] if [file] of [entity] differs from the stored file of [current] by name
 * or content.
 */
@InternalDatabaseApi
internal fun FileDataStorage.requireSameFile(entity: DomainEntity<*>, file: FileData, current: ResourceJpaEntity) =
    entity.requireUnchanged("file", matches(current.fileDataId, file), current.versionBucket) { "to '${file.uploadedFilename}'" }
