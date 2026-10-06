package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.builder.api.*
import tech.testsys.domain.contract.persistence.repository.DeveloperSolutionRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.TaskValidationRequestRepository
import tech.testsys.domain.model.task.*
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.*
import tech.testsys.infra.database.internal.jpa.repository.task.*
import tech.testsys.infra.database.internal.mapping.task.TaskValidationRequestMapping
import tech.testsys.infra.database.internal.mapping.task.TestDiagnosticResultMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError
import tech.testsys.infra.database.internal.utils.findIdByTagOrError
import tech.testsys.infra.database.internal.utils.requireId
import tech.testsys.infra.database.internal.utils.syncJoinTable
import java.time.Instant

/**
 * Persistence adapter of [TaskValidationRequest] entities backed by [TaskValidationRequestJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class TaskValidationRequestPersistenceAdapter(
    private val requests: TaskValidationRequestJpaEntityRepository,
    private val tasks: TaskJpaEntityRepository,
    private val taskRepository: TaskRepository,
    private val developerSolutionRepository: DeveloperSolutionRepository,
    private val tests: TestToTaskValidationRequestJpaEntityRepository,
    private val solutions: DeveloperSolutionToTaskValidationRequestJpaEntityRepository,
    private val versions: TrikStudioVersionToTaskValidationRequestJpaEntityRepository,
    private val submissions: SubmissionToTaskValidationRequestJpaEntityRepository,
    private val studioVersions: TrikStudioVersionJpaEntityRepository,
    private val results: TestDiagnosticResultJpaEntityRepository,
    private val reports: DiagnosticReportJpaEntityRepository,
) : AbstractPersistenceAdapter<TaskValidationRequestData, TaskValidationRequestId, TaskValidationRequest, TaskValidationRequestJpaEntity>(
    requests,
),
    TaskValidationRequestRepository {

    @Transactional
    override fun findOrCreateActive(taskId: TaskId, requestedBy: MultipleRoleUserId): TaskValidationRequest {
        val taskRow = requireNotNull(tasks.findLockedById(taskId.value)) { "Task id=${taskId.value} does not exist" }
        require(taskRow.ownerId == requestedBy.value) { "Task id=${taskId.value} is not owned by initiator=${requestedBy.value}" }
        check(taskRow.status != TaskStatusJpaEnum.COMMITTED) {
            "Task id=${taskId.value} has no working revision for diagnostics"
        }
        val task = requireNotNull(taskRepository.findById(taskId)) { "Task id=${taskId.value} does not exist" }
        val snapshot = snapshotOf(task)
        val existing = findHistory(taskId).firstOrNull { request ->
            request.data.isActive && sameSnapshot(request.data.snapshot, snapshot)
        }
        return existing ?: save(
            taskValidationRequestData {
                this.task = taskId
                this.requestedBy = requestedBy
                this.snapshot = snapshot
                execution.pendingDiagnostics()
            },
        )
    }

    @Transactional(readOnly = true)
    override fun findHistory(taskId: TaskId): List<TaskValidationRequest> =
        requests.findAllByTaskIdOrderByCreatedAtAscIdAsc(taskId.value).map { assemble(it) }

    @Transactional
    override fun startDiagnostics(requestId: TaskValidationRequestId): TaskValidationRequest? {
        val row = requests.findLockedById(requestId.value) ?: return null
        return when (row.execution) {
            TaskValidationExecutionJpaEnum.PENDING_DIAGNOSTICS -> update(
                assemble(row).withData {
                    execution.diagnosticsInProgress()
                },
            )
            TaskValidationExecutionJpaEnum.DIAGNOSTICS_IN_PROGRESS -> assemble(row)
            TaskValidationExecutionJpaEnum.AWAITING_SUBMISSIONS,
            TaskValidationExecutionJpaEnum.SUBMISSIONS_CREATED,
            TaskValidationExecutionJpaEnum.STOPPED_BY_DIAGNOSTICS,
            TaskValidationExecutionJpaEnum.TECHNICAL_FAILURE,
            -> null
        }
    }

    @Transactional(readOnly = true)
    override fun findDiagnosticProgress(requestId: TaskValidationRequestId): List<TestDiagnosticResult> =
        results.findAllByIdRequestId(requestId.value).sortedBy { it.id.testId }.map { row ->
            resultOf(requestId = row.id.requestId, testId = row.id.testId)
        }

    @Transactional
    override fun saveDiagnosticProgress(requestId: TaskValidationRequestId, result: TestDiagnosticResult): TestDiagnosticResult {
        val row = locked(requestId)
        require(tests.findAllByIdRequestId(requestId.value).any { it.id.testId == result.testId.value }) {
            "Polygon id=${result.testId.value} is absent from request id=${requestId.value}"
        }
        val key = TestDiagnosticResultId(requestId = requestId.value, testId = result.testId.value)
        if (results.existsById(key)) return resultOf(requestId = requestId.value, testId = result.testId.value)
        check(row.execution == TaskValidationExecutionJpaEnum.DIAGNOSTICS_IN_PROGRESS) {
            "Request id=${requestId.value} is not diagnosing: ${row.execution}"
        }
        persistResult(requestId.value, result)
        return resultOf(requestId = requestId.value, testId = result.testId.value)
    }

    @Transactional
    override fun completeDiagnostics(requestId: TaskValidationRequestId): TaskValidationRequest {
        val row = locked(requestId)
        if (row.areDiagnosticsComplete) return assemble(row)
        check(row.execution == TaskValidationExecutionJpaEnum.DIAGNOSTICS_IN_PROGRESS) {
            "Request id=${requestId.value} cannot complete diagnostics from ${row.execution}"
        }
        val request = assemble(row)
        val progress = findDiagnosticProgress(requestId)
        check(progress.map { it.testId }.toSet() == request.data.snapshot.tests.ids.toSet()) {
            "Request id=${requestId.value} has unsaved polygon results"
        }
        val hasError = progress.any { result -> result.reports.any { it.severity == DiagnosticSeverity.Error } }
        return update(
            request.withData {
                if (hasError) {
                    execution.stoppedByDiagnostics {
                        diagnostics = progress.toMutableList()
                        completedAt = Instant.now()
                    }
                } else {
                    execution.awaitingSubmissions { diagnostics = progress.toMutableList() }
                }
            },
        )
    }

    @Transactional
    override fun recordTechnicalFailure(
        requestId: TaskValidationRequestId,
        failure: TaskValidationTechnicalFailure,
    ): TaskValidationRequest {
        val request = assemble(locked(requestId))
        if (!request.data.isActive) return request
        return update(
            request.withData {
                when (val state = request.data.execution) {
                    TaskValidationExecution.PendingDiagnostics,
                    TaskValidationExecution.DiagnosticsInProgress,
                    -> execution.incompleteDiagnosticsFailure { this.failure = failure }
                    is TaskValidationExecution.AwaitingSubmissions -> execution.completedDiagnosticsFailure {
                        diagnostics = state.diagnostics.toMutableList()
                        this.failure = failure
                    }
                    is TaskValidationExecution.SubmissionsCreated -> execution.createdSubmissionsFailure {
                        diagnostics = state.diagnostics.toMutableList()
                        submissions = state.submissions.ids.toMutableList()
                        this.failure = failure
                    }
                    is TaskValidationExecution.StoppedByDiagnostics,
                    is TaskValidationExecution.TechnicalFailure,
                    -> error("Request id=${requestId.value} is already stopped")
                }
            },
        )
    }

    @Transactional
    override fun save(data: TaskValidationRequestData): TaskValidationRequest {
        validateCompletedDiagnostics(data, data.snapshot.tests.ids.toSet())
        val row = requests.saveAndFlush(TaskValidationRequestMapping.toJpaEntity(data))
        val requestId = row.requireId()
        persistSnapshot(requestId, data.snapshot)
        persistSubmissions(requestId, (data.execution as? TaskValidationExecution.WithSubmissions)?.submissions?.ids.orEmpty())
        (data.execution as? TaskValidationExecution.WithDiagnostics)?.diagnostics?.forEach { persistResult(requestId, it) }
        return assemble(row)
    }

    @Transactional
    override fun update(entity: TaskValidationRequest): TaskValidationRequest {
        val current = locked(entity.id)
        val storedTests = tests.findAllByIdRequestId(entity.id.value).map { TestId(it.id.testId) }.toSet()
        validateCompletedDiagnostics(entity.data, storedTests)
        val saved = requests.saveAndFlush(TaskValidationRequestMapping.toJpaEntity(entity, current))
        (entity.data.execution as? TaskValidationExecution.WithDiagnostics)?.diagnostics?.forEach { result ->
            val key = TestDiagnosticResultId(requestId = entity.id.value, testId = result.testId.value)
            if (!results.existsById(key)) persistResult(entity.id.value, result)
        }
        syncSubmissions(entity.id.value, (entity.data.execution as? TaskValidationExecution.WithSubmissions)?.submissions?.ids.orEmpty())
        return assemble(saved)
    }

    @Transactional
    override fun removeById(id: TaskValidationRequestId) {
        val row = requests.findLockedById(id.value) ?: return
        results.findAllByIdRequestId(id.value).forEach { result ->
            reports.deleteAll(reports.findAllByRequestIdAndTestIdOrderByPositionAsc(requestId = id.value, testId = result.id.testId))
        }
        reports.flush()
        results.deleteAll(results.findAllByIdRequestId(id.value))
        tests.deleteAll(tests.findAllByIdRequestId(id.value))
        solutions.deleteAll(solutions.findAllByIdRequestId(id.value))
        versions.deleteAll(versions.findAllByIdRequestId(id.value))
        submissions.deleteAll(submissions.findAllByIdRequestId(id.value))
        results.flush()
        tests.flush()
        solutions.flush()
        versions.flush()
        submissions.flush()
        requests.delete(row)
    }

    @Transactional
    override fun removeByIds(ids: List<TaskValidationRequestId>) = ids.sortedBy { it.value }.forEach(::removeById)

    override fun assemble(jpaEntity: TaskValidationRequestJpaEntity): TaskValidationRequest {
        val requestId = jpaEntity.requireId()
        val snapshot = taskValidationSnapshot {
            tests = this@TaskValidationRequestPersistenceAdapter.tests.findAllByIdRequestId(requestId)
                .map { TestId(it.id.testId) }.toMutableList()
            developerSolutions = solutions.findAllByIdRequestId(requestId).sortedBy { it.id.developerSolutionId }
                .map { row ->
                    developerSolutionValidationInput {
                        developerSolution(row.id.developerSolutionId)
                        solution(row.solutionId)
                        expectedScore = Score(row.expectedScore)
                    }
                }.toMutableList()
            supportedTrikStudioVersions = versions.findAllByIdRequestId(requestId).map { row ->
                TrikStudioVersion(studioVersions.findByIdOrError(row.id.trikStudioVersionId).tag)
            }.toMutableList()
        }
        val data = taskValidationRequestData {
            task(jpaEntity.taskId)
            requestedBy(jpaEntity.requestedById)
            this.snapshot = snapshot
            val completedDiagnostics = if (jpaEntity.areDiagnosticsComplete) {
                findDiagnosticProgress(TaskValidationRequestId(requestId))
            } else {
                emptyList()
            }
            val submissionIds = this@TaskValidationRequestPersistenceAdapter.submissions.findAllByIdRequestId(requestId)
                .map { SubmissionId(it.id.submissionId) }
            TaskValidationRequestMapping.decodeExecution(
                row = jpaEntity,
                diagnostics = completedDiagnostics,
                submissionIds = submissionIds,
                builder = this,
            )
        }
        validateCompletedDiagnostics(data, snapshot.tests.ids.toSet())
        return TaskValidationRequestMapping.toDomain(jpaEntity, data)
    }

    private fun validateCompletedDiagnostics(data: TaskValidationRequestData, testIds: Set<TestId>) {
        val execution = data.execution
        if (execution is TaskValidationExecution.WithDiagnostics) {
            require(execution.diagnostics.map { it.testId }.toSet() == testIds && execution.diagnostics.size == testIds.size) {
                "Request diagnostics must cover exactly snapshot polygon ids=$testIds"
            }
        }
    }

    private fun locked(id: TaskValidationRequestId): TaskValidationRequestJpaEntity =
        requireNotNull(requests.findLockedById(id.value)) { "TaskValidationRequest id=${id.value} does not exist" }

    private fun snapshotOf(task: Task): TaskValidationSnapshot {
        val content = when (val revisions = task.data.content) {
            is TaskContent.New -> revisions.wip
            is TaskContent.Uncommitted -> revisions.wip
            is TaskContent.Committed -> error("Task id=${task.id.value} has no working revision for diagnostics")
        }
        return taskValidationSnapshot {
            tests = content.tests.ids.toMutableList()
            developerSolutions = developerSolutionRepository.load(content.developerSolutions).sortedBy { it.id.value }.map { solution ->
                developerSolutionValidationInput {
                    developerSolution = solution.id
                    this.solution = solution.data.solution.id
                    expectedScore = solution.data.expectedScore
                }
            }.toMutableList()
            supportedTrikStudioVersions = content.supportedTrikStudioVersions.toMutableList()
        }
    }

    private fun sameSnapshot(left: TaskValidationSnapshot, right: TaskValidationSnapshot): Boolean {
        return left.tests.ids == right.tests.ids &&
            left.supportedTrikStudioVersions == right.supportedTrikStudioVersions &&
            left.developerSolutions.map { Triple(it.developerSolution.id, it.solution.id, it.expectedScore) } ==
            right.developerSolutions.map { Triple(it.developerSolution.id, it.solution.id, it.expectedScore) }
    }

    private fun persistSnapshot(requestId: Long, snapshot: TaskValidationSnapshot) {
        tests.saveAll(TaskValidationRequestMapping.toTestAssociations(requestId, snapshot.tests.ids))
        solutions.saveAll(TaskValidationRequestMapping.toDeveloperSolutionAssociations(requestId, snapshot.developerSolutions))
        val versionIds = snapshot.supportedTrikStudioVersions.map { studioVersions.findIdByTagOrError(it.version) }
        versions.saveAll(TaskValidationRequestMapping.toTrikStudioVersionAssociations(requestId, versionIds))
    }

    private fun persistSubmissions(requestId: Long, ids: List<SubmissionId>) {
        submissions.saveAll(TaskValidationRequestMapping.toSubmissionAssociations(requestId, ids))
    }

    private fun syncSubmissions(requestId: Long, ids: List<SubmissionId>) = syncJoinTable(
        existing = submissions.findAllByIdRequestId(requestId),
        targetKeys = ids.distinct(),
        keyOf = { SubmissionId(it.id.submissionId) },
        buildAssociation = { TaskValidationRequestMapping.toSubmissionAssociations(requestId, listOf(it)).single() },
        deleteAll = { submissions.deleteAll(it) },
        saveAll = { submissions.saveAll(it) },
    )

    private fun persistResult(requestId: Long, result: TestDiagnosticResult) {
        results.saveAndFlush(
            TestDiagnosticResultJpaEntity(id = TestDiagnosticResultId(requestId = requestId, testId = result.testId.value)),
        )
        reports.saveAll(
            result.reports.mapIndexed { index, report ->
                TestDiagnosticResultMapping.toJpaEntity(
                    requestId = requestId,
                    testId = result.testId.value,
                    position = index,
                    report = report,
                )
            },
        )
        reports.flush()
    }

    private fun resultOf(requestId: Long, testId: Long): TestDiagnosticResult = testDiagnosticResult {
        this.testId = TestId(testId)
        reports = this@TaskValidationRequestPersistenceAdapter.reports.findAllByRequestIdAndTestIdOrderByPositionAsc(
            requestId = requestId,
            testId = testId,
        )
            .map { TestDiagnosticResultMapping.toDomain(it) }.toMutableList()
    }
}
