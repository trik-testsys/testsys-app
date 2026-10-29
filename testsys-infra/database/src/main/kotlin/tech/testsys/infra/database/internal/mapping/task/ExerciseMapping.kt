package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.exercise
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseData
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.ExerciseJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.chose
import tech.testsys.infra.database.internal.utils.populateFields
import tech.testsys.infra.database.internal.utils.requireVersion
import tech.testsys.infra.database.internal.utils.toJpaEnum

/**
 * Mapping between [Exercise] and [ExerciseJpaEntity].
 *
 * @since 1.0.0-beta.1
 */
@InternalDatabaseApi
object ExerciseMapping : EntityMapping<Exercise, ExerciseJpaEntity> {

    /**
     * Assembles an [Exercise] from [jpaEntity] and its [file].
     *
     * @since 1.0.0-beta.1
     */
    fun toDomain(jpaEntity: ExerciseJpaEntity, file: FileData) = exercise {
        populateFields(jpaEntity)

        data {
            file(file)

            name = jpaEntity.name
            description = jpaEntity.description
            versionBucket = VersionBucket(jpaEntity.versionBucket)

            language.chose(jpaEntity.language)
        }
    }

    /**
     * Creates a new [ExerciseJpaEntity] row from [data] referencing the stored file [fileDataId].
     *
     * @since 1.0.0-beta.1
     */
    fun toJpaEntity(data: ExerciseData, fileDataId: Long) = ExerciseJpaEntity(
        name = data.name,
        description = data.description,
        fileDataId = fileDataId,
        versionBucket = data.versionBucket.value,
        language = data.language.toJpaEnum(),
    )

    /**
     * Creates the [ExerciseJpaEntity] row replacing [current] from [entity], keeping `fileDataId`, `language`,
     * `versionBucket`, `createdAt` and `version`.
     *
     * @since 1.0.0-beta.1
     */
    fun toJpaEntity(entity: Exercise, current: ExerciseJpaEntity) = ExerciseJpaEntity(
        name = entity.data.name,
        description = entity.data.description,
        fileDataId = current.fileDataId,
        versionBucket = current.versionBucket,
        language = current.language,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = entity.requireVersion()
    }
}
