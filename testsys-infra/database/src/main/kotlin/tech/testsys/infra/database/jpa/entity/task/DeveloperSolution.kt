package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Entity
import java.util.UUID

/**
 * JPA entity representing a developer solution domain entity.
 *
 * A developer solution is a reference implementation submitted along with a task
 * by its author; it carries an [expectedScore] used to validate grading.
 *
 * @see tech.testsys.domain.model.task.DeveloperSolution
 * @see tech.testsys.domain.model.task.DeveloperSolutionData
 * @since %CURRENT_VERSION%
 */
@Entity
class DeveloperSolutionJpaEntity(
    name: String,
    description: String,
    fileDataId: Long,
    versionBucket: UUID,
    val expectedScore: Int,
) : ResourceJpaEntity(name, description, fileDataId, versionBucket)
