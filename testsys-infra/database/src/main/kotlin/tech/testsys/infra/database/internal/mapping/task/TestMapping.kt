package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.test
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.TestJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields

/**
 * Mapping between [Test] and [TestJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object TestMapping : EntityMapping<Test, TestJpaEntity> {

    /**
     * Assembles a [Test] from [jpaEntity] and its loaded file [uploadedFilename] and [content].
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(jpaEntity: TestJpaEntity, uploadedFilename: String, content: ByteArray) = test {
        populateFields(jpaEntity)
        data {
            file(uploadedFilename, content)

            name = jpaEntity.name
            description = jpaEntity.description
            versionBucket = jpaEntity.versionBucket
        }
    }

    /**
     * Creates a new [TestJpaEntity] row from [data] referencing the stored file [fileDataId].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(data: TestData, fileDataId: Long) = TestJpaEntity(
        name = data.name,
        description = data.description,
        fileDataId = fileDataId,
        versionBucket = data.versionBucket,
    )

    /**
     * Creates the [TestJpaEntity] row replacing [current] from [entity] and file [fileDataId], keeping `createdAt` and `version`.
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(entity: Test, current: TestJpaEntity, fileDataId: Long) = TestJpaEntity(
        name = entity.data.name,
        description = entity.data.description,
        fileDataId = fileDataId,
        versionBucket = entity.data.versionBucket,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = entity.version.value
    }
}
