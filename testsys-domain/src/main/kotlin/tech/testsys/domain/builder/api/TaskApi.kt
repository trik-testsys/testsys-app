package tech.testsys.domain.builder.api

import tech.testsys.domain.builder.task.CommittedTaskContentBuilder
import tech.testsys.domain.builder.task.ContestBuilder
import tech.testsys.domain.builder.task.ContestDataBuilder
import tech.testsys.domain.builder.task.DeveloperSolutionBuilder
import tech.testsys.domain.builder.task.DeveloperSolutionDataBuilder
import tech.testsys.domain.builder.task.DeveloperSolutionValidationInputBuilder
import tech.testsys.domain.builder.task.DiagnosticLocationBuilder
import tech.testsys.domain.builder.task.DiagnosticPathSegmentBuilder
import tech.testsys.domain.builder.task.DiagnosticReportBuilder
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
import tech.testsys.domain.builder.task.TaskValidationRequestBuilder
import tech.testsys.domain.builder.task.TaskValidationRequestDataBuilder
import tech.testsys.domain.builder.task.TaskValidationSnapshotBuilder
import tech.testsys.domain.builder.task.TaskValidationTechnicalFailureBuilder
import tech.testsys.domain.builder.task.TestBuilder
import tech.testsys.domain.builder.task.TestDataBuilder
import tech.testsys.domain.builder.task.TestDiagnosticResultBuilder
import tech.testsys.domain.builder.task.TestVerdictBuilder
import tech.testsys.domain.builder.task.VerdictBuilder
import tech.testsys.domain.builder.task.VerdictDataBuilder
import tech.testsys.domain.builder.task.WipTaskContentBuilder
import tech.testsys.domain.builder.util.applyVersion
import tech.testsys.domain.builder.util.chooser.TaskContentChooser
import tech.testsys.domain.model.task.CommittedTaskContent
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestData
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionData
import tech.testsys.domain.model.task.DeveloperSolutionValidationInput
import tech.testsys.domain.model.task.DiagnosticLocation
import tech.testsys.domain.model.task.DiagnosticPathSegment
import tech.testsys.domain.model.task.DiagnosticReport
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
import tech.testsys.domain.model.task.TaskValidationExecution
import tech.testsys.domain.model.task.TaskValidationRequest
import tech.testsys.domain.model.task.TaskValidationRequestData
import tech.testsys.domain.model.task.TaskValidationSnapshot
import tech.testsys.domain.model.task.TaskValidationTechnicalFailure
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestData
import tech.testsys.domain.model.task.TestDiagnosticResult
import tech.testsys.domain.model.task.TestVerdict
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
 * Builds a [TestVerdict] with a [TestVerdictBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun testVerdict(builder: TestVerdictBuilder.() -> Unit) = TestVerdictBuilder().apply(builder).build()

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
inline fun taskContentCommitted(builder: CommittedTaskContentBuilder.() -> Unit) =
    TaskContent.Committed(CommittedTaskContentBuilder().apply(builder).build())

/**
 * Builds [TaskContent.Uncommitted].
 *
 * @param wipBuilder the [WipTaskContentBuilder] block of the work-in-progress revision.
 * @param lastCommittedBuilder the [CommittedTaskContentBuilder] block of the last committed revision.
 * @since %CURRENT_VERSION%
 */
inline fun taskContentUncommitted(
    wipBuilder: WipTaskContentBuilder.() -> Unit,
    lastCommittedBuilder: CommittedTaskContentBuilder.() -> Unit,
) = TaskContent.Uncommitted(
    wip = WipTaskContentBuilder().apply(wipBuilder).build(),
    lastCommitted = CommittedTaskContentBuilder().apply(lastCommittedBuilder).build(),
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
            is SubmissionKind.DeveloperSolutionTest -> kind.developerSolutionTest {
                trikStudioVersion = originKind.trikStudioVersion
            }
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
        id = this.id,
        createdAt = this.createdAt,
        data = this.data.toBuilder().apply(builder).build(),
    ).applyVersion(this.version)
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
    return Contest(
        id = this.id,
        createdAt = this.createdAt,
        data = this.data.toBuilder().apply(builder).build(),
    ).applyVersion(this.version)
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
    return DeveloperSolution(
        id = this.id,
        createdAt = this.createdAt,
        data = this.data.toBuilder().apply(builder).build(),
    ).applyVersion(this.version)
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
    return Exercise(
        id = this.id,
        createdAt = this.createdAt,
        data = this.data.toBuilder().apply(builder).build(),
    ).applyVersion(this.version)
}

private fun JudgmentOrderData.toBuilder(): JudgmentOrderDataBuilder {
    val thisData = this
    return JudgmentOrderDataBuilder().apply {
        judge = thisData.judge.id
        submission = thisData.submission.id
        score = thisData.score.value
        reason = thisData.reason
    }
}

/**
 * Returns a copy of this judgment order with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun JudgmentOrder.withData(builder: JudgmentOrderDataBuilder.() -> Unit): JudgmentOrder {
    return JudgmentOrder(
        id = this.id,
        createdAt = this.createdAt,
        data = this.data.toBuilder().apply(builder).build(),
    ).applyVersion(this.version)
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
    return Logs(
        id = this.id,
        createdAt = this.createdAt,
        data = this.data.toBuilder().apply(builder).build(),
    ).applyVersion(this.version)
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
    return Recording(
        id = this.id,
        createdAt = this.createdAt,
        data = this.data.toBuilder().apply(builder).build(),
    ).applyVersion(this.version)
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
    return Solution(
        id = this.id,
        createdAt = this.createdAt,
        data = this.data.toBuilder().apply(builder).build(),
    ).applyVersion(this.version)
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
    return Statement(
        id = this.id,
        createdAt = this.createdAt,
        data = this.data.toBuilder().apply(builder).build(),
    ).applyVersion(this.version)
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
    return Test(
        id = this.id,
        createdAt = this.createdAt,
        data = this.data.toBuilder().apply(builder).build(),
    ).applyVersion(this.version)
}

private fun WipTaskContentBuilder.populateFrom(content: WipTaskContent) {
    tests = content.tests.ids.toMutableList()
    exercises = content.exercises.ids.toMutableList()
    statement = content.statement?.id
    developerSolutions = content.developerSolutions.ids.toMutableList()
    supportedTrikStudioVersions = content.supportedTrikStudioVersions.toMutableList()
}

private fun CommittedTaskContentBuilder.populateFrom(content: CommittedTaskContent) {
    tests = content.tests.ids.toMutableList()
    exercises = content.exercises.ids.toMutableList()
    statement = content.statement.id
    developerSolutions = content.developerSolutions.ids.toMutableList()
    supportedTrikStudioVersions = content.supportedTrikStudioVersions.toMutableList()
}

private fun TaskContentChooser.populateFrom(taskContent: TaskContent) {
    when (taskContent) {
        is TaskContent.New -> new { populateFrom(taskContent.wip) }
        is TaskContent.Uncommitted -> {
            uncommitted(
                wipBuilder = { populateFrom(taskContent.wip) },
                lastCommittedBuilder = { populateFrom(taskContent.lastCommitted) },
            )
        }
        is TaskContent.Committed -> committed { populateFrom(taskContent.lastCommitted) }
    }
}

private fun TaskData.toBuilder(): TaskDataBuilder {
    val thisData = this
    return TaskDataBuilder().apply {
        owner = thisData.owner.id
        name = thisData.name
        description = thisData.description
        sharedTo = thisData.sharedTo.ids.toMutableList()
        uploadedResources = thisData.uploadedResources.toMutableSet()
        content.populateFrom(thisData.content)
    }
}

/**
 * Returns a copy of this task with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun Task.withData(builder: TaskDataBuilder.() -> Unit): Task {
    return Task(
        id = this.id,
        createdAt = this.createdAt,
        data = this.data.toBuilder().apply(builder).build(),
    ).applyVersion(this.version)
}

private fun VerdictData.toBuilder(): VerdictDataBuilder {
    val thisData = this
    return VerdictDataBuilder().apply {
        task = thisData.task.id
        submission = thisData.submission.id
        testVerdicts = thisData.testVerdicts.toMutableList()
    }
}

/**
 * Returns a copy of this verdict with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun Verdict.withData(builder: VerdictDataBuilder.() -> Unit): Verdict {
    return Verdict(
        id = this.id,
        createdAt = this.createdAt,
        data = this.data.toBuilder().apply(builder).build(),
    ).applyVersion(this.version)
}

/**
 * Builds [TaskValidationRequestData] with a [TaskValidationRequestDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun taskValidationRequestData(builder: TaskValidationRequestDataBuilder.() -> Unit): TaskValidationRequestData =
    TaskValidationRequestDataBuilder().apply(builder).build()

/**
 * Builds [TaskValidationRequest] with a [TaskValidationRequestBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun taskValidationRequest(builder: TaskValidationRequestBuilder.() -> Unit): TaskValidationRequest =
    TaskValidationRequestBuilder().apply(builder).build()

/**
 * Builds [TaskValidationSnapshot] with a [TaskValidationSnapshotBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun taskValidationSnapshot(builder: TaskValidationSnapshotBuilder.() -> Unit): TaskValidationSnapshot =
    TaskValidationSnapshotBuilder().apply(builder).build()

/**
 * Builds [DeveloperSolutionValidationInput] with a [DeveloperSolutionValidationInputBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun developerSolutionValidationInput(builder: DeveloperSolutionValidationInputBuilder.() -> Unit): DeveloperSolutionValidationInput =
    DeveloperSolutionValidationInputBuilder().apply(builder).build()

/**
 * Builds [TestDiagnosticResult] with a [TestDiagnosticResultBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun testDiagnosticResult(builder: TestDiagnosticResultBuilder.() -> Unit): TestDiagnosticResult =
    TestDiagnosticResultBuilder().apply(builder).build()

/**
 * Builds [DiagnosticReport] with a [DiagnosticReportBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun diagnosticReport(builder: DiagnosticReportBuilder.() -> Unit): DiagnosticReport =
    DiagnosticReportBuilder().apply(builder).build()

/**
 * Builds [DiagnosticLocation] with a [DiagnosticLocationBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun diagnosticLocation(builder: DiagnosticLocationBuilder.() -> Unit): DiagnosticLocation =
    DiagnosticLocationBuilder().apply(builder).build()

/**
 * Builds [DiagnosticPathSegment] with a [DiagnosticPathSegmentBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun diagnosticPathSegment(builder: DiagnosticPathSegmentBuilder.() -> Unit): DiagnosticPathSegment =
    DiagnosticPathSegmentBuilder().apply(builder).build()

/**
 * Builds [TaskValidationTechnicalFailure] with a [TaskValidationTechnicalFailureBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun taskValidationTechnicalFailure(builder: TaskValidationTechnicalFailureBuilder.() -> Unit): TaskValidationTechnicalFailure =
    TaskValidationTechnicalFailureBuilder().apply(builder).build()

/**
 * Copies a [TaskValidationRequest] with [builder] applied to its data, retaining its identifier and version.
 *
 * @since %CURRENT_VERSION%
 */
fun TaskValidationRequest.withData(builder: TaskValidationRequestDataBuilder.() -> Unit): TaskValidationRequest {
    return taskValidationRequest {
        id = this@withData.id.value
        createdAt = this@withData.createdAt
        version = this@withData.version
        data = this@withData.data.toBuilder().apply(builder).build()
    }
}

private fun TaskValidationRequestData.toBuilder() = TaskValidationRequestDataBuilder().also { builder ->
    builder.task = task.id
    builder.requestedBy = requestedBy.id
    builder.snapshot = snapshot
    when (val state = execution) {
        TaskValidationExecution.PendingDiagnostics -> builder.execution.pendingDiagnostics()
        is TaskValidationExecution.AwaitingSubmissions -> builder.execution.awaitingSubmissions {
            diagnostics = state.diagnostics.toMutableList()
        }
        is TaskValidationExecution.StoppedByDiagnostics -> builder.execution.stoppedByDiagnostics {
            diagnostics = state.diagnostics.toMutableList()
            completedAt = state.completedAt
        }
        is TaskValidationExecution.SubmissionsCreated -> builder.execution.submissionsCreated {
            diagnostics = state.diagnostics.toMutableList()
            submissions = state.submissions.ids.toMutableList()
        }
        is TaskValidationExecution.Completed -> builder.execution.completed {
            diagnostics = state.diagnostics.toMutableList()
            submissions = state.submissions.ids.toMutableList()
            failures = state.failures.toMutableList()
            completedAt = state.completedAt
        }
        is TaskValidationExecution.TechnicalFailure.IncompleteDiagnostics -> builder.execution.incompleteDiagnosticsFailure {
            failure = state.failure
        }
        is TaskValidationExecution.TechnicalFailure.CompletedDiagnostics -> builder.execution.completedDiagnosticsFailure {
            diagnostics = state.diagnostics.toMutableList()
            failure = state.failure
        }
        is TaskValidationExecution.TechnicalFailure.CreatedSubmissions -> builder.execution.createdSubmissionsFailure {
            diagnostics = state.diagnostics.toMutableList()
            submissions = state.submissions.ids.toMutableList()
            failure = state.failure
        }
    }
}
