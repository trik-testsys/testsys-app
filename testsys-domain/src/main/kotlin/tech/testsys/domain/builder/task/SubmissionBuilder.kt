package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.applyVersion
import tech.testsys.domain.builder.util.chooser.SubmissionKindChooser
import tech.testsys.domain.builder.util.chooser.SubmissionStatusChooser
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.LogsId
import tech.testsys.domain.model.task.RecordingId
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionData
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TestVerdict
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.UserId

/**
 * Builder of [TestVerdict]. Required: [score], [test], [logs].
 *
 * @property score the score awarded for the test, or `null` if not set yet.
 * @property test the id of the test the solution was run on, or `null` if not set yet.
 * @property logs the id of the grading logs of the run, or `null` if not set yet.
 * @property recording the id of the run recording, or `null` if there is none.
 * @since %CURRENT_VERSION%
 */
class TestVerdictBuilder : Builder<TestVerdict> {

    var score: Int? = null

    var test: TestId? = null

    var logs: LogsId? = null

    var recording: RecordingId? = null

    /**
     * Sets [test] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun test(test: Long) {
        this.test = TestId(test)
    }

    /**
     * Sets [logs] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun logs(logs: Long) {
        this.logs = LogsId(logs)
    }

    /**
     * Sets [recording] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun recording(recording: Long) {
        this.recording = RecordingId(recording)
    }

    override fun build(): TestVerdict {
        val score = requireField(score) { ::score }
        val test = requireField(test) { ::test }
        val logs = requireField(logs) { ::logs }

        return TestVerdict(
            score = Score(score),
            test = test.lazify(),
            logs = logs.lazify(),
            recording = recording?.lazify(),
        )
    }
}

/**
 * Builder of [VerdictData]. Required: [task], [submission], non-empty [testVerdicts].
 *
 * @property task the id of the graded task, or `null` if not set yet.
 * @property submission the id of the graded submission, or `null` if not set yet.
 * @property testVerdicts the outcomes of the runs on the task tests; at least one is required.
 * @since %CURRENT_VERSION%
 */
class VerdictDataBuilder : Builder<VerdictData> {

    var task: TaskId? = null

    var submission: SubmissionId? = null

    var testVerdicts = mutableListOf<TestVerdict>()

    /**
     * Sets [task] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun task(task: Long) {
        this.task = TaskId(task)
    }

    /**
     * Sets [submission] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun submission(submission: Long) {
        this.submission = SubmissionId(submission)
    }

    /**
     * Adds a [TestVerdict] built with a [TestVerdictBuilder] block to [testVerdicts].
     *
     * @since %CURRENT_VERSION%
     */
    fun testVerdict(builder: TestVerdictBuilder.() -> Unit) {
        testVerdicts.add(TestVerdictBuilder().apply(builder).build())
    }

    override fun build(): VerdictData {
        val task = requireField(task) { ::task }
        val submission = requireField(submission) { ::submission }

        return VerdictData(
            task = task.lazify(),
            submission = submission.lazify(),
            testVerdicts = testVerdicts.toList(),
        )
    }
}

/**
 * Builder of [Verdict] entities. Required: [id], [createdAt], [data].
 *
 * @since %CURRENT_VERSION%
 */
class VerdictBuilder : DomainEntityWithDataBuilder<Verdict, VerdictData, VerdictDataBuilder>() {

    override fun dataBuilder() = VerdictDataBuilder()

    override fun build(): Verdict {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Verdict(
            id = VerdictId(id),
            createdAt = createdAt,
            data = data,
        ).applyVersion(version)
    }
}

/**
 * Builder of [SubmissionData]. Required: [author], [solution], [task], choices in [status] and [kind].
 *
 * @property author the id of the submitting user, or `null` if not set yet.
 * @property solution the id of the submitted solution, or `null` if not set yet.
 * @property task the id of the task, or `null` if not set yet.
 * @property judgmentOrders the ids of the judgment orders concerning the submission.
 * @property status the chooser of the submission status.
 * @property kind the chooser of the submission kind.
 * @since %CURRENT_VERSION%
 */
class SubmissionDataBuilder : Builder<SubmissionData> {

    var author: UserId? = null

    var solution: SolutionId? = null

    var task: TaskId? = null

    var judgmentOrders = mutableListOf<JudgmentOrderId>()

    val status = SubmissionStatusChooser()

    val kind = SubmissionKindChooser()

    /**
     * Sets [author] from a raw id as a [MultipleRoleUserId]; assign [author] directly for other [UserId] kinds.
     *
     * @since %CURRENT_VERSION%
     */
    fun author(author: Long) {
        this.author = MultipleRoleUserId(author)
    }

    /**
     * Sets [solution] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun solution(solution: Long) {
        this.solution = SolutionId(solution)
    }

    /**
     * Sets [task] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun task(task: Long) {
        this.task = TaskId(task)
    }

    /**
     * Sets [judgmentOrders] from raw ids.
     *
     * @since %CURRENT_VERSION%
     */
    fun judgmentOrders(orders: Iterable<Long>) {
        this.judgmentOrders = orders.map { JudgmentOrderId(it) }.toMutableList()
    }

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
            judgmentOrders = judgmentOrders.lazify(),
        )
    }
}

/**
 * Builder of [Submission] entities. Required: [id], [createdAt], [data].
 *
 * @since %CURRENT_VERSION%
 */
class SubmissionBuilder : DomainEntityWithDataBuilder<Submission, SubmissionData, SubmissionDataBuilder>() {

    override fun dataBuilder() = SubmissionDataBuilder()

    override fun build(): Submission {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Submission(
            id = SubmissionId(id),
            createdAt = createdAt,
            data = data,
        ).applyVersion(version)
    }
}
