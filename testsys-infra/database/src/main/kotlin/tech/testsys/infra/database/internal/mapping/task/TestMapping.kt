package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.test
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.TestJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields

@InternalDatabaseApi
object TestMapping : EntityMapping<Test, TestJpaEntity> {

    fun toDomain(jpaEntity: TestJpaEntity, uploadedFilename: String, content: ByteArray) = test {
        populateFields(jpaEntity)
        data {
            file(uploadedFilename, content)

            name = jpaEntity.name
            description = jpaEntity.description
            versionBucket = jpaEntity.versionBucket
        }
    }

    fun toJpaEntity(data: TestData, fileDataId: Long) = TestJpaEntity(
        name = data.name,
        description = data.description,
        fileDataId = fileDataId,
        versionBucket = data.versionBucket,
    )

    fun toJpaEntity(entity: Test, current: TestJpaEntity, fileDataId: Long) = TestJpaEntity(
        name = entity.data.name,
        description = entity.data.description,
        fileDataId = fileDataId,
        versionBucket = entity.data.versionBucket,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = current.version
    }
}
