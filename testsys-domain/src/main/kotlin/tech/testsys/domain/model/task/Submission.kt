package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId
import java.time.Instant

/**
 * Identifier of a [Recording].
 *
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class RecordingId(
    override val value: Long,
) : DomainId

/**
 * Data of a [Recording].
 *
 * @property file the recording file.
 * @since %CURRENT_VERSION%
 */
class RecordingData(
    val file: FileData,
)

/**
 * A video captured while a graded solution was running in the TRIK Studio world.
 *
 * @property data the data of the recording.
 * @since %CURRENT_VERSION%
 */
class Recording(
    id: RecordingId,
    createdAt: Instant,
    version: EntityVersion,
    val data: RecordingData,
) : DomainEntity<RecordingId>(id, createdAt, version)

/**
 * Identifier of [Logs].
 *
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class LogsId(
    override val value: Long,
) : DomainId

/**
 * Data of [Logs].
 *
 * @property file the log file.
 * @since %CURRENT_VERSION%
 */
class LogsData(
    val file: FileData,
)

/**
 * The log output produced by the grader while checking a solution.
 *
 * @property data the data of the logs.
 * @since %CURRENT_VERSION%
 */
class Logs(
    id: LogsId,
    createdAt: Instant,
    version: EntityVersion,
    val data: LogsData,
) : DomainEntity<LogsId>(id, createdAt, version)

/**
 * Identifier of a [Verdict].
 *
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class VerdictId(
    override val value: Long,
) : DomainId

/**
 * Data of a [Verdict].
 *
 * @property score the score awarded to the solution.
 * @property task the task the solution was graded against.
 * @property submission the submission the verdict was produced for.
 * @property logs the grading logs, or `null` if none were produced.
 * @property recording the recording of the solution run, or `null` if none was produced.
 * @since %CURRENT_VERSION%
 */
data class VerdictData(
    val score: Score,
    val task: LazyEntity<TaskId, Task>,
    val submission: LazyEntity<SubmissionId, Submission>,
    val logs: LazyEntity<LogsId, Logs>?,
    val recording: LazyEntity<RecordingId, Recording>?,
)

/**
 * The result of grading a submitted solution: a score plus optional grading artefacts.
 *
 * @property data the data of the verdict.
 * @since %CURRENT_VERSION%
 */
class Verdict(
    id: VerdictId,
    createdAt: Instant,
    version: EntityVersion,
    val data: VerdictData,
) : DomainEntity<VerdictId>(id, createdAt, version)

/**
 * Identifier of a [Submission].
 *
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class SubmissionId(
    override val value: Long,
) : DomainId

/**
 * Outcome of grading a [Submission], available once grading has finished.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface GradingResult {
    /**
     * Grading completed and produced a verdict.
     *
     * @property verdict the produced verdict.
     * @since %CURRENT_VERSION%
     */
    data class Success(val verdict: LazyEntity<VerdictId, Verdict>) : GradingResult

    /**
     * Grading failed; no verdict was produced.
     *
     * @property description a human-readable description of the failure.
     * @since %CURRENT_VERSION%
     */
    data class GradingError(val description: String) : GradingResult

    /**
     * Grading did not finish in the allotted time; no verdict was produced.
     *
     * @since %CURRENT_VERSION%
     */
    object Timeout : GradingResult
}

/**
 * Position of a [Submission] in its grading lifecycle: queued, in progress, graded.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface SubmissionStatus {
    /**
     * The submission is waiting to be picked up by the grader.
     *
     * @since %CURRENT_VERSION%
     */
    object Queued : SubmissionStatus

    /**
     * The grader is checking the submitted solution.
     *
     * @since %CURRENT_VERSION%
     */
    object InProgress : SubmissionStatus

    /**
     * Grading has finished.
     *
     * @property grade the outcome of the grading.
     * @since %CURRENT_VERSION%
     */
    data class Graded(val grade: GradingResult) : SubmissionStatus
}

/**
 * The context in which a [Submission] was made.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface SubmissionKind {
    /**
     * A test run of a developer's reference solution, made to validate the task itself; not bound to any contest.
     *
     * @since %CURRENT_VERSION%
     */
    object DeveloperSolutionTest : SubmissionKind

    /**
     * A regular submission made within a contest.
     *
     * @property contest the contest the solution was submitted in.
     * @since %CURRENT_VERSION%
     */
    class Grading(val contest: LazyEntity<ContestId, Contest>) : SubmissionKind
}

/**
 * Data of a [Submission].
 *
 * @property author the user who submitted the solution.
 * @property solution the submitted program.
 * @property task the task the solution is graded against.
 * @property status the position of the submission in the grading lifecycle.
 * @property kind the context of the submission.
 * @property judgmentOrders the judges' rulings concerning the submission.
 * @since %CURRENT_VERSION%
 */
data class SubmissionData(
    val author: LazyEntity<UserId, User<UserId>>,
    val solution: LazyEntity<SolutionId, Solution>,
    val task: LazyEntity<TaskId, Task>,
    val status: SubmissionStatus,
    val kind: SubmissionKind,
    val judgmentOrders: LazyEntityList<JudgmentOrderId, JudgmentOrder>,
)

/**
 * A solution sent for grading against a task.
 *
 * @property data the data of the submission.
 * @since %CURRENT_VERSION%
 */
class Submission(
    id: SubmissionId,
    createdAt: Instant,
    version: EntityVersion,
    val data: SubmissionData,
) : DomainEntity<SubmissionId>(id, createdAt, version)
