package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import java.time.Instant

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
 * @property file the uploaded program file.
 * @property language the programming language of the program.
 * @since %CURRENT_VERSION%
 */
data class SolutionData(
    val file: FileData,
    val language: TrikSupportedLanguage,
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
