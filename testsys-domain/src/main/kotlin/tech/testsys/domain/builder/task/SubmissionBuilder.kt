package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.chooser.SubmissionKindChooser
import tech.testsys.domain.builder.util.chooser.SubmissionStatusChooser
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionData
import tech.testsys.domain.model.task.SubmissionId
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

        return SubmissionData(
            author = author.lazify(),
            solution = solution.lazify(),
            task = task.lazify(),
            status = status.build(),
            kind = kind.build(),
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
