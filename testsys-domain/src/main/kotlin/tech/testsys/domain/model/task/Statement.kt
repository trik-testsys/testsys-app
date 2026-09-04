package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import java.time.Instant
import java.util.UUID

/**
 * Identifier of a [Statement].
 *
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class StatementId(
    override val value: Long,
) : DomainId

/**
 * Data of a [Statement].
 *
 * @property file the uploaded statement document.
 * @property name the name of the statement.
 * @property description the description of the statement.
 * @property versionBucket the UUID shared by all versions of the same logical statement.
 * @since %CURRENT_VERSION%
 */
data class StatementData(
    val file: FileData,
    val name: String,
    val description: String,
    val versionBucket: UUID,
)

/**
 * A PDF/TXT document describing a task to solvers.
 *
 * @property data the data of the statement.
 * @since %CURRENT_VERSION%
 */
class Statement(
    id: StatementId,
    createdAt: Instant,
    val data: StatementData,
) : DomainEntity<StatementId>(id, createdAt)
