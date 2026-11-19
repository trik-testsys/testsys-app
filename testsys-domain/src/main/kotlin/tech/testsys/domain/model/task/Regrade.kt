package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import java.time.Instant

/**
 * Identifier of a [Regrade].
 *
 * @since 1.0.0-beta.1
 */
@JvmInline
value class RegradeId(
    override val value: Long,
) : DomainId

/**
 * Data of a [Regrade].
 *
 * @property task the task whose submissions were sent to grading again; fixed on creation.
 * @property tests the polygons of the last committed revision at the moment of the regrade; fixed on creation.
 * @property submissions the submissions sent to grading again; fixed on creation.
 * @property stoppedAt the creation moment of the next regrade of the task that stopped this unfinished one, or `null`
 *   if it was not stopped; changed only by `RegradeRepository.stopPrevious`.
 * @since 1.0.0-beta.1
 */
data class RegradeData(
    val task: LazyEntity<TaskId, Task>,
    val tests: LazyEntityList<TestId, Test>,
    val submissions: LazyEntityList<SubmissionId, Submission>,
    val stoppedAt: Instant? = null,
)

/**
 * A record of submissions of a task sent to grading again after a commit changed its set of polygons or at the
 * developer's request; the creation moment is the moment the submissions were sent, and the next regrade of the task
 * can stop it while it is unfinished.
 *
 * @property data the data of the regrade.
 * @since 1.0.0-beta.1
 */
class Regrade(
    id: RegradeId,
    createdAt: Instant,
    val data: RegradeData,
) : DomainEntity<RegradeId>(id, createdAt)
