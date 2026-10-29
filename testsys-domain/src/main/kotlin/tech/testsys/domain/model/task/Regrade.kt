package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import java.time.Instant

/**
 * Identifier of a [Regrade].
 *
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class RegradeId(
    override val value: Long,
) : DomainId

/**
 * Data of a [Regrade].
 *
 * @property task the task whose submissions were sent to grading again; fixed on creation.
 * @property tests the polygons of the committed revision that caused the regrade; fixed on creation.
 * @property submissions the submissions sent to grading again; fixed on creation.
 * @since %CURRENT_VERSION%
 */
data class RegradeData(
    val task: LazyEntity<TaskId, Task>,
    val tests: LazyEntityList<TestId, Test>,
    val submissions: LazyEntityList<SubmissionId, Submission>,
)

/**
 * A record of submissions of a task sent to grading again after a commit changed its set of polygons; the creation
 * moment is the moment the submissions were sent.
 *
 * @property data the data of the regrade.
 * @since %CURRENT_VERSION%
 */
class Regrade(
    id: RegradeId,
    createdAt: Instant,
    val data: RegradeData,
) : DomainEntity<RegradeId>(id, createdAt)
