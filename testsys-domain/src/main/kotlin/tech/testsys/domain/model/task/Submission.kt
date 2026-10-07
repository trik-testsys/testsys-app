package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
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
 * @property file the recording file; fixed on creation.
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
    val data: RecordingData,
) : DomainEntity<RecordingId>(id, createdAt)

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
 * @property file the log file; fixed on creation.
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
    val data: LogsData,
) : DomainEntity<LogsId>(id, createdAt)

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
 * The outcome of running a graded solution on a single test of a [Verdict].
 *
 * @property score the score awarded for the test.
 * @property test the test the solution was run on.
 * @property logs the grading logs of the run; every run produces them.
 * @property recording the recording of the run, or `null` if none was produced.
 * @since %CURRENT_VERSION%
 */
data class TestVerdict(
    val score: Score,
    val test: LazyEntity<TestId, Test>,
    val logs: LazyEntity<LogsId, Logs>,
    val recording: LazyEntity<RecordingId, Recording>?,
)

/**
 * Data of a [Verdict]; the total score of the verdict is the sum of the scores of [testVerdicts].
 *
 * @property task the task the solution was graded against; fixed on creation.
 * @property submission the submission the verdict was produced for; fixed on creation.
 * @property testVerdicts the non-empty list of outcomes of the runs on the task tests; fixed on creation.
 * @throws IllegalArgumentException if [testVerdicts] is empty.
 * @since %CURRENT_VERSION%
 */
data class VerdictData(
    val task: LazyEntity<TaskId, Task>,
    val submission: LazyEntity<SubmissionId, Submission>,
    val testVerdicts: List<TestVerdict>,
) {
    init {
        require(testVerdicts.isNotEmpty()) {
            "Verdict for submission ${submission.id.value} must contain at least one test outcome"
        }
    }
}

/**
 * The result of grading a submitted solution: a score and grading logs per test plus an optional recording.
 *
 * @property data the data of the verdict.
 * @since %CURRENT_VERSION%
 */
class Verdict(
    id: VerdictId,
    createdAt: Instant,
    val data: VerdictData,
) : DomainEntity<VerdictId>(id, createdAt)

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
     * @property trikStudioVersion the TRIK Studio version used for the test run.
     * @since %CURRENT_VERSION%
     */
    data class DeveloperSolutionTest(val trikStudioVersion: TrikStudioVersion) : SubmissionKind

    /**
     * A regular submission made within a contest.
     *
     * @property contest the contest the solution was submitted in.
     * @since %CURRENT_VERSION%
     */
    data class Grading(val contest: LazyEntity<ContestId, Contest>) : SubmissionKind
}

/**
 * Data of a [Submission].
 *
 * @property author the user who submitted the solution; fixed on creation and ignored on update.
 * @property solution the submitted program; fixed on creation and ignored on update.
 * @property task the task the solution is graded against; fixed on creation and ignored on update.
 * @property status the position of the submission in the grading lifecycle.
 * @property kind the context of the submission; fixed on creation and ignored on update.
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
    val data: SubmissionData,
) : DomainEntity<SubmissionId>(id, createdAt)
