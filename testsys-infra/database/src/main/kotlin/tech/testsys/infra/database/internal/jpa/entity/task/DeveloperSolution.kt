package tech.testsys.infra.database.internal.jpa.entity.task

import jakarta.persistence.Entity
import tech.testsys.infra.database.internal.InternalDatabaseApi
import java.util.UUID

/**
 * JPA entity representing a developer solution domain entity.
 *
 * A developer solution is a thin aggregate over a [SolutionJpaEntity]
 * (referenced by [solutionId]) carrying an [expectedScore] used to validate
 * grading; the actual source code, file content and language live on the
 * referenced solution. Owned by the developer who authored it ([ownerId]).
 *
 * @see tech.testsys.domain.model.task.DeveloperSolution
 * @see tech.testsys.domain.model.task.DeveloperSolutionData
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
