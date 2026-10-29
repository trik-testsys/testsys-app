package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.solution
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.SolutionJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.chose
import tech.testsys.infra.database.internal.utils.populateFields
import tech.testsys.infra.database.internal.utils.toJpaEnum

/**
 * Mapping between [Solution] and [SolutionJpaEntity].
 *
 * @since 1.0.0-beta.1
 */
@InternalDatabaseApi
object SolutionMapping : EntityMapping<Solution, SolutionJpaEntity> {

    /**
     * Assembles a [Solution] from [jpaEntity] and its [file].
     *
     * @since 1.0.0-beta.1
     */
    fun toDomain(jpaEntity: SolutionJpaEntity, file: FileData) = solution {
        populateFields(jpaEntity)
        data {
            file(file)

            language.chose(jpaEntity.language)
        }
    }

    /**
     * Creates a new [SolutionJpaEntity] row from [data] referencing the stored file [fileDataId].
     *
     * @since 1.0.0-beta.1
     */
    fun toJpaEntity(data: SolutionData, fileDataId: Long) = SolutionJpaEntity(
        fileDataId = fileDataId,
        language = data.language.toJpaEnum(),
    )
}
