package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.user.MultipleRoleUserBuilder
import tech.testsys.domain.builder.util.chooser.SubmissionKindChooser
import tech.testsys.domain.builder.util.chooser.SubmissionStatusChooser
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.JudgmentOrderId
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
import tech.testsys.domain.model.user.MultipleRoleUser

class VerdictDataBuilder : Builder<VerdictData> {

    var score: Int? = null
    var task: TaskId? = null
    var submission: SubmissionId? = null

    fun task(task: Long) {
        this.task = TaskId(task)
    }

    fun submission(submission: Long) {
        this.submission = SubmissionId(submission)
    }

    override fun build(): VerdictData {
        val score = requireNotNull(score)
        val task = requireNotNull(task)
        val submission = requireNotNull(submission)

        return VerdictData(
            score = score,
            task = task.lazify(),
            submission = submission.lazify(),
        )
    }

}

class VerdictBuilder : DomainEntityWithDataBuilder<Verdict, VerdictData, VerdictDataBuilder>() {

    override fun dataBuilder() = VerdictDataBuilder()

    override fun build(): Verdict {
        val id = requireNotNull(id)
        val createdAt = requireNotNull(createdAt)
        val data = requireNotNull(data)

        return Verdict(
            id = VerdictId(id),
            createdAt = createdAt,
            data = data,
        )
    }

}

inline fun buildVerdictData(builder: VerdictDataBuilder.() -> Unit) = VerdictDataBuilder().apply(builder).build()

inline fun buildVerdict(builder: VerdictBuilder.() -> Unit) = VerdictBuilder().apply(builder).build()

fun GradingResult.Companion.success(verdict: VerdictId) = GradingResult.Success(verdict.lazify())
fun GradingResult.Companion.success(verdict: Long) = VerdictId(verdict).let { GradingResult.success(it) }
fun GradingResult.Companion.error(description: String) = GradingResult.GradingError(description)
fun GradingResult.Companion.timeout() = GradingResult.Timeout

fun GradingResult.graded() = SubmissionStatus.Graded(this)
inline fun SubmissionStatus.Companion.graded(builder: GradingResult.Companion.() -> GradingResult) =
    builder(GradingResult.Companion).graded()
fun SubmissionStatus.Companion.queued() = SubmissionStatus.Queued
fun SubmissionStatus.Companion.inProgress() = SubmissionStatus.InProgress

fun SubmissionKind.Companion.developerSolutionTest() = SubmissionKind.DeveloperSolutionTest
fun SubmissionKind.Companion.grading(contest: ContestId) = SubmissionKind.Grading(contest.lazify())
fun SubmissionKind.Companion.grading(contest: Long) = ContestId(contest).let { SubmissionKind.grading(it) }

class SubmissionDataBuilder : Builder<SubmissionData> {

    var author: MultipleRoleUser? = null
    var solution: SolutionId? = null
    var task: TaskId? = null
    var judgmentOrders = mutableListOf<JudgmentOrderId>()

    fun author(builder: MultipleRoleUserBuilder.() -> Unit) {
        author = MultipleRoleUserBuilder().apply(builder).build()
    }

    fun solution(solution: Long) {
        this.solution = SolutionId(solution)
    }

    fun task(task: Long) {
        this.task = TaskId(task)
    }

    val status = SubmissionStatusChooser()

    val kind = SubmissionKindChooser()

    fun judgmentOrders(orders: Iterable<Long>) {
        this.judgmentOrders = orders.map { JudgmentOrderId(it) }.toMutableList()
    }

    override fun build(): SubmissionData {
        val author = requireNotNull(author)
        val solution = requireNotNull(solution)
        val task = requireNotNull(task)
        val status = requireNotNull(status.choice)
        val kind = requireNotNull(kind.choice)

        return SubmissionData(
            author = author,
            solution = solution.lazify(),
            task = task.lazify(),
            status = status,
            kind = kind,
            judgmentOrders = judgmentOrders.lazify()
        )
    }

}

class SubmissionBuilder : DomainEntityWithDataBuilder<Submission, SubmissionData, SubmissionDataBuilder>() {

    override fun dataBuilder() = SubmissionDataBuilder()

    override fun build(): Submission {
        val id = requireNotNull(id)
        val createdAt = requireNotNull(createdAt)
        val data = requireNotNull(data)

        return Submission(
            id = SubmissionId(id),
            createdAt = createdAt,
            data = data,
        )
    }

}

inline fun buildSubmissionData(builder: SubmissionDataBuilder.() -> Unit) =
    SubmissionDataBuilder().apply(builder).build()

inline fun buildSubmission(builder: SubmissionBuilder.() -> Unit) =
    SubmissionBuilder().apply(builder).build()
