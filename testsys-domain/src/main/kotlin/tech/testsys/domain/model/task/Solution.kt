package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import java.time.Instant
import java.util.UUID

/**
 * Identifier of a [Solution].
 *
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class SolutionId(
    override val value: Long,
) : DomainId

/**
 * Data of a [Solution].
 *
 * @property file the uploaded program file; fixed on creation.
 * @property language the programming language of the program; fixed on creation.
 * @property versionBucket the UUID shared by all versions of the same logical solution; fixed on creation.
 * @since %CURRENT_VERSION%
 */
data class SolutionData(
    val file: FileData,
    val language: TrikSupportedLanguage,
    val versionBucket: UUID,
)

/**
 * A TRIK Studio program uploaded to the system.
 *
 * @property data the data of the solution.
 * @since %CURRENT_VERSION%
 */
class Solution(
    id: SolutionId,
    createdAt: Instant,
    val data: SolutionData,
) : DomainEntity<SolutionId>(id, createdAt)
