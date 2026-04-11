package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.chooser.SubmissionKindChooser
import tech.testsys.domain.builder.util.chooser.SubmissionStatusChooser
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionData
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.SubmissionKind
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.domain.model.user.MultipleRoleUserId

/**
 * Builder for constructing [VerdictData].
 *
 * @since %CURRENT_VERSION%
 */
class VerdictDataBuilder : Builder<VerdictData> {

    /**
     * The score awarded by this verdict.
     *
     * @since %CURRENT_VERSION%
     */
    var score: Int? = null

    /**
     * The ID of the task this verdict is for.
     *
     * @since %CURRENT_VERSION%
     */
    var task: TaskId? = null

    /**
     * The ID of the submission this verdict is for.
     *
     * @since %CURRENT_VERSION%
     */
    var submission: SubmissionId? = null

    /**
     * Sets the [task] from a raw ID value.
     *
     * @param task the raw task ID.
     * @since %CURRENT_VERSION%
     */
    fun task(task: Long) {
        this.task = TaskId(task)
    }

    /**
     * Sets the [submission] from a raw ID value.
     *
     * @param submission the raw submission ID.
     * @since %CURRENT_VERSION%
     */
    fun submission(submission: Long) {
        this.submission = SubmissionId(submission)
    }

    /**
     * Builds the [VerdictData] instance.
     *
     * @return the constructed [VerdictData].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): VerdictData {
        val score = requireField(score) { ::score }
        val task = requireField(task) { ::task }
        val submission = requireField(submission) { ::submission }

        return VerdictData(
            score = Score(score),
            task = task.lazify(),
            submission = submission.lazify(),
        )
    }

}

/**
 * Builder for constructing [Verdict] domain entities.
 *
 * @since %CURRENT_VERSION%
 */
class VerdictBuilder : DomainEntityWithDataBuilder<Verdict, VerdictData, VerdictDataBuilder>() {

    override fun dataBuilder() = VerdictDataBuilder()

    /**
     * Builds the [Verdict] instance.
     *
     * @return the constructed [Verdict].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): Verdict {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Verdict(
            id = VerdictId(id),
            createdAt = createdAt,
            data = data,
        )
    }

}

/**
 * DSL entry point for building [VerdictData].
 *
 * @param builder the configuration block applied to [VerdictDataBuilder].
 * @return the constructed [VerdictData].
 * @since %CURRENT_VERSION%
 */
inline fun buildVerdictData(builder: VerdictDataBuilder.() -> Unit) = VerdictDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Verdict].
 *
 * @param builder the configuration block applied to [VerdictBuilder].
 * @return the constructed [Verdict].
 * @since %CURRENT_VERSION%
 */
inline fun buildVerdict(builder: VerdictBuilder.() -> Unit) = VerdictBuilder().apply(builder).build()

/**
 * Creates a [GradingResult.Success] with the given verdict ID.
 *
 * @param verdict the verdict ID.
 * @return the success grading result.
 * @since %CURRENT_VERSION%
 */
fun GradingResult.Companion.success(verdict: VerdictId) = GradingResult.Success(verdict.lazify())

/**
 * Creates a [GradingResult.Success] with the given raw verdict ID.
 *
 * @param verdict the raw verdict ID value.
 * @return the success grading result.
 * @since %CURRENT_VERSION%
 */
fun GradingResult.Companion.success(verdict: Long) = VerdictId(verdict).let { GradingResult.success(it) }

/**
 * Creates a [GradingResult.GradingError] with the given description.
 *
 * @param description the error description.
 * @return the grading error result.
 * @since %CURRENT_VERSION%
 */
fun GradingResult.Companion.error(description: String) = GradingResult.GradingError(description)

/**
 * Creates a [GradingResult.Timeout] instance.
 *
 * @return the timeout grading result.
 * @since %CURRENT_VERSION%
 */
fun GradingResult.Companion.timeout() = GradingResult.Timeout

/**
 * Wraps this [GradingResult] into a [SubmissionStatus.Graded] status.
 *
 * @return the graded submission status containing this result.
 * @since %CURRENT_VERSION%
 */
fun GradingResult.graded() = SubmissionStatus.Graded(this)

/**
 * Creates a [SubmissionStatus.Graded] status from a [GradingResult] configured via the [builder] block.
 *
 * @param builder a lambda on [GradingResult.Companion] that returns the grading result.
 * @return the graded submission status.
 * @since %CURRENT_VERSION%
 */
inline fun SubmissionStatus.Companion.graded(builder: GradingResult.Companion.() -> GradingResult) =
    builder(GradingResult.Companion).graded()

/**
 * Creates a [SubmissionStatus.Queued] status.
 *
 * @return the queued submission status.
 * @since %CURRENT_VERSION%
 */
fun SubmissionStatus.Companion.queued() = SubmissionStatus.Queued

/**
 * Creates a [SubmissionStatus.InProgress] status.
 *
 * @return the in-progress submission status.
 * @since %CURRENT_VERSION%
 */
fun SubmissionStatus.Companion.inProgress() = SubmissionStatus.InProgress

/**
 * Creates a [SubmissionKind.DeveloperSolutionTest] kind.
 *
 * @return the developer solution test submission kind.
 * @since %CURRENT_VERSION%
 */
fun SubmissionKind.Companion.developerSolutionTest() = SubmissionKind.DeveloperSolutionTest

/**
 * Creates a [SubmissionKind.Grading] kind for the given contest.
 *
 * @param contest the contest ID.
 * @return the grading submission kind.
 * @since %CURRENT_VERSION%
 */
fun SubmissionKind.Companion.grading(contest: ContestId) = SubmissionKind.Grading(contest.lazify())

/**
 * Creates a [SubmissionKind.Grading] kind for the given raw contest ID.
 *
 * @param contest the raw contest ID value.
 * @return the grading submission kind.
 * @since %CURRENT_VERSION%
 */
fun SubmissionKind.Companion.grading(contest: Long) = ContestId(contest).let { SubmissionKind.grading(it) }

/**
 * Builder for constructing [SubmissionData].
 *
 * @since %CURRENT_VERSION%
 */
class SubmissionDataBuilder : Builder<SubmissionData> {

    /**
     * The author of the submission.
     *
     * @since %CURRENT_VERSION%
     */
    var author: MultipleRoleUserId? = null

    /**
     * Sets the [author] from a raw ID value.
     *
     * @param author the raw author ID.
     * @since %CURRENT_VERSION%
     */
    fun author(author: Long) {
        this.author = MultipleRoleUserId(author)
    }

    /**
     * The ID of the solution being submitted.
     *
     * @since %CURRENT_VERSION%
     */
    var solution: SolutionId? = null

    /**
     * The ID of the task this submission is for.
     *
     * @since %CURRENT_VERSION%
     */
    var task: TaskId? = null

    /**
     * The list of judgment order IDs associated with this submission.
     *
     * @since %CURRENT_VERSION%
     */
    var judgmentOrders = mutableListOf<JudgmentOrderId>()

    /**
     * Sets the [solution] from a raw ID value.
     *
     * @param solution the raw solution ID.
     * @since %CURRENT_VERSION%
     */
    fun solution(solution: Long) {
        this.solution = SolutionId(solution)
    }

    /**
     * Sets the [task] from a raw ID value.
     *
     * @param task the raw task ID.
     * @since %CURRENT_VERSION%
     */
    fun task(task: Long) {
        this.task = TaskId(task)
    }

    /**
     * Chooser for selecting the submission status.
     *
     * @since %CURRENT_VERSION%
     */
    val status = SubmissionStatusChooser()

    /**
     * Chooser for selecting the submission kind.
     *
     * @since %CURRENT_VERSION%
     */
    val kind = SubmissionKindChooser()

    /**
     * Sets the [judgmentOrders] list from raw ID values.
     *
     * @param orders the raw judgment order IDs.
     * @since %CURRENT_VERSION%
     */
    fun judgmentOrders(orders: Iterable<Long>) {
        this.judgmentOrders = orders.map { JudgmentOrderId(it) }.toMutableList()
    }

    /**
     * Builds the [SubmissionData] instance.
     *
     * @return the constructed [SubmissionData].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): SubmissionData {
        val author = requireField(author) { ::author }
        val solution = requireField(solution) { ::solution }
        val task = requireField(task) { ::task }
        val status = requireField(status.choice) { status::choice }
        val kind = requireField(kind.choice) { kind::choice }

        return SubmissionData(
            author = author.lazify(),
            solution = solution.lazify(),
            task = task.lazify(),
            status = status,
            kind = kind,
            judgmentOrders = judgmentOrders.lazify()
        )
    }

}

/**
 * Builder for constructing [Submission] domain entities.
 *
 * @since %CURRENT_VERSION%
 */
class SubmissionBuilder : DomainEntityWithDataBuilder<Submission, SubmissionData, SubmissionDataBuilder>() {

    override fun dataBuilder() = SubmissionDataBuilder()

    /**
     * Builds the [Submission] instance.
     *
     * @return the constructed [Submission].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): Submission {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Submission(
            id = SubmissionId(id),
            createdAt = createdAt,
            data = data,
        )
    }

}

/**
 * DSL entry point for building [SubmissionData].
 *
 * @param builder the configuration block applied to [SubmissionDataBuilder].
 * @return the constructed [SubmissionData].
 * @since %CURRENT_VERSION%
 */
inline fun buildSubmissionData(builder: SubmissionDataBuilder.() -> Unit) =
    SubmissionDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Submission].
 *
 * @param builder the configuration block applied to [SubmissionBuilder].
 * @return the constructed [Submission].
 * @since %CURRENT_VERSION%
 */
inline fun buildSubmission(builder: SubmissionBuilder.() -> Unit) =
    SubmissionBuilder().apply(builder).build()
