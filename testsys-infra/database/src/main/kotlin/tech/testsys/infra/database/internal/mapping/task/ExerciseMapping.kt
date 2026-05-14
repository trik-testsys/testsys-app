package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.exercise
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.ExerciseJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.chose
import tech.testsys.infra.database.internal.utils.populateFields
import tech.testsys.infra.database.internal.utils.toJpaEnum

@InternalDatabaseApi
object ExerciseMapping : EntityMapping<Exercise, ExerciseJpaEntity> {

    fun toDomain(jpaEntity: ExerciseJpaEntity, uploadedFilename: String, content: ByteArray) = exercise {
        populateFields(jpaEntity)

        data {
            file(uploadedFilename, content)

            name = jpaEntity.name
            description = jpaEntity.description
            versionBucket = jpaEntity.versionBucket

            language.chose(jpaEntity.language)
        }
    }

    fun toJpaEntity(data: ExerciseData, fileDataId: Long) = ExerciseJpaEntity(
        name = data.name,
        description = data.description,
        fileDataId = fileDataId,
        versionBucket = data.versionBucket,
        language = data.language.toJpaEnum(),
    )

    fun toJpaEntity(entity: Exercise, current: ExerciseJpaEntity, fileDataId: Long) = ExerciseJpaEntity(
        name = entity.data.name,
        description = entity.data.description,
        fileDataId = fileDataId,
        versionBucket = entity.data.versionBucket,
        language = entity.data.language.toJpaEnum(),
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = current.version
    }
}
