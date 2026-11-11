package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import java.time.Instant

/**
 * Identifier of a [Statement].
 *
 * @since 1.0.0-beta.1
 */
@JvmInline
value class StatementId(
    override val value: Long,
) : DomainId

/**
 * Data of a [Statement].
 *
 * @property file the uploaded statement document; fixed on creation; update fails if it differs.
 * @property name the name of the statement.
 * @property description the description of the statement.
 * @property versionBucket the version chain shared by all versions of the same logical statement;
 *   fixed on creation and ignored on update.
 * @since 1.0.0-beta.1
 */
data class StatementData(
    val file: FileData,
    val name: String,
    val description: String,
    val versionBucket: VersionBucket,
)

/**
 * A PDF/TXT document describing a task to solvers.
 *
 * @property data the data of the statement.
 * @since 1.0.0-beta.1
 */
class Statement(
    id: StatementId,
    createdAt: Instant,
    val data: StatementData,
) : DomainEntity<StatementId>(id, createdAt)
