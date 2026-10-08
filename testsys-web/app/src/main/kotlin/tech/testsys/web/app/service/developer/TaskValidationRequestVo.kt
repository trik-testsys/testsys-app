package tech.testsys.web.app.service.developer

import tech.testsys.domain.model.task.AuthorSubmissionFailure
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.DeveloperSolutionValidationInput
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TaskValidationExecution
import tech.testsys.domain.model.task.TaskValidationRequest
import tech.testsys.domain.model.task.TaskValidationRequestId
import tech.testsys.domain.model.task.TaskValidationSnapshot
import tech.testsys.domain.model.task.TaskValidationTechnicalFailure
import tech.testsys.domain.model.task.TestDiagnosticResult
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant

/**
 * Task validation request data for pages, with links replaced by identifiers.
 *
 * @property id the identifier of the request.
 * @property createdAt the moment the request was created.
 * @property task the identifier of the validated task.
 * @property requestedBy the identifier of the developer who requested validation.
 * @property snapshot the validated content of the task.
 * @property execution the progress of the request.
 * @property isActive whether the request is still being processed.
 * @since %CURRENT_VERSION%
 */
data class TaskValidationRequestVo(
    val id: TaskValidationRequestId,
    val createdAt: Instant,
    val task: TaskId,
    val requestedBy: MultipleRoleUserId,
    val snapshot: TaskValidationSnapshotVo,
    val execution: TaskValidationExecutionVo,
    val isActive: Boolean,
)

/**
 * Validated content of a [TaskValidationRequestVo].
 *
 * @property tests the identifiers of the tests.
 * @property developerSolutions the developer solutions with their expected scores.
 * @property supportedTrikStudioVersions the TRIK Studio versions to run the solutions on.
 * @since %CURRENT_VERSION%
 */
data class TaskValidationSnapshotVo(
    val tests: List<TestId>,
    val developerSolutions: List<DeveloperSolutionInputVo>,
    val supportedTrikStudioVersions: List<TrikStudioVersion>,
)

/**
 * Developer solution of a [TaskValidationSnapshotVo].
 *
 * @property developerSolution the identifier of the developer solution.
 * @property solution the identifier of its solution.
 * @property expectedScore the score the solution is expected to get.
 * @since %CURRENT_VERSION%
 */
data class DeveloperSolutionInputVo(
    val developerSolution: DeveloperSolutionId,
    val solution: SolutionId,
    val expectedScore: Score,
)

/**
 * Progress of a [TaskValidationRequestVo]; mirrors the domain execution states with submissions as identifiers.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface TaskValidationExecutionVo {
    /**
     * Diagnostics have not finished yet.
     *
     * @since %CURRENT_VERSION%
     */
    data object PendingDiagnostics : TaskValidationExecutionVo

    /**
     * Diagnostics passed; submissions are not created yet.
     *
     * @property diagnostics the diagnostic results of the tests.
     * @since %CURRENT_VERSION%
     */
    data class AwaitingSubmissions(val diagnostics: List<TestDiagnosticResult>) : TaskValidationExecutionVo

    /**
     * Diagnostics found an error, so no submissions were created.
     *
     * @property diagnostics the diagnostic results of the tests.
     * @property completedAt the moment the request was stopped.
     * @since %CURRENT_VERSION%
     */
    data class StoppedByDiagnostics(val diagnostics: List<TestDiagnosticResult>, val completedAt: Instant) :
        TaskValidationExecutionVo

    /**
     * Submissions are created and being graded.
     *
     * @property diagnostics the diagnostic results of the tests.
     * @property submissions the identifiers of the author submissions.
     * @since %CURRENT_VERSION%
     */
    data class SubmissionsCreated(val diagnostics: List<TestDiagnosticResult>, val submissions: List<SubmissionId>) :
        TaskValidationExecutionVo

    /**
     * All submissions are graded; an empty [failures] list means success.
     *
     * @property diagnostics the diagnostic results of the tests.
     * @property submissions the identifiers of the author submissions.
     * @property failures the failed author submissions.
     * @property completedAt the moment the request was completed.
     * @since %CURRENT_VERSION%
     */
    data class Completed(
        val diagnostics: List<TestDiagnosticResult>,
        val submissions: List<SubmissionId>,
        val failures: List<AuthorSubmissionFailure>,
        val completedAt: Instant,
    ) : TaskValidationExecutionVo

    /**
     * A technical failure stopped the request before diagnostics finished.
     *
     * @property failure the technical failure.
     * @since %CURRENT_VERSION%
     */
    data class IncompleteDiagnostics(val failure: TaskValidationTechnicalFailure) : TaskValidationExecutionVo

    /**
     * A technical failure stopped the request after diagnostics.
     *
     * @property diagnostics the diagnostic results of the tests.
     * @property failure the technical failure.
     * @since %CURRENT_VERSION%
     */
    data class CompletedDiagnostics(val diagnostics: List<TestDiagnosticResult>, val failure: TaskValidationTechnicalFailure) :
        TaskValidationExecutionVo

    /**
     * A technical failure stopped the request after submissions were created.
     *
     * @property diagnostics the diagnostic results of the tests.
     * @property submissions the identifiers of the author submissions.
     * @property failure the technical failure.
     * @since %CURRENT_VERSION%
     */
    data class CreatedSubmissions(
        val diagnostics: List<TestDiagnosticResult>,
        val submissions: List<SubmissionId>,
        val failure: TaskValidationTechnicalFailure,
    ) : TaskValidationExecutionVo
}

internal fun TaskValidationRequest.toVo(): TaskValidationRequestVo = TaskValidationRequestVo(
    id = id,
    createdAt = createdAt,
    task = data.task.id,
    requestedBy = data.requestedBy.id,
    snapshot = data.snapshot.toVo(),
    execution = data.execution.toVo(),
    isActive = data.isActive,
)

private fun TaskValidationSnapshot.toVo() = TaskValidationSnapshotVo(
    tests = tests.ids,
    developerSolutions = developerSolutions.map { input -> input.toVo() },
    supportedTrikStudioVersions = supportedTrikStudioVersions,
)

private fun DeveloperSolutionValidationInput.toVo() = DeveloperSolutionInputVo(
    developerSolution = developerSolution.id,
    solution = solution.id,
    expectedScore = expectedScore,
)

private fun TaskValidationExecution.toVo(): TaskValidationExecutionVo = when (this) {
    TaskValidationExecution.PendingDiagnostics -> TaskValidationExecutionVo.PendingDiagnostics
    is TaskValidationExecution.AwaitingSubmissions -> TaskValidationExecutionVo.AwaitingSubmissions(diagnostics)
    is TaskValidationExecution.StoppedByDiagnostics -> TaskValidationExecutionVo.StoppedByDiagnostics(diagnostics, completedAt)
    is TaskValidationExecution.SubmissionsCreated -> TaskValidationExecutionVo.SubmissionsCreated(diagnostics, submissions.ids)
    is TaskValidationExecution.Completed ->
        TaskValidationExecutionVo.Completed(diagnostics, submissions.ids, failures, completedAt)
    is TaskValidationExecution.TechnicalFailure.IncompleteDiagnostics -> TaskValidationExecutionVo.IncompleteDiagnostics(failure)
    is TaskValidationExecution.TechnicalFailure.CompletedDiagnostics ->
        TaskValidationExecutionVo.CompletedDiagnostics(diagnostics, failure)
    is TaskValidationExecution.TechnicalFailure.CreatedSubmissions ->
        TaskValidationExecutionVo.CreatedSubmissions(diagnostics, submissions.ids, failure)
}
