package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Entity
import java.util.UUID

/**
 * JPA entity representing a polygon (test) domain entity.
 *
 * A polygon is a single test case used by the grader to evaluate submissions
 * against a task. Owned by the developer who authored it ([ownerId]).
 *
 * @see tech.testsys.domain.model.task.Test
 * @see tech.testsys.domain.model.task.TestData
 * @since %CURRENT_VERSION%
 */
@Entity
class TestJpaEntity(
    name: String,
    description: String,
    fileDataId: Long,
    versionBucket: UUID,
    val ownerId: Long,
) : ResourceJpaEntity(name, description, fileDataId, versionBucket)
