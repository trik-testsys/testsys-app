package tech.testsys.infra.database.internal.jpa.entity.task

import jakarta.persistence.Entity
import tech.testsys.infra.database.internal.InternalDatabaseApi
import java.util.UUID

/**
 * JPA entity representing a task statement domain entity.
 *
 * A statement is the human-readable problem description bundled with a task.
 *
 * @see tech.testsys.domain.model.task.Statement
 * @see tech.testsys.domain.model.task.StatementData
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class StatementJpaEntity(
    name: String,
    description: String,
    fileDataId: Long,
    versionBucket: UUID,
    id: Long? = null,
) : ResourceJpaEntity(name, description, fileDataId, versionBucket, id)
