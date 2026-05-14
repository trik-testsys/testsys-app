package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.developerSolution
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.DeveloperSolutionJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields

@InternalDatabaseApi
object DeveloperSolutionMapping : EntityMapping<DeveloperSolution, DeveloperSolutionJpaEntity> {

    fun toDomain(jpaEntity: DeveloperSolutionJpaEntity) = developerSolution {
        populateFields(jpaEntity)
        data {
            name = jpaEntity.name
            description = jpaEntity.description

            solution(jpaEntity.solutionId)
            expectedScore(jpaEntity.expectedScore)

            versionBucket = jpaEntity.versionBucket
        }
    }

    fun toJpaEntity(data: DeveloperSolutionData, fileDataId: Long) = DeveloperSolutionJpaEntity(
        name = data.name,
        description = data.description,
        solutionId = data.solution.id.value,
        expectedScore = data.expectedScore.value,
        versionBucket = data.versionBucket,
        fileDataId = fileDataId,
    )

    fun toJpaEntity(entity: DeveloperSolution, current: DeveloperSolutionJpaEntity) = DeveloperSolutionJpaEntity(
        name = entity.data.name,
        description = entity.data.description,
        solutionId = entity.data.solution.id.value,
        expectedScore = entity.data.expectedScore.value,
        id = entity.id.value,
        versionBucket = entity.data.versionBucket,
        fileDataId = current.fileDataId,
    ).also {
        it.createdAt = current.createdAt
        it.version = current.version
    }
}
