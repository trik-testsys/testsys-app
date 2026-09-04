package tech.testsys.infra.database.internal.jpa.entity.task

import jakarta.persistence.Entity
import tech.testsys.infra.database.internal.InternalDatabaseApi
import java.util.UUID

/**
 * JPA entity of [tech.testsys.domain.model.task.Test], a single polygon the grader runs submissions against.
 *
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
