package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.statement
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.task.Statement
import tech.testsys.domain.model.task.StatementData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.StatementJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields

@InternalDatabaseApi
object StatementMapping : EntityMapping<Statement, StatementJpaEntity> {

    fun toDomain(jpaEntity: StatementJpaEntity, uploadedFilename: String, content: ByteArray) = statement {
        populateFields(jpaEntity)

        data {
            file(uploadedFilename, content)

            name = jpaEntity.name
            description = jpaEntity.description
            versionBucket = jpaEntity.versionBucket
        }
    }

    fun toJpaEntity(data: StatementData, fileDataId: Long) = StatementJpaEntity(
        name = data.name,
        description = data.description,
        fileDataId = fileDataId,
        versionBucket = data.versionBucket,
    )

    fun toJpaEntity(entity: Statement, current: StatementJpaEntity, fileDataId: Long) = StatementJpaEntity(
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
