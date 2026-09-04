package tech.testsys.domain.builder.api

import tech.testsys.domain.builder.task.CommittedTaskContentBuilder
import tech.testsys.domain.builder.task.ContestBuilder
import tech.testsys.domain.builder.task.ContestDataBuilder
import tech.testsys.domain.builder.task.DeveloperSolutionBuilder
import tech.testsys.domain.builder.task.DeveloperSolutionDataBuilder
import tech.testsys.domain.builder.task.ExerciseBuilder
import tech.testsys.domain.builder.task.ExerciseDataBuilder
import tech.testsys.domain.builder.task.JudgmentOrderBuilder
import tech.testsys.domain.builder.task.JudgmentOrderDataBuilder
import tech.testsys.domain.builder.task.LogsBuilder
import tech.testsys.domain.builder.task.LogsDataBuilder
import tech.testsys.domain.builder.task.RecordingBuilder
import tech.testsys.domain.builder.task.RecordingDataBuilder
import tech.testsys.domain.builder.task.SolutionBuilder
import tech.testsys.domain.builder.task.SolutionDataBuilder
import tech.testsys.domain.builder.task.StatementBuilder
import tech.testsys.domain.builder.task.StatementDataBuilder
import tech.testsys.domain.builder.task.SubmissionBuilder
import tech.testsys.domain.builder.task.SubmissionDataBuilder
import tech.testsys.domain.builder.task.TaskBuilder
import tech.testsys.domain.builder.task.TaskDataBuilder
import tech.testsys.domain.builder.task.TestBuilder
import tech.testsys.domain.builder.task.TestDataBuilder
import tech.testsys.domain.builder.task.VerdictBuilder
import tech.testsys.domain.builder.task.VerdictDataBuilder
import tech.testsys.domain.builder.task.WipTaskContentBuilder
import tech.testsys.domain.builder.util.chooser.TaskContentChooser
import tech.testsys.domain.model.task.CommitedTaskContent
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestData
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionData
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseData
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderData
import tech.testsys.domain.model.task.Logs
import tech.testsys.domain.model.task.LogsData
import tech.testsys.domain.model.task.Recording
import tech.testsys.domain.model.task.RecordingData
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionData
import tech.testsys.domain.model.task.Statement
import tech.testsys.domain.model.task.StatementData
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionData
import tech.testsys.domain.model.task.SubmissionKind
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestData
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData
import tech.testsys.domain.model.task.WipTaskContent

/**
 * Builds [ContestData] with a [ContestDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun contestData(builder: ContestDataBuilder.() -> Unit) = ContestDataBuilder().apply(builder).build()

/**
 * Builds a [Contest] with a [ContestBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun contest(builder: ContestBuilder.() -> Unit) = ContestBuilder().apply(builder).build()

/**
 * Builds [DeveloperSolutionData] with a [DeveloperSolutionDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun developerSolutionData(builder: DeveloperSolutionDataBuilder.() -> Unit) = DeveloperSolutionDataBuilder().apply(builder).build()

/**
 * Builds a [DeveloperSolution] with a [DeveloperSolutionBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun developerSolution(builder: DeveloperSolutionBuilder.() -> Unit) = DeveloperSolutionBuilder().apply(builder).build()

/**
 * Builds [ExerciseData] with an [ExerciseDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun exerciseData(builder: ExerciseDataBuilder.() -> Unit) = ExerciseDataBuilder().apply(builder).build()

/**
 * Builds an [Exercise] with an [ExerciseBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun exercise(builder: ExerciseBuilder.() -> Unit) = ExerciseBuilder().apply(builder).build()

/**
 * Builds [JudgmentOrderData] with a [JudgmentOrderDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun judgmentOrderData(builder: JudgmentOrderDataBuilder.() -> Unit) = JudgmentOrderDataBuilder().apply(builder).build()

/**
 * Builds a [JudgmentOrder] with a [JudgmentOrderBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun judgmentOrder(builder: JudgmentOrderBuilder.() -> Unit) = JudgmentOrderBuilder().apply(builder).build()

/**
 * Builds [LogsData] with a [LogsDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun logsData(builder: LogsDataBuilder.() -> Unit) = LogsDataBuilder().apply(builder).build()

/**
 * Builds [Logs] with a [LogsBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun logs(builder: LogsBuilder.() -> Unit) = LogsBuilder().apply(builder).build()

/**
 * Builds [RecordingData] with a [RecordingDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun recordingData(builder: RecordingDataBuilder.() -> Unit) = RecordingDataBuilder().apply(builder).build()

/**
 * Builds a [Recording] with a [RecordingBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun recording(builder: RecordingBuilder.() -> Unit) = RecordingBuilder().apply(builder).build()

/**
 * Builds [SolutionData] with a [SolutionDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun solutionData(builder: SolutionDataBuilder.() -> Unit) = SolutionDataBuilder().apply(builder).build()

/**
 * Builds a [Solution] with a [SolutionBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun solution(builder: SolutionBuilder.() -> Unit) = SolutionBuilder().apply(builder).build()

/**
 * Builds [VerdictData] with a [VerdictDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun verdictData(builder: VerdictDataBuilder.() -> Unit) = VerdictDataBuilder().apply(builder).build()

/**
 * Builds a [Verdict] with a [VerdictBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun verdict(builder: VerdictBuilder.() -> Unit) = VerdictBuilder().apply(builder).build()

/**
 * Wraps this result into a [SubmissionStatus.Graded].
 *
 * @since %CURRENT_VERSION%
 */
fun GradingResult.graded() = SubmissionStatus.Graded(this)

/**
 * Builds [StatementData] with a [StatementDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun statementData(builder: StatementDataBuilder.() -> Unit) = StatementDataBuilder().apply(builder).build()

/**
 * Builds a [Statement] with a [StatementBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun statement(builder: StatementBuilder.() -> Unit) = StatementBuilder().apply(builder).build()

/**
 * Builds [SubmissionData] with a [SubmissionDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun submissionData(builder: SubmissionDataBuilder.() -> Unit) = SubmissionDataBuilder().apply(builder).build()

/**
 * Builds a [Submission] with a [SubmissionBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun submission(builder: SubmissionBuilder.() -> Unit) = SubmissionBuilder().apply(builder).build()

/**
 * Builds [TaskData] with a [TaskDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun taskData(builder: TaskDataBuilder.() -> Unit) = TaskDataBuilder().apply(builder).build()

/**
 * Builds [TaskContent.New] with a [WipTaskContentBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun taskContentNew(builder: WipTaskContentBuilder.() -> Unit) = TaskContent.New(WipTaskContentBuilder().apply(builder).build())

/**
 * Builds [TaskContent.Committed] with a [CommittedTaskContentBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun taskContentCommited(builder: CommittedTaskContentBuilder.() -> Unit) =
    TaskContent.Committed(CommittedTaskContentBuilder().apply(builder).build())

/**
 * Builds [TaskContent.Uncommited].
 *
 * @param wipBuilder the [WipTaskContentBuilder] block of the work-in-progress revision.
 * @param lastCommitedBuilder the [CommittedTaskContentBuilder] block of the last committed revision.
 * @since %CURRENT_VERSION%
 */
inline fun taskContentUncommited(
    wipBuilder: WipTaskContentBuilder.() -> Unit,
    lastCommitedBuilder: CommittedTaskContentBuilder.() -> Unit,
) = TaskContent.Uncommited(
    wip = WipTaskContentBuilder().apply(wipBuilder).build(),
    lastCommited = CommittedTaskContentBuilder().apply(lastCommitedBuilder).build(),
)

/**
 * Builds a [Task] with a [TaskBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun task(builder: TaskBuilder.() -> Unit) = TaskBuilder().apply(builder).build()

/**
 * Builds [TestData] with a [TestDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun testData(builder: TestDataBuilder.() -> Unit) = TestDataBuilder().apply(builder).build()

/**
 * Builds a [Test] with a [TestBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun test(builder: TestBuilder.() -> Unit) = TestBuilder().apply(builder).build()

private fun SubmissionData.toBuilder(): SubmissionDataBuilder {
    val thisData = this
    return SubmissionDataBuilder().apply {
        author = thisData.author.id
        solution = thisData.solution.id
        task = thisData.task.id
        judgmentOrders = thisData.judgmentOrders.ids.toMutableList()

        when (val originStatus = thisData.status) {
            is SubmissionStatus.Graded -> {
                status.graded {
                    when (val originGrade = originStatus.grade) {
                        is GradingResult.GradingError -> status.error { description = originGrade.description }
                        is GradingResult.Success -> status.success { verdict = originGrade.verdict.id }
                        GradingResult.Timeout -> status.timeout()
                    }
                }
            }
            SubmissionStatus.InProgress -> status.inProgress()
            SubmissionStatus.Queued -> status.queued()
        }

        when (val originKind = thisData.kind) {
            SubmissionKind.DeveloperSolutionTest -> kind.developerSolutionTest()
            is SubmissionKind.Grading -> kind.grading { contest = originKind.contest.id }
        }
    }
}

/**
 * Returns a copy of this submission with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun Submission.withData(builder: SubmissionDataBuilder.() -> Unit): Submission {
    return Submission(
        this.id,
        this.createdAt,
        this.data.toBuilder().apply(builder).build(),
    )
}

private fun ContestData.toBuilder(): ContestDataBuilder {
    val thisData = this
    return ContestDataBuilder().apply {
        owner = thisData.owner.id
        name = thisData.name
        description = thisData.description
        tasks = thisData.tasks.ids.toMutableList()
        startsAt = thisData.startsAt
        contestDuration = thisData.contestDuration
        attemptDuration = thisData.attemptDuration
        trikStudioVersion = thisData.trikStudioVersion
        sharedTo = thisData.sharedTo.ids.toMutableList()
    }
}

/**
 * Returns a copy of this contest with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun Contest.withData(builder: ContestDataBuilder.() -> Unit): Contest {
    return Contest(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun DeveloperSolutionData.toBuilder(): DeveloperSolutionDataBuilder {
    val thisData = this
    return DeveloperSolutionDataBuilder().apply {
        name = thisData.name
        description = thisData.description
        solution = thisData.solution.id
        expectedScore = thisData.expectedScore
        versionBucket = thisData.versionBucket
    }
}

/**
 * Returns a copy of this developer solution with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun DeveloperSolution.withData(builder: DeveloperSolutionDataBuilder.() -> Unit): DeveloperSolution {
    return DeveloperSolution(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun ExerciseData.toBuilder(): ExerciseDataBuilder {
    val thisData = this
    return ExerciseDataBuilder().apply {
        name = thisData.name
        description = thisData.description
        file(thisData.file.uploadedFilename, thisData.file.content)
        when (thisData.language) {
            TrikSupportedLanguage.Python -> language.python()
            TrikSupportedLanguage.JavaScript -> language.javaScript()
            TrikSupportedLanguage.VisualLanguage -> language.visualLanguage()
        }
        versionBucket = thisData.versionBucket
    }
}

/**
 * Returns a copy of this exercise with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun Exercise.withData(builder: ExerciseDataBuilder.() -> Unit): Exercise {
    return Exercise(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun JudgmentOrderData.toBuilder(): JudgmentOrderDataBuilder {
    val thisData = this
    return JudgmentOrderDataBuilder().apply {
        judge = thisData.judge.id
        verdict = thisData.verdict.id
        reason = thisData.reason
    }
}

/**
 * Returns a copy of this judgment order with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun JudgmentOrder.withData(builder: JudgmentOrderDataBuilder.() -> Unit): JudgmentOrder {
    return JudgmentOrder(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun LogsData.toBuilder(): LogsDataBuilder {
    val thisData = this
    return LogsDataBuilder().apply {
        file(thisData.file.uploadedFilename, thisData.file.content)
    }
}

/**
 * Returns a copy of these logs with their data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun Logs.withData(builder: LogsDataBuilder.() -> Unit): Logs {
    return Logs(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun RecordingData.toBuilder(): RecordingDataBuilder {
    val thisData = this
    return RecordingDataBuilder().apply {
        file(thisData.file.uploadedFilename, thisData.file.content)
    }
}

/**
 * Returns a copy of this recording with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun Recording.withData(builder: RecordingDataBuilder.() -> Unit): Recording {
    return Recording(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun SolutionData.toBuilder(): SolutionDataBuilder {
    val thisData = this
    return SolutionDataBuilder().apply {
        file(thisData.file.uploadedFilename, thisData.file.content)
        when (thisData.language) {
            TrikSupportedLanguage.Python -> language.python()
            TrikSupportedLanguage.JavaScript -> language.javaScript()
            TrikSupportedLanguage.VisualLanguage -> language.visualLanguage()
        }
    }
}

/**
 * Returns a copy of this solution with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun Solution.withData(builder: SolutionDataBuilder.() -> Unit): Solution {
    return Solution(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun StatementData.toBuilder(): StatementDataBuilder {
    val thisData = this
    return StatementDataBuilder().apply {
        name = thisData.name
        description = thisData.description
        file(thisData.file.uploadedFilename, thisData.file.content)
        versionBucket = thisData.versionBucket
    }
}

/**
 * Returns a copy of this statement with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun Statement.withData(builder: StatementDataBuilder.() -> Unit): Statement {
    return Statement(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun TestData.toBuilder(): TestDataBuilder {
    val thisData = this
    return TestDataBuilder().apply {
        name = thisData.name
        description = thisData.description
        file(thisData.file.uploadedFilename, thisData.file.content)
        versionBucket = thisData.versionBucket
    }
}

/**
 * Returns a copy of this test with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun Test.withData(builder: TestDataBuilder.() -> Unit): Test {
    return Test(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun WipTaskContentBuilder.populateFrom(content: WipTaskContent) {
    tests = content.tests.ids.toMutableList()
    exercise = content.exercise?.id
    statement = content.statement?.id
    developerSolutions = content.developerSolutions.ids.toMutableList()
    supportedTrikStudioVersions = content.supportedTrikStudioVersions.toMutableList()
}

private fun CommittedTaskContentBuilder.populateFrom(content: CommitedTaskContent) {
    tests = content.tests.ids.toMutableList()
    exercise = content.exercise.id
    statement = content.statement.id
    developerSolutions = content.developerSolutions.ids.toMutableList()
    supportedTrikStudioVersions = content.supportedTrikStudioVersions.toMutableList()
}

private fun TaskContentChooser.populateFrom(taskContent: TaskContent) {
    when (taskContent) {
        is TaskContent.New -> new { populateFrom(taskContent.wip) }
        is TaskContent.Uncommited -> {
            uncommited(
                wipBuilder = { populateFrom(taskContent.wip) },
                lastCommitedBuilder = { populateFrom(taskContent.lastCommited) },
            )
        }
        is TaskContent.Committed -> committed { populateFrom(taskContent.lastCommited) }
    }
}

private fun TaskData.toBuilder(): TaskDataBuilder {
    val thisData = this
    return TaskDataBuilder().apply {
        owner = thisData.owner.id
        name = thisData.name
        description = thisData.description
        sharedTo = thisData.sharedTo.ids.toMutableList()
        content.populateFrom(thisData.content)
    }
}

/**
 * Returns a copy of this task with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun Task.withData(builder: TaskDataBuilder.() -> Unit): Task {
    return Task(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun VerdictData.toBuilder(): VerdictDataBuilder {
    val thisData = this
    return VerdictDataBuilder().apply {
        score = thisData.score.value
        task = thisData.task.id
        submission = thisData.submission.id
    }
}

/**
 * Returns a copy of this verdict with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun Verdict.withData(builder: VerdictDataBuilder.() -> Unit): Verdict {
    return Verdict(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}
