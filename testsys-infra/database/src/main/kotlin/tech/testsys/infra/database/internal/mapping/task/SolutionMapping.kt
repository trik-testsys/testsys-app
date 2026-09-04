package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.solution
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.SolutionJpaEntity
import tech.testsys.infra.database.internal.utils.chose
import tech.testsys.infra.database.internal.utils.populateFields
import tech.testsys.infra.database.internal.utils.toJpaEnum

/**
 * Mapping between [Solution] and [SolutionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object SolutionMapping {

    /**
     * Assembles a [Solution] from [jpaEntity] and its loaded file [uploadedFilename] and [content].
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(jpaEntity: SolutionJpaEntity, uploadedFilename: String, content: ByteArray) = solution {
        populateFields(jpaEntity)
        data {
            file(uploadedFilename, content)

            language.chose(jpaEntity.language)
        }
    }

    /**
     * Creates a new [SolutionJpaEntity] row from [data] referencing the stored file [fileDataId].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(data: SolutionData, fileDataId: Long) = SolutionJpaEntity(
        fileDataId = fileDataId,
        language = data.language.toJpaEnum(),
    )

    /**
     * Creates the [SolutionJpaEntity] row replacing [current] from [entity] and file [fileDataId], keeping `createdAt` and `version`.
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(entity: Solution, current: SolutionJpaEntity, fileDataId: Long) = SolutionJpaEntity(
        fileDataId = fileDataId,
        language = entity.data.language.toJpaEnum(),
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = current.version
    }
}
