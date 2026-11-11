package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import java.time.Instant

/**
 * Identifier of a [Solution].
 *
 * @since 1.0.0-beta.1
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
 * @since 1.0.0-beta.1
 */
data class SolutionData(
    val file: FileData,
    val language: TrikSupportedLanguage,
)

/**
 * A TRIK Studio program uploaded to the system.
 *
 * @property data the data of the solution.
 * @since 1.0.0-beta.1
 */
class Solution(
    id: SolutionId,
    createdAt: Instant,
    val data: SolutionData,
) : DomainEntity<SolutionId>(id, createdAt)
