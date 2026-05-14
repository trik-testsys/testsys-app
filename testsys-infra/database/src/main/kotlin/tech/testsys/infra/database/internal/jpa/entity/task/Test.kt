package tech.testsys.infra.database.internal.jpa.entity.task

import jakarta.persistence.Entity
import tech.testsys.infra.database.internal.InternalDatabaseApi
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
@InternalDatabaseApi
class TestJpaEntity(
    name: String,
    description: String,
    fileDataId: Long,
    versionBucket: UUID,
    id: Long? = null,
) : ResourceJpaEntity(name, description, fileDataId, versionBucket, id)
