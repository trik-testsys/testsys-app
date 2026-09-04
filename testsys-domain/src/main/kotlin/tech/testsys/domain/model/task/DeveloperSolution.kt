package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.LazyEntity
import java.time.Instant
import java.util.UUID

/**
 * Identifier of a [DeveloperSolution].
 *
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class DeveloperSolutionId(
    override val value: Long,
) : DomainId

/**
 * Data of a [DeveloperSolution].
 *
 * @property name the name of the developer solution.
 * @property description the description of the developer solution.
 * @property solution the reference program.
 * @property expectedScore the score the reference program is expected to get when graded against the task.
 * @property versionBucket the UUID shared by all versions of the same logical developer solution.
 * @since %CURRENT_VERSION%
 */
data class DeveloperSolutionData(
    val name: String,
    val description: String,
    val solution: LazyEntity<SolutionId, Solution>,
    val expectedScore: Score,
    val versionBucket: UUID,
)

/**
 * A task author's reference solution together with the score it is expected to receive; used to validate the task.
 *
 * @property data the data of the developer solution.
 * @since %CURRENT_VERSION%
 */
class DeveloperSolution(
    id: DeveloperSolutionId,
    createdAt: Instant,
    version: EntityVersion,
    val data: DeveloperSolutionData,
) : DomainEntity<DeveloperSolutionId>(id, createdAt, version)
