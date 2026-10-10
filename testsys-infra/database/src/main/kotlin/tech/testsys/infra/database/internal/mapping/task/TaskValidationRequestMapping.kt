package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.taskValidationRequest
import tech.testsys.domain.builder.api.taskValidationTechnicalFailure
import tech.testsys.domain.builder.data
import tech.testsys.domain.builder.task.TaskValidationRequestDataBuilder
import tech.testsys.domain.model.task.*
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.AuthorSubmissionFailureJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.task.DeveloperSolutionToTaskValidationRequestId
import tech.testsys.infra.database.internal.jpa.entity.task.DeveloperSolutionToTaskValidationRequestJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionToTaskValidationRequestId
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionToTaskValidationRequestJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TaskValidationExecutionJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.task.TaskValidationRequestJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TestToTaskValidationRequestJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TrikStudioVersionToTaskValidationRequestJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields
import tech.testsys.infra.database.internal.utils.requireVersion

/**
 * Mapping between [TaskValidationRequest] and [TaskValidationRequestJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object TaskValidationRequestMapping : EntityMapping<TaskValidationRequest, TaskValidationRequestJpaEntity> {
    /**
     * Assembles a request from [jpaEntity] and its resolved [data].
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(jpaEntity: TaskValidationRequestJpaEntity, data: TaskValidationRequestData): TaskValidationRequest =
        taskValidationRequest {
            populateFields(jpaEntity)
            this.data = data
        }

    /**
     * Creates a new request row from [data].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(data: TaskValidationRequestData): TaskValidationRequestJpaEntity = TaskValidationRequestJpaEntity(
        taskId = data.task.id.value,
        requestedById = data.requestedBy.id.value,
        execution = encodeExecution(data.execution),
        areDiagnosticsComplete = data.execution is TaskValidationExecution.WithDiagnostics,
        areSubmissionsCreated = data.execution is TaskValidationExecution.WithSubmissions,
        failureDescription = (data.execution as? TaskValidationExecution.TechnicalFailure)?.failure?.description,
        failureOccurredAt = (data.execution as? TaskValidationExecution.TechnicalFailure)?.failure?.occurredAt,
        completedAt = completionTime(data.execution),
    )

    /**
     * Creates a replacement request row retaining fixed fields, creation time and [entity]'s version.
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(entity: TaskValidationRequest, current: TaskValidationRequestJpaEntity): TaskValidationRequestJpaEntity =
        TaskValidationRequestJpaEntity(
            taskId = current.taskId,
            requestedById = current.requestedById,
            execution = encodeExecution(entity.data.execution),
            areDiagnosticsComplete = entity.data.execution is TaskValidationExecution.WithDiagnostics,
            areSubmissionsCreated = entity.data.execution is TaskValidationExecution.WithSubmissions,
            failureDescription = (entity.data.execution as? TaskValidationExecution.TechnicalFailure)?.failure?.description,
            failureOccurredAt = (entity.data.execution as? TaskValidationExecution.TechnicalFailure)?.failure?.occurredAt,
            completedAt = completionTime(entity.data.execution),
            id = entity.id.value,
        ).also {
            it.createdAt = current.createdAt
            it.version = entity.requireVersion()
        }

    /**
     * Restores the state from [row], completed [diagnostics], stored [submissionIds] and their [failures] into [builder].
     * Inconsistent storage flags or missing required payload cause an exception.
     *
     * @since %CURRENT_VERSION%
     */
    fun decodeExecution(
        row: TaskValidationRequestJpaEntity,
        diagnostics: List<TestDiagnosticResult>,
        submissionIds: List<SubmissionId>,
        failures: List<AuthorSubmissionFailure>,
        builder: TaskValidationRequestDataBuilder,
    ) {
        validateStoredState(row, diagnostics, submissionIds)
        check(failures.isEmpty() || row.execution == TaskValidationExecutionJpaEnum.COMPLETED) {
            "Request id=${row.id} has author submission failures in ${row.execution}"
        }
        when (row.execution) {
            TaskValidationExecutionJpaEnum.PENDING_DIAGNOSTICS -> builder.execution.pendingDiagnostics()
            TaskValidationExecutionJpaEnum.AWAITING_SUBMISSIONS -> builder.execution.awaitingSubmissions {
                this.diagnostics = diagnostics.toMutableList()
            }
            TaskValidationExecutionJpaEnum.STOPPED_BY_DIAGNOSTICS -> builder.execution.stoppedByDiagnostics {
                this.diagnostics = diagnostics.toMutableList()
                completedAt = checkNotNull(row.completedAt) { "Request id=${row.id} has no completion time" }
            }
            TaskValidationExecutionJpaEnum.SUBMISSIONS_CREATED -> builder.execution.submissionsCreated {
                this.diagnostics = diagnostics.toMutableList()
                submissions = submissionIds.toMutableList()
            }
            TaskValidationExecutionJpaEnum.COMPLETED -> builder.execution.completed {
                this.diagnostics = diagnostics.toMutableList()
                submissions = submissionIds.toMutableList()
                this.failures = failures.toMutableList()
                completedAt = checkNotNull(row.completedAt) { "Request id=${row.id} has no completion time" }
            }
            TaskValidationExecutionJpaEnum.TECHNICAL_FAILURE -> {
                val failure = taskValidationTechnicalFailure {
                    description = checkNotNull(row.failureDescription) { "Request id=${row.id} has no failure description" }
                    occurredAt = checkNotNull(row.failureOccurredAt) { "Request id=${row.id} has no failure time" }
                }
                when {
                    row.areSubmissionsCreated -> builder.execution.createdSubmissionsFailure {
                        this.diagnostics = diagnostics.toMutableList()
                        submissions = submissionIds.toMutableList()
                        this.failure = failure
                    }
                    row.areDiagnosticsComplete -> builder.execution.completedDiagnosticsFailure {
                        this.diagnostics = diagnostics.toMutableList()
                        this.failure = failure
                    }
                    else -> builder.execution.incompleteDiagnosticsFailure { this.failure = failure }
                }
            }
        }
    }

    /**
     * Creates the pinned polygon associations of [requestId] from [testIds].
     *
     * @since %CURRENT_VERSION%
     */
    fun toTestAssociations(requestId: Long, testIds: List<TestId>) = testIds.distinct().map {
        TestToTaskValidationRequestJpaEntity(testId = it.value, requestId = requestId)
    }

    /**
     * Creates the pinned author inputs of [requestId] from [inputs], one row per distinct developer solution: a repeated
     * one keeps the position of its first input and the values of its last.
     *
     * @since %CURRENT_VERSION%
     */
    fun toDeveloperSolutionAssociations(
        requestId: Long,
        inputs: List<DeveloperSolutionValidationInput>,
    ): List<DeveloperSolutionToTaskValidationRequestJpaEntity> = inputs.associateBy { it.developerSolution.id }.values.map { input ->
        DeveloperSolutionToTaskValidationRequestJpaEntity(
            id = DeveloperSolutionToTaskValidationRequestId(developerSolutionId = input.developerSolution.id.value, requestId = requestId),
            solutionId = input.solution.id.value,
            expectedScore = input.expectedScore.value,
        )
    }

    /**
     * Creates the pinned version associations of [requestId] from resolved [versionIds].
     *
     * @since %CURRENT_VERSION%
     */
    fun toTrikStudioVersionAssociations(requestId: Long, versionIds: List<Long>) = versionIds.distinct().map {
        TrikStudioVersionToTaskValidationRequestJpaEntity(trikStudioVersionId = it, requestId = requestId)
    }

    /**
     * Creates ordered submission references of [requestId] from [submissionIds], each with its entry of [failures].
     *
     * @throws IllegalArgumentException if [failures] do not name distinct linked submissions in [submissionIds] order.
     * @since %CURRENT_VERSION%
     */
    fun toSubmissionAssociations(
        requestId: Long,
        submissionIds: List<SubmissionId>,
        failures: List<AuthorSubmissionFailure>,
    ): List<SubmissionToTaskValidationRequestJpaEntity> {
        val failureBySubmission = failures.associateBy { it.submission }
        require(failures.map { it.submission } == submissionIds.filter { it in failureBySubmission }) {
            "Request id=$requestId failures must name distinct linked submissions in submission order"
        }
        return submissionIds.distinct().mapIndexed { index, id ->
            val failure = failureBySubmission[id]
            SubmissionToTaskValidationRequestJpaEntity(
                id = SubmissionToTaskValidationRequestId(submissionId = id.value, requestId = requestId),
                position = index,
                failure = failure?.let(::encodeFailure),
                actualScore = (failure as? AuthorSubmissionFailure.ScoreMismatch)?.actualScore,
            )
        }
    }

    /**
     * Restores the failure stored in [link], or `null` for a submission without one.
     * A score is required for a mismatch and forbidden otherwise.
     *
     * @since %CURRENT_VERSION%
     */
    fun toFailure(link: SubmissionToTaskValidationRequestJpaEntity): AuthorSubmissionFailure? {
        val submission = SubmissionId(link.id.submissionId)
        check((link.actualScore != null) == (link.failure == AuthorSubmissionFailureJpaEnum.SCORE_MISMATCH)) {
            "Submission id=${submission.value} of request id=${link.id.requestId} has an inconsistent failure score"
        }
        return when (link.failure) {
            null -> null
            AuthorSubmissionFailureJpaEnum.SCORE_MISMATCH -> AuthorSubmissionFailure.ScoreMismatch(
                submission = submission,
                actualScore = checkNotNull(link.actualScore),
            )
            AuthorSubmissionFailureJpaEnum.GRADING_FAILED -> AuthorSubmissionFailure.GradingFailed(submission)
        }
    }

    private fun encodeFailure(failure: AuthorSubmissionFailure): AuthorSubmissionFailureJpaEnum = when (failure) {
        is AuthorSubmissionFailure.ScoreMismatch -> AuthorSubmissionFailureJpaEnum.SCORE_MISMATCH
        is AuthorSubmissionFailure.GradingFailed -> AuthorSubmissionFailureJpaEnum.GRADING_FAILED
    }

    private fun completionTime(execution: TaskValidationExecution): java.time.Instant? = when (execution) {
        TaskValidationExecution.PendingDiagnostics,
        is TaskValidationExecution.AwaitingSubmissions,
        is TaskValidationExecution.SubmissionsCreated,
        -> null
        is TaskValidationExecution.Completed -> execution.completedAt
        is TaskValidationExecution.StoppedByDiagnostics -> execution.completedAt
        is TaskValidationExecution.TechnicalFailure -> execution.completedAt
    }

    private fun encodeExecution(execution: TaskValidationExecution): TaskValidationExecutionJpaEnum = when (execution) {
        TaskValidationExecution.PendingDiagnostics -> TaskValidationExecutionJpaEnum.PENDING_DIAGNOSTICS
        is TaskValidationExecution.AwaitingSubmissions -> TaskValidationExecutionJpaEnum.AWAITING_SUBMISSIONS
        is TaskValidationExecution.StoppedByDiagnostics -> TaskValidationExecutionJpaEnum.STOPPED_BY_DIAGNOSTICS
        is TaskValidationExecution.Completed -> TaskValidationExecutionJpaEnum.COMPLETED
        is TaskValidationExecution.SubmissionsCreated -> TaskValidationExecutionJpaEnum.SUBMISSIONS_CREATED
        is TaskValidationExecution.TechnicalFailure -> TaskValidationExecutionJpaEnum.TECHNICAL_FAILURE
    }

    private fun validateStoredState(
        row: TaskValidationRequestJpaEntity,
        diagnostics: List<TestDiagnosticResult>,
        submissionIds: List<SubmissionId>,
    ) {
        val isFailure = row.execution == TaskValidationExecutionJpaEnum.TECHNICAL_FAILURE
        val isStopped = row.execution == TaskValidationExecutionJpaEnum.STOPPED_BY_DIAGNOSTICS
        val hasError = diagnostics.any { result -> result.reports.any { it.severity == DiagnosticSeverity.Error } }
        val areExpectedDiagnosticsComplete = when (row.execution) {
            TaskValidationExecutionJpaEnum.PENDING_DIAGNOSTICS -> false
            TaskValidationExecutionJpaEnum.AWAITING_SUBMISSIONS,
            TaskValidationExecutionJpaEnum.STOPPED_BY_DIAGNOSTICS,
            TaskValidationExecutionJpaEnum.SUBMISSIONS_CREATED,
            TaskValidationExecutionJpaEnum.COMPLETED,
            -> true
            TaskValidationExecutionJpaEnum.TECHNICAL_FAILURE -> row.areDiagnosticsComplete
        }
        val areExpectedSubmissionsCreated = when (row.execution) {
            TaskValidationExecutionJpaEnum.SUBMISSIONS_CREATED,
            TaskValidationExecutionJpaEnum.COMPLETED,
            -> true
            TaskValidationExecutionJpaEnum.TECHNICAL_FAILURE -> row.areSubmissionsCreated
            TaskValidationExecutionJpaEnum.PENDING_DIAGNOSTICS,
            TaskValidationExecutionJpaEnum.AWAITING_SUBMISSIONS,
            TaskValidationExecutionJpaEnum.STOPPED_BY_DIAGNOSTICS,
            -> false
        }
        check(
            row.areDiagnosticsComplete == areExpectedDiagnosticsComplete &&
                row.areSubmissionsCreated == areExpectedSubmissionsCreated &&
                (!row.areSubmissionsCreated || row.areDiagnosticsComplete) &&
                (row.areSubmissionsCreated || submissionIds.isEmpty()),
        ) { "Request id=${row.id} has inconsistent stage flags for ${row.execution}" }
        check(
            (row.failureDescription != null) == isFailure &&
                (row.failureOccurredAt != null) == isFailure &&
                (row.completedAt != null) == (isFailure || isStopped || row.execution == TaskValidationExecutionJpaEnum.COMPLETED) &&
                (!isFailure || row.completedAt == row.failureOccurredAt),
        ) { "Request id=${row.id} has inconsistent failure or completion data for ${row.execution}" }
        check(
            (!isStopped || hasError) &&
                (isStopped || isFailure || !hasError),
        ) { "Request id=${row.id} has diagnostic errors inconsistent with ${row.execution}" }
    }
}
