package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity
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
class DeveloperSolutionJpaEntity(
    val name: String,
    @field:JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    val description: String,
    val solutionId: Long,
    val expectedScore: Int,
    val ownerId: Long,
) : SequenceJpaEntity()
