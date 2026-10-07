package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant

/**
 * Identifier of a [TaskValidationRequest].
 *
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class TaskValidationRequestId(override val value: Long) : DomainId

/**
 * Execution state and stage data of a [TaskValidationRequest].
 *
 * @since %CURRENT_VERSION%
 */
sealed interface TaskValidationExecution {
    /**
     * A [TaskValidationRequest] whose diagnostics are unfinished, with separately stored polygon progress.
     *
     * @since %CURRENT_VERSION%
     */
    data object PendingDiagnostics : TaskValidationExecution

    /**
     * Completed diagnostics of a [TaskValidationRequest].
     *
     * @property diagnostics the result of every polygon in the snapshot.
     * @since %CURRENT_VERSION%
     */
    sealed interface WithDiagnostics : TaskValidationExecution {
        val diagnostics: List<TestDiagnosticResult>
    }

    /**
     * Created submission references of a [TaskValidationRequest].
     *
     * @property submissions the submitted author solution tests.
     * @since %CURRENT_VERSION%
     */
    sealed interface WithSubmissions : WithDiagnostics {
        val submissions: LazyEntityList<SubmissionId, Submission>
    }

    /**
     * A [TaskValidationRequest] whose diagnostics permit submission of author solutions.
     *
     * @since %CURRENT_VERSION%
     */
    data class AwaitingSubmissions(override val diagnostics: List<TestDiagnosticResult>) : WithDiagnostics

    /**
     * A [TaskValidationRequest] stopped by diagnostic errors.
     *
     * @property completedAt the terminal stop moment.
     * @since %CURRENT_VERSION%
     */
    data class StoppedByDiagnostics(
        override val diagnostics: List<TestDiagnosticResult>,
        val completedAt: Instant,
    ) : WithDiagnostics

    /**
     * A [TaskValidationRequest] with submitted author solution tests awaiting further processing.
     *
     * @since %CURRENT_VERSION%
     */
    data class SubmissionsCreated(
        override val diagnostics: List<TestDiagnosticResult>,
        override val submissions: LazyEntityList<SubmissionId, Submission>,
    ) : WithSubmissions

    /**
     * Finished author testing of a [TaskValidationRequest]; testing succeeded if [failures] is empty.
     *
     * @property failures every failed author submission in submission order, fixed when all submissions finished.
     * @property completedAt the terminal completion moment.
     * @since %CURRENT_VERSION%
     */
    data class Completed(
        override val diagnostics: List<TestDiagnosticResult>,
        override val submissions: LazyEntityList<SubmissionId, Submission>,
        val failures: List<AuthorSubmissionFailure>,
        val completedAt: Instant,
    ) : WithSubmissions

    /**
     * A technical stop of a [TaskValidationRequest], retaining its completed stages.
     *
     * @property failure the recorded failure.
     * @property completedAt the failure time and terminal stop moment.
     * @since %CURRENT_VERSION%
     */
    sealed interface TechnicalFailure : TaskValidationExecution {
        val failure: TaskValidationTechnicalFailure
        val completedAt: Instant get() = failure.occurredAt

        /**
         * A [TaskValidationRequest] stopped before diagnostics completed, with separately stored progress retained.
         *
         * @since %CURRENT_VERSION%
         */
        data class IncompleteDiagnostics(override val failure: TaskValidationTechnicalFailure) : TechnicalFailure

        /**
         * A [TaskValidationRequest] stopped after diagnostics completed.
         *
         * @since %CURRENT_VERSION%
         */
        data class CompletedDiagnostics(
            override val diagnostics: List<TestDiagnosticResult>,
            override val failure: TaskValidationTechnicalFailure,
        ) : TechnicalFailure, WithDiagnostics

        /**
         * A [TaskValidationRequest] stopped after author solution submissions were created.
         *
         * @since %CURRENT_VERSION%
         */
        data class CreatedSubmissions(
            override val diagnostics: List<TestDiagnosticResult>,
            override val submissions: LazyEntityList<SubmissionId, Submission>,
            override val failure: TaskValidationTechnicalFailure,
        ) : TechnicalFailure, WithSubmissions
    }
}

/**
 * Reason why an author submission of a [TaskValidationRequest] failed testing.
 *
 * @property submission the failed submission.
 * @since %CURRENT_VERSION%
 */
sealed interface AuthorSubmissionFailure {
    val submission: SubmissionId

    /**
     * The submission scored a total other than the expected score of its author solution.
     *
     * @property actualScore the total of all polygon scores, calculated without integer overflow.
     * @since %CURRENT_VERSION%
     */
    data class ScoreMismatch(override val submission: SubmissionId, val actualScore: Long) : AuthorSubmissionFailure

    /**
     * The submission finished with a grading error or timeout kept in the submission.
     *
     * @since %CURRENT_VERSION%
     */
    data class GradingFailed(override val submission: SubmissionId) : AuthorSubmissionFailure
}

/**
 * Technical stop recorded by the external caller of a [TaskValidationRequest].
 *
 * @property description the technical failure details.
 * @property occurredAt the moment the failure was recorded.
 * @since %CURRENT_VERSION%
 */
data class TaskValidationTechnicalFailure(
    val description: String,
    val occurredAt: Instant,
)

/**
 * Data of a [TaskValidationRequest].
 *
 * @property task the task; fixed on creation and ignored on update.
 * @property requestedBy the initiator; fixed on creation and ignored on update.
 * @property snapshot the validation inputs; fixed on creation and ignored on update.
 * @property execution the execution state and its stage data.
 * @property isActive whether execution may continue.
 * @since %CURRENT_VERSION%
 */
data class TaskValidationRequestData(
    val task: LazyEntity<TaskId, Task>,
    val requestedBy: LazyEntity<MultipleRoleUserId, MultipleRoleUser>,
    val snapshot: TaskValidationSnapshot,
    val execution: TaskValidationExecution,
) {
    val isActive: Boolean = when (execution) {
        TaskValidationExecution.PendingDiagnostics,
        is TaskValidationExecution.AwaitingSubmissions,
        is TaskValidationExecution.SubmissionsCreated,
        -> true
        is TaskValidationExecution.StoppedByDiagnostics,
        is TaskValidationExecution.Completed,
        is TaskValidationExecution.TechnicalFailure,
        -> false
    }
}

/**
 * A saved validation of immutable task inputs, independent of later edits to the task.
 *
 * @property data the data of the request.
 * @since %CURRENT_VERSION%
 */
class TaskValidationRequest(
    id: TaskValidationRequestId,
    createdAt: Instant,
    val data: TaskValidationRequestData,
) : DomainEntity<TaskValidationRequestId>(id, createdAt)
