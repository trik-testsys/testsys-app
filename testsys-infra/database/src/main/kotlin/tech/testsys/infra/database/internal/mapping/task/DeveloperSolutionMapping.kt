package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.developerSolution
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.DeveloperSolutionJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields

/**
 * Mapping between [DeveloperSolution] and [DeveloperSolutionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object DeveloperSolutionMapping : EntityMapping<DeveloperSolution, DeveloperSolutionJpaEntity> {

    /**
     * Assembles a [DeveloperSolution] from [jpaEntity].
     *
     * @since %CURRENT_VERSION%
     */
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

    /**
     * Creates a new [DeveloperSolutionJpaEntity] row from [data] referencing the stored file [fileDataId] of its solution.
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(data: DeveloperSolutionData, fileDataId: Long) = DeveloperSolutionJpaEntity(
        name = data.name,
        description = data.description,
        solutionId = data.solution.id.value,
        expectedScore = data.expectedScore.value,
        versionBucket = data.versionBucket,
        fileDataId = fileDataId,
    )

    /**
     * Creates the [DeveloperSolutionJpaEntity] row replacing [current] from [entity], keeping its file, `createdAt` and `version`.
     *
     * @since %CURRENT_VERSION%
     */
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
