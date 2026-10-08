package tech.testsys.operation

import tech.testsys.domain.contract.Grader
import tech.testsys.domain.contract.PolygonDiagnostics
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.TaskValidationRequestRepository
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.task.AuthorSolutionRun
import tech.testsys.domain.model.task.AuthorSubmissionFailure
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.TaskValidationExecution
import tech.testsys.domain.model.task.TaskValidationRequest
import tech.testsys.domain.model.task.TaskValidationRequestId
import tech.testsys.domain.model.task.TaskValidationSnapshot
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.util.loadByIdsAsMap

/**
 * Application-invoked processing of saved task validation requests: polygon diagnostics, then author testing.
 * The caller provides exclusive execution per request; technical exceptions propagate with saved progress retained.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalOperationsApi::class)
class TaskValidationOperations(
    private val requests: TaskValidationRequestRepository,
    private val tests: TestRepository,
    private val diagnostics: PolygonDiagnostics,
    private val submissions: SubmissionRepository,
    private val verdicts: VerdictRepository,
    private val grader: Grader,
) {
    /**
     * Processes the caller-selected [requestId], skipping saved polygon results and completing the stage.
     * Error messages do not interrupt other polygons; technical exceptions propagate with saved progress retained.
     *
     * @return the request with completed diagnostics, or `null` when the request is missing or no longer eligible.
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.runDiagnostics")
    fun runDiagnostics(requestId: TaskValidationRequestId): TaskValidationRequest? {
        val request = requests.startDiagnostics(requestId) ?: return null
        val savedIds = requests.findDiagnosticProgress(requestId).map { it.testId }.toSet()
        request.data.snapshot.tests.ids.filterNot { it in savedIds }.forEach { testId ->
            val test = requireNotNull(tests.findById(testId)) {
                "Polygon id=${testId.value} of request id=${requestId.value} does not exist"
            }
            val result = diagnostics.diagnose(test)
            check(result.testId == testId) {
                "Diagnostics returned polygon id=${result.testId.value} for requested polygon id=${testId.value}"
            }
            requests.saveDiagnosticProgress(requestId = requestId, result = result)
        }
        return requests.completeDiagnostics(requestId)
    }

    /**
     * Advances [requestId] as far as possible without waiting for grading: runs unfinished diagnostics,
     * creates and sends author submissions, and completes testing with every failed submission once all are graded.
     * Repeated calls neither recreate nor resend submissions; terminal requests are returned unchanged.
     *
     * @return the current request, or `null` when it does not exist.
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.testTask")
    fun proceed(requestId: TaskValidationRequestId): TaskValidationRequest? {
        val request = requests.findById(requestId) ?: return null
        return when (request.data.execution) {
            TaskValidationExecution.PendingDiagnostics -> {
                val diagnosed = runDiagnostics(requestId) ?: return requests.findById(requestId)
                if (diagnosed.data.execution is TaskValidationExecution.AwaitingSubmissions) submit(requestId) else diagnosed
            }
            is TaskValidationExecution.AwaitingSubmissions -> submit(requestId)
            is TaskValidationExecution.SubmissionsCreated -> completeIfGraded(request)
            is TaskValidationExecution.StoppedByDiagnostics,
            is TaskValidationExecution.Completed,
            is TaskValidationExecution.TechnicalFailure,
            -> request
        }
    }

    /**
     * Sends the not yet graded author submissions of [requestId] again, for recovery after an application restart
     * lost the grader's unfinished work. The grader reuses a submission that is still pending.
     *
     * @return the current request, or `null` when it does not exist.
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.testTask")
    fun resendUnfinishedSubmissions(requestId: TaskValidationRequestId): TaskValidationRequest? {
        val request = requests.findById(requestId) ?: return null
        val state = request.data.execution as? TaskValidationExecution.SubmissionsCreated ?: return request
        loadSubmissions(state).filter { it.data.status !is SubmissionStatus.Graded }.forEach { grader.sendToGrade(it) }
        return request
    }

    private fun submit(requestId: TaskValidationRequestId): TaskValidationRequest {
        val created = requests.createSubmissions(requestId)
        val state = checkNotNull(created.data.execution as? TaskValidationExecution.SubmissionsCreated) {
            "Request id=${requestId.value} has no created submissions: ${created.data.execution}"
        }
        loadSubmissions(state).forEach { grader.sendToGrade(it) }
        return created
    }

    private fun completeIfGraded(request: TaskValidationRequest): TaskValidationRequest {
        val state = checkNotNull(request.data.execution as? TaskValidationExecution.SubmissionsCreated) {
            "Request id=${request.id.value} has no created submissions: ${request.data.execution}"
        }
        val runs = TaskValidationSnapshot.authorRuns(request.data.snapshot)
        val created = loadSubmissions(state)
        check(created.size == runs.size) {
            "Request id=${request.id.value} has ${created.size} submissions for ${runs.size} author runs"
        }
        val grades = created.map { submission -> (submission.data.status as? SubmissionStatus.Graded)?.grade ?: return request }
        val verdictIds = grades.filterIsInstance<GradingResult.Success>().map { grade -> grade.verdict.id }
        val actualScores = verdicts.loadByIdsAsMap(verdictIds).mapValues { (_, verdict) ->
            verdict.data.testVerdicts.sumOf { it.score.value.toLong() }
        }
        val failures = runs.indices.mapNotNull { index -> failureOf(runs[index], created[index], grades[index], actualScores) }
        return requests.completeTesting(requestId = request.id, failures = failures)
    }

    private fun failureOf(
        run: AuthorSolutionRun,
        submission: Submission,
        grade: GradingResult,
        actualScores: Map<VerdictId, Long>,
    ): AuthorSubmissionFailure? {
        return when (grade) {
            is GradingResult.Success -> {
                val actualScore = actualScores.getValue(grade.verdict.id)
                if (actualScore == run.input.expectedScore.value.toLong()) {
                    null
                } else {
                    AuthorSubmissionFailure.ScoreMismatch(submission = submission.id, actualScore = actualScore)
                }
            }
            is GradingResult.GradingError,
            GradingResult.Timeout,
            -> AuthorSubmissionFailure.GradingFailed(submission.id)
        }
    }

    private fun loadSubmissions(state: TaskValidationExecution.WithSubmissions): List<Submission> {
        val ids = state.submissions.ids
        val submissionsById = submissions.loadByIdsAsMap(ids)
        return ids.map(submissionsById::getValue)
    }
}
