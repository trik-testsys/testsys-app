package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.builder.api.*
import tech.testsys.domain.contract.persistence.repository.DeveloperSolutionRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
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
import tech.testsys.infra.database.internal.utils.requireVersion
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
    private val submissionRepository: SubmissionRepository,
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

    @Transactional(readOnly = true)
    override fun startDiagnostics(requestId: TaskValidationRequestId): TaskValidationRequest? {
        val row = requests.findById(requestId.value).orElse(null) ?: return null
        return when (row.execution) {
            TaskValidationExecutionJpaEnum.PENDING_DIAGNOSTICS -> assemble(row)
            TaskValidationExecutionJpaEnum.AWAITING_SUBMISSIONS,
            TaskValidationExecutionJpaEnum.SUBMISSIONS_CREATED,
            TaskValidationExecutionJpaEnum.COMPLETED,
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
        check(row.execution == TaskValidationExecutionJpaEnum.PENDING_DIAGNOSTICS) {
            "Request id=${requestId.value} is not diagnosing: ${row.execution}"
        }
        persistResult(requestId.value, result)
        return resultOf(requestId = requestId.value, testId = result.testId.value)
    }

    @Transactional
    override fun completeDiagnostics(requestId: TaskValidationRequestId): TaskValidationRequest {
        val row = locked(requestId)
        if (row.areDiagnosticsComplete) return assemble(row)
        check(row.execution == TaskValidationExecutionJpaEnum.PENDING_DIAGNOSTICS) {
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
                    TaskValidationExecution.PendingDiagnostics -> execution.incompleteDiagnosticsFailure { this.failure = failure }
                    is TaskValidationExecution.AwaitingSubmissions -> execution.completedDiagnosticsFailure {
                        diagnostics = state.diagnostics.toMutableList()
                        this.failure = failure
                    }
                    is TaskValidationExecution.SubmissionsCreated -> execution.createdSubmissionsFailure {
                        diagnostics = state.diagnostics.toMutableList()
                        submissions = state.submissions.ids.toMutableList()
                        this.failure = failure
                    }
                    is TaskValidationExecution.Completed,
                    is TaskValidationExecution.StoppedByDiagnostics,
                    is TaskValidationExecution.TechnicalFailure,
                    -> error("Request id=${requestId.value} is already stopped")
                }
            },
        )
    }

    @Transactional
    override fun createSubmissions(requestId: TaskValidationRequestId): TaskValidationRequest {
        val request = assemble(locked(requestId))
        val state = request.data.execution
        if (state is TaskValidationExecution.SubmissionsCreated || state is TaskValidationExecution.Completed) return request
        check(state is TaskValidationExecution.AwaitingSubmissions) {
            "Request id=${requestId.value} cannot create submissions from $state"
        }
        val saved = TaskValidationSnapshot.authorRuns(request.data.snapshot).map { run ->
            submissionRepository.save(
                submissionData {
                    author = request.data.requestedBy.id
                    task = request.data.task.id
                    solution = run.input.solution.id
                    status.queued()
                    kind.developerSolutionTest { trikStudioVersion = run.trikStudioVersion }
                },
            )
        }
        return update(
            request.withData {
                execution.submissionsCreated {
                    diagnostics = state.diagnostics.toMutableList()
                    submissions = saved.map { it.id }.toMutableList()
                }
            },
        )
    }

    @Transactional
    override fun completeTesting(requestId: TaskValidationRequestId, failures: List<AuthorSubmissionFailure>): TaskValidationRequest {
        val request = assemble(locked(requestId))
        val state = request.data.execution
        if (state is TaskValidationExecution.Completed) return request
        check(state is TaskValidationExecution.SubmissionsCreated) {
            "Request id=${requestId.value} cannot complete testing from $state"
        }
        return update(
            request.withData {
                execution.completed {
                    diagnostics = state.diagnostics.toMutableList()
                    submissions = state.submissions.ids.toMutableList()
                    this.failures = failures.toMutableList()
                    completedAt = Instant.now()
                }
            },
        )
    }

    @Transactional(readOnly = true)
    override fun findActive(): List<TaskValidationRequest> =
        requests.findAllByExecutionInOrderByIdAsc(ACTIVE_EXECUTIONS).map { assemble(it) }

    @Transactional(readOnly = true)
    override fun findBySubmissionId(submissionId: SubmissionId): TaskValidationRequest? {
        val links = submissions.findAllByIdSubmissionId(submissionId.value)
        check(links.size <= 1) { "Submission id=${submissionId.value} belongs to several validation requests" }
        return links.singleOrNull()?.let { assemble(requests.findByIdOrError(it.id.requestId)) }
    }

    @Transactional
    override fun save(data: TaskValidationRequestData): TaskValidationRequest {
        validateCompletedDiagnostics(data, data.snapshot.tests.ids.toSet())
        val row = requests.saveAndFlush(TaskValidationRequestMapping.toJpaEntity(data))
        val requestId = row.requireId()
        persistSnapshot(requestId, data.snapshot)
        persistSubmissions(requestId, submissionIdsOf(data.execution), failuresOf(data.execution))
        (data.execution as? TaskValidationExecution.WithDiagnostics)?.diagnostics?.forEach { persistResult(requestId, it) }
        return assemble(row)
    }

    @Transactional
    override fun update(entity: TaskValidationRequest): TaskValidationRequest {
        val current = locked(entity.id)
        if (current.version != entity.requireVersion()) {
            throw OptimisticLockingFailureException("Stale task validation request id=${entity.id.value}")
        }
        check(assemble(current).data.isActive) { "Request id=${entity.id.value} is terminal: ${current.execution}" }
        val storedTests = tests.findAllByIdRequestId(entity.id.value).map { TestId(it.id.testId) }.toSet()
        validateCompletedDiagnostics(entity.data, storedTests)
        val saved = requests.saveAndFlush(TaskValidationRequestMapping.toJpaEntity(entity, current))
        (entity.data.execution as? TaskValidationExecution.WithDiagnostics)?.diagnostics?.forEach { result ->
            val key = TestDiagnosticResultId(requestId = entity.id.value, testId = result.testId.value)
            if (!results.existsById(key)) persistResult(entity.id.value, result)
        }
        syncSubmissions(entity.id.value, submissionIdsOf(entity.data.execution), failuresOf(entity.data.execution))
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
                .map { TestId(it.id.testId) }.sortedBy { it.value }.toMutableList()
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
            }.sortedBy { it.version }.toMutableList()
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
            val links = orderedSubmissionLinks(requestId)
            TaskValidationRequestMapping.decodeExecution(
                row = jpaEntity,
                diagnostics = completedDiagnostics,
                submissionIds = links.map { SubmissionId(it.id.submissionId) },
                failures = links.mapNotNull { TaskValidationRequestMapping.toFailure(it) },
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
            tests = content.tests.ids.sortedBy { it.value }.toMutableList()
            developerSolutions = developerSolutionRepository.load(content.developerSolutions).sortedBy { it.id.value }.map { solution ->
                developerSolutionValidationInput {
                    developerSolution = solution.id
                    this.solution = solution.data.solution.id
                    expectedScore = solution.data.expectedScore
                }
            }.toMutableList()
            supportedTrikStudioVersions = content.supportedTrikStudioVersions.sortedBy { it.version }.toMutableList()
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

    private fun submissionIdsOf(execution: TaskValidationExecution): List<SubmissionId> =
        (execution as? TaskValidationExecution.WithSubmissions)?.submissions?.ids.orEmpty()

    private fun failuresOf(execution: TaskValidationExecution): List<AuthorSubmissionFailure> =
        (execution as? TaskValidationExecution.Completed)?.failures.orEmpty()

    private fun persistSubmissions(requestId: Long, ids: List<SubmissionId>, failures: List<AuthorSubmissionFailure>) {
        submissions.saveAll(TaskValidationRequestMapping.toSubmissionAssociations(requestId, ids, failures))
    }

    private fun syncSubmissions(requestId: Long, ids: List<SubmissionId>, failures: List<AuthorSubmissionFailure>) {
        val existing = submissions.findAllByIdRequestIdOrderByPositionAsc(requestId)
        if (existing.map { SubmissionId(it.id.submissionId) } == ids &&
            existing.withIndex().all { (index, row) -> row.position == index } &&
            existing.mapNotNull { TaskValidationRequestMapping.toFailure(it) } == failures
        ) {
            return
        }
        submissions.deleteAll(existing)
        submissions.flush()
        persistSubmissions(requestId, ids, failures)
        submissions.flush()
    }

    private fun orderedSubmissionLinks(requestId: Long): List<SubmissionToTaskValidationRequestJpaEntity> {
        val links = submissions.findAllByIdRequestIdOrderByPositionAsc(requestId)
        check(links.withIndex().all { (index, link) -> link.position == index }) {
            "Request id=$requestId has inconsistent submission positions"
        }
        return links
    }

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

    private companion object {
        val ACTIVE_EXECUTIONS = listOf(
            TaskValidationExecutionJpaEnum.PENDING_DIAGNOSTICS,
            TaskValidationExecutionJpaEnum.AWAITING_SUBMISSIONS,
            TaskValidationExecutionJpaEnum.SUBMISSIONS_CREATED,
        )
    }
}
