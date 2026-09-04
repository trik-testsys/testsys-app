package tech.testsys.infra.database.internal.jpa.entity.task

import jakarta.persistence.Entity
import tech.testsys.infra.database.internal.InternalDatabaseApi
import java.util.UUID

/**
 * JPA entity of [tech.testsys.domain.model.task.DeveloperSolution]: a [SolutionJpaEntity] plus the score it must reach.
 *
 * @property solutionId id of the [SolutionJpaEntity] holding the source.
 * @property expectedScore the score the solution is expected to reach.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class DeveloperSolutionJpaEntity(
    name: String,
    description: String,
    fileDataId: Long,
    versionBucket: UUID,
    val solutionId: Long,
    val expectedScore: Int,
    id: Long? = null,
) : ResourceJpaEntity(name, description, fileDataId, versionBucket, id)
