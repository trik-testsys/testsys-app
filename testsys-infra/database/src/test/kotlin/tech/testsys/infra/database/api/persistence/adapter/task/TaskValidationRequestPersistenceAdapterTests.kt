package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.doThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import tech.testsys.domain.builder.api.*
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.TaskValidationRequestRepository
import tech.testsys.domain.model.task.*
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.task.DiagnosticReportJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.SubmissionJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.SubmissionToTaskValidationRequestJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TestDiagnosticResultJpaEntityRepository
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@OptIn(InternalDatabaseApi::class)
@TestPropertySource(
    properties = ["spring.datasource.url=jdbc:h2:mem:testsys_task_validation;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"],
)
class TaskValidationRequestPersistenceAdapterTests :
    UpdatablePersistenceAdapterContractTests<TaskValidationRequestData, TaskValidationRequestId, TaskValidationRequest>() {
    @Autowired
    override lateinit var repository: TaskValidationRequestRepository

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var submissionRows: SubmissionJpaEntityRepository

    @MockitoSpyBean
    private lateinit var savedSubmissions: SubmissionRepository

    @Autowired
    private lateinit var contestRepository: ContestRepository

    @Autowired
    private lateinit var results: TestDiagnosticResultJpaEntityRepository

    @Autowired
    private lateinit var reports: DiagnosticReportJpaEntityRepository

    @Autowired
    private lateinit var submissionLinks: SubmissionToTaskValidationRequestJpaEntityRepository

    @Test
    fun `should preserve shared programs distinct expectations and deterministic submission order on creation and repeat`() {
        val task = fixtures.workingTask()
        val polygon = fixtures.polygon()
        val program = fixtures.solution()
        val first = fixtures.developerSolution(solution = program, expectedScore = 5)
        val second = fixtures.developerSolution(solution = program, expectedScore = 9)
        val firstVersion = fixtures.trikStudioVersion("v1-" + fixtures.unique("version"))
        val secondVersion = fixtures.trikStudioVersion("v2-" + fixtures.unique("version"))
        val edited = taskRepository.update(
            task.withData {
                content.new {
                    tests = mutableListOf(polygon.id)
                    developerSolutions = mutableListOf(second.id, first.id)
                    supportedTrikStudioVersions = mutableListOf(secondVersion, firstVersion)
                }
            },
        )
        val request = repository.findOrCreateActive(edited.id, edited.data.owner.id)
        repository.startDiagnostics(request.id)
        repository.saveDiagnosticProgress(request.id, testDiagnosticResult { testId = polygon.id })
        repository.completeDiagnostics(request.id)
        val count = submissionRows.count()

        val created = repository.createSubmissions(request.id)
        val repeated = repository.createSubmissions(request.id)
        val read = requireNotNull(repository.findById(request.id))

        val links = (created.data.execution as TaskValidationExecution.WithSubmissions).submissions.ids
        val saved = links.map { id -> requireNotNull(savedSubmissions.findById(id)) }
        assertEquals(4, links.distinct().size)
        assertEquals(count + 4, submissionRows.count())
        assertEquals(
            listOf(firstVersion, secondVersion, firstVersion, secondVersion),
            saved.map { (it.data.kind as SubmissionKind.DeveloperSolutionTest).trikStudioVersion },
        )
        assertEquals(List(4) { program.id }, saved.map { it.data.solution.id })
        assertEquals(List(4) { SubmissionStatus.Queued }, saved.map { it.data.status })
        assertEquals(links, (repeated.data.execution as TaskValidationExecution.WithSubmissions).submissions.ids)
        assertEquals(links, (read.data.execution as TaskValidationExecution.WithSubmissions).submissions.ids)
        assertEquals(listOf(Score(5), Score(9)), read.data.snapshot.developerSolutions.map { it.expectedScore })
        assertEquals(created.id, repository.findBySubmissionId(links.first())?.id)
    }

    @Test
    fun `should roll back earlier submissions when saving a later submission fails and permit retry`() {
        val request = readyRequest()
        val secondVersion = request.data.snapshot.supportedTrikStudioVersions[1]
        val failure = IllegalStateException("Second submission save failed")
        doThrow(failure).doCallRealMethod().`when`(savedSubmissions).save(submissionIn(secondVersion))
        val count = submissionRows.count()

        assertThrows(IllegalStateException::class.java) { repository.createSubmissions(request.id) }

        assertEquals(count, submissionRows.count())
        assertInstanceOf(TaskValidationExecution.AwaitingSubmissions::class.java, repository.findById(request.id)?.data?.execution)
        val retried = repository.createSubmissions(request.id)
        assertEquals(2, (retried.data.execution as TaskValidationExecution.WithSubmissions).submissions.ids.size)
        assertEquals(count + 2, submissionRows.count())
    }

    @Test
    fun `should reject creating submissions before diagnostics complete`() {
        val request = repository.save(newData())

        assertThrows(IllegalStateException::class.java) { repository.createSubmissions(request.id) }

        assertEquals(TaskValidationExecution.PendingDiagnostics, repository.findById(request.id)?.data?.execution)
    }

    @Test
    fun `should round trip every failure of completed testing with ordered links and keep the request terminal`() {
        val created = repository.createSubmissions(readyRequest().id)
        val state = created.data.execution as TaskValidationExecution.WithSubmissions
        val failures = listOf(
            AuthorSubmissionFailure.GradingFailed(state.submissions.ids.first()),
            AuthorSubmissionFailure.ScoreMismatch(submission = state.submissions.ids.last(), actualScore = 4_294_967_294L),
        )

        val completed = repository.completeTesting(requestId = created.id, failures = failures)
        val repeated = repository.completeTesting(requestId = created.id, failures = emptyList())

        val read = requireNotNull(repository.findById(completed.id))
        val result = assertInstanceOf(TaskValidationExecution.Completed::class.java, read.data.execution)
        assertEquals(failures, result.failures)
        assertEquals(state.submissions.ids, result.submissions.ids)
        assertEquals(state.diagnostics, result.diagnostics)
        assertNotNull(result.completedAt)
        assertFalse(read.data.isActive)
        assertEquals(failures, (repeated.data.execution as TaskValidationExecution.Completed).failures)
        assertNull(repository.startDiagnostics(read.id))
    }

    @Test
    fun `should round trip successful testing without failures`() {
        val created = repository.createSubmissions(readyRequest().id)

        repository.completeTesting(requestId = created.id, failures = emptyList())

        val read = requireNotNull(repository.findById(created.id))
        assertEquals(emptyList<AuthorSubmissionFailure>(), (read.data.execution as TaskValidationExecution.Completed).failures)
    }

    @Test
    fun `should reject failures out of submission order without completing testing`() {
        val created = repository.createSubmissions(readyRequest().id)
        val ids = (created.data.execution as TaskValidationExecution.WithSubmissions).submissions.ids
        val failures = ids.reversed().map { AuthorSubmissionFailure.GradingFailed(it) }

        assertThrows(IllegalArgumentException::class.java) { repository.completeTesting(requestId = created.id, failures = failures) }

        assertInstanceOf(TaskValidationExecution.SubmissionsCreated::class.java, repository.findById(created.id)?.data?.execution)
    }

    @Test
    fun `should reject completing testing before submissions are created`() {
        val request = readyRequest()

        assertThrows(IllegalStateException::class.java) {
            repository.completeTesting(requestId = request.id, failures = emptyList())
        }

        assertInstanceOf(TaskValidationExecution.AwaitingSubmissions::class.java, repository.findById(request.id)?.data?.execution)
    }

    @Test
    fun `should reject updating a terminal request`() {
        val created = repository.createSubmissions(readyRequest().id)
        val completed = repository.completeTesting(requestId = created.id, failures = emptyList())

        assertThrows(IllegalStateException::class.java) {
            repository.update(completed.withData { execution.pendingDiagnostics() })
        }

        assertInstanceOf(TaskValidationExecution.Completed::class.java, repository.findById(completed.id)?.data?.execution)
    }

    @Test
    fun `should find only active requests`() {
        val pending = repository.save(newData())
        val awaiting = readyRequest()
        val created = repository.createSubmissions(readyRequest().id)
        val completed = repository.completeTesting(repository.createSubmissions(readyRequest().id).id, emptyList())

        val active = repository.findActive().map { it.id }

        assertTrue(active.containsAll(listOf(pending.id, awaiting.id, created.id)))
        assertFalse(completed.id in active)
        assertEquals(active.sortedBy { it.value }, active)
    }

    @Test
    fun `should find only contests currently attached to the task`() {
        val task = fixtures.task()
        val included = fixtures.contest()
        fixtures.contest()
        contestRepository.update(included.withData { tasks = mutableListOf(task.id) })

        val found = contestRepository.findByTaskId(task.id)

        assertEquals(listOf(included.id), found.map { it.id })
    }

    @Test
    fun `should retain completed diagnostics when a technical failure is recorded after the stage`() {
        val request = fixtures.taskValidationRequest()
        repository.startDiagnostics(request.id)
        repository.completeDiagnostics(request.id)
        val failure = taskValidationTechnicalFailure {
            description = "Dispatch failure"
            occurredAt = Instant.EPOCH
        }

        val stopped = repository.recordTechnicalFailure(requestId = request.id, failure = failure)

        val state =
            assertInstanceOf(TaskValidationExecution.TechnicalFailure.CompletedDiagnostics::class.java, stopped.data.execution)
        assertEquals(emptyList<TestDiagnosticResult>(), state.diagnostics)
        assertEquals(failure, state.failure)
        assertEquals(Instant.EPOCH, state.completedAt)
        assertFalse(stopped.data.isActive)
        assertNull(repository.startDiagnostics(request.id))
        assertEquals(stopped.id, repository.recordTechnicalFailure(request.id, failure).id)
    }

    @Test
    fun `should retain completed diagnostics and submission references when a technical failure is recorded`() {
        val submission = fixtures.submission()
        val request = fixtures.taskValidationRequest()
        val submitted = repository.update(
            request.withData {
                execution.submissionsCreated { submissions = mutableListOf(submission.id) }
            },
        )
        val failure = taskValidationTechnicalFailure {
            description = "Grading unavailable"
            occurredAt = Instant.EPOCH
        }

        val stopped = repository.recordTechnicalFailure(requestId = submitted.id, failure = failure)

        val state =
            assertInstanceOf(TaskValidationExecution.TechnicalFailure.CreatedSubmissions::class.java, stopped.data.execution)
        assertEquals(emptyList<TestDiagnosticResult>(), state.diagnostics)
        assertEquals(listOf(submission.id), state.submissions.ids)
        assertEquals(failure, state.failure)
        assertFalse(stopped.data.isActive)
        assertNull(repository.startDiagnostics(request.id))
        assertSameData(stopped, repository.recordTechnicalFailure(request.id, failure))
    }

    override fun newData(): TaskValidationRequestData {
        val task = fixtures.task()
        val test = fixtures.polygon()
        val author = fixtures.developerSolution()
        val version = fixtures.trikStudioVersion()
        return taskValidationRequestData {
            this.task = task.id
            requestedBy = task.data.owner.id
            snapshot = taskValidationSnapshot {
                tests = mutableListOf(test.id)
                developerSolutions += developerSolutionValidationInput {
                    developerSolution = author.id
                    solution = author.data.solution.id
                    expectedScore = author.data.expectedScore
                }
                supportedTrikStudioVersions = mutableListOf(version)
            }
            execution.pendingDiagnostics()
        }
    }

    override fun modified(entity: TaskValidationRequest): TaskValidationRequest {
        val submission = fixtures.submission()
        return entity.withData {
            execution.createdSubmissionsFailure {
                diagnostics = mutableListOf(testDiagnosticResult { testId = entity.data.snapshot.tests.ids.single() })
                submissions = mutableListOf(submission.id)
                failure = taskValidationTechnicalFailure {
                    description = "Processing failed"
                    occurredAt = Instant.EPOCH
                }
            }
        }
    }

    override fun detached(entity: TaskValidationRequest): TaskValidationRequest = taskValidationRequest {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long): TaskValidationRequestId = TaskValidationRequestId(value)

    override fun assertSameData(expected: TaskValidationRequest, actual: TaskValidationRequest) {
        assertEquals(expected.data.task.id, actual.data.task.id)
        assertEquals(expected.data.requestedBy.id, actual.data.requestedBy.id)
        assertEquals(expected.data.snapshot.tests.ids, actual.data.snapshot.tests.ids)
        assertEquals(expected.data.snapshot.supportedTrikStudioVersions, actual.data.snapshot.supportedTrikStudioVersions)
        assertEquals(
            expected.data.snapshot.developerSolutions.map { Triple(it.developerSolution.id, it.solution.id, it.expectedScore) },
            actual.data.snapshot.developerSolutions.map { Triple(it.developerSolution.id, it.solution.id, it.expectedScore) },
        )
        assertEquals(expected.data.execution::class, actual.data.execution::class)
        assertEquals(
            (expected.data.execution as? TaskValidationExecution.WithDiagnostics)?.diagnostics,
            (actual.data.execution as? TaskValidationExecution.WithDiagnostics)?.diagnostics,
        )
        assertEquals(
            (expected.data.execution as? TaskValidationExecution.WithSubmissions)?.submissions?.ids,
            (actual.data.execution as? TaskValidationExecution.WithSubmissions)?.submissions?.ids,
        )
        assertEquals(
            (expected.data.execution as? TaskValidationExecution.TechnicalFailure)?.failure,
            (actual.data.execution as? TaskValidationExecution.TechnicalFailure)?.failure,
        )
    }

    @Test
    fun `should keep task initiator and every snapshot input fixed on update`() {
        val saved = repository.save(newData())
        val anotherTask = fixtures.task()
        val anotherOwner = fixtures.developer()

        val updated = repository.update(
            saved.withData {
                task = anotherTask.id
                requestedBy = anotherOwner.id
                snapshot = taskValidationSnapshot {}
            },
        )

        assertSameData(saved, updated)
    }

    @Test
    fun `should reject creating a request when the stored task has no working revision`() {
        val task = fixtures.task()

        assertThrows(IllegalStateException::class.java) {
            repository.findOrCreateActive(task.id, task.data.owner.id)
        }

        assertEquals(emptyList<TaskValidationRequest>(), repository.findHistory(task.id))
    }

    @Test
    fun `should continue a saved working snapshot after the task is committed with different inputs`() {
        val task = fixtures.workingTask()
        val workingTest = fixtures.polygon()
        val committedTest = fixtures.polygon()
        val exercise = fixtures.exercise()
        val statement = fixtures.statement()
        val editable = taskRepository.update(
            task.withData {
                content.new { tests = mutableListOf(workingTest.id) }
            },
        )
        val request = repository.findOrCreateActive(task.id, task.data.owner.id)
        taskRepository.update(
            editable.withData {
                content.committed {
                    tests = mutableListOf(committedTest.id)
                    exercises = mutableListOf(exercise.id)
                    this.statement = statement.id
                }
            },
        )
        val started = requireNotNull(repository.startDiagnostics(request.id))
        val result = testDiagnosticResult { testId = started.data.snapshot.tests.ids.single() }
        repository.saveDiagnosticProgress(requestId = request.id, result = result)

        val completed = repository.completeDiagnostics(request.id)

        assertEquals(listOf(workingTest.id), completed.data.snapshot.tests.ids)
        val completedState =
            assertInstanceOf(TaskValidationExecution.WithDiagnostics::class.java, completed.data.execution)
        assertEquals(listOf(result), completedState.diagnostics)
        assertInstanceOf(TaskValidationExecution.AwaitingSubmissions::class.java, completed.data.execution)
        assertThrows(IllegalStateException::class.java) {
            repository.findOrCreateActive(task.id, task.data.owner.id)
        }
    }

    @Test
    fun `should distinguish absent stage results and submissions from completed empty collections`() {
        val request = fixtures.taskValidationRequest()
        val started = requireNotNull(repository.startDiagnostics(request.id))

        val completed = repository.completeDiagnostics(started.id)

        assertEquals(TaskValidationExecution.PendingDiagnostics, request.data.execution)
        val state = assertInstanceOf(TaskValidationExecution.AwaitingSubmissions::class.java, completed.data.execution)
        assertEquals(emptyList<TestDiagnosticResult>(), state.diagnostics)
        assertTrue(completed.data.isActive)
        assertEquals(completed.id, repository.findOrCreateActive(completed.data.task.id, completed.data.requestedBy.id).id)
    }

    @Test
    fun `should preserve empty submitted collection separately from no submission`() {
        val request = fixtures.taskValidationRequest()

        val updated = repository.update(request.withData { execution.submissionsCreated {} })

        val state = assertInstanceOf(TaskValidationExecution.SubmissionsCreated::class.java, updated.data.execution)
        assertEquals(emptyList<SubmissionId>(), state.submissions.ids)
    }

    @Test
    fun `should atomically deduplicate concurrent requests for the same task snapshot`() {
        val task = fixtures.workingTask()
        val gate = CountDownLatch(2)
        val executor = Executors.newFixedThreadPool(2)

        val requests = executor.use {
            it.invokeAll(
                listOf(
                    Callable {
                        gate.countDown()
                        check(gate.await(10, TimeUnit.SECONDS))
                        repository.findOrCreateActive(task.id, task.data.owner.id)
                    },
                    Callable {
                        gate.countDown()
                        check(gate.await(10, TimeUnit.SECONDS))
                        repository.findOrCreateActive(task.id, task.data.owner.id)
                    },
                ),
            ).map { result -> result.get() }
        }

        assertEquals(requests[0].id, requests[1].id)
        assertEquals(1, repository.findHistory(task.id).size)
    }

    @Test
    fun `should use editable inputs and preserve the old snapshot after task edits`() {
        val task = fixtures.task()
        val oldTest = fixtures.polygon()
        val committedTest = fixtures.polygon()
        val newTest = fixtures.polygon()
        val author = fixtures.developerSolution()
        val version = fixtures.trikStudioVersion()
        val committedVersion = fixtures.trikStudioVersion()
        val editable = taskRepository.update(
            task.withData {
                content.uncommitted(
                    wipBuilder = {
                        tests = mutableListOf(oldTest.id)
                        developerSolutions = mutableListOf(author.id)
                        supportedTrikStudioVersions = mutableListOf(version)
                    },
                    lastCommittedBuilder = {
                        statement = (task.data.content as? TaskContent.Committed)?.lastCommitted?.statement?.id
                        exercises = (task.data.content as? TaskContent.Committed)?.lastCommitted?.exercises?.ids.orEmpty().toMutableList()
                        tests = mutableListOf(committedTest.id)
                        supportedTrikStudioVersions = mutableListOf(committedVersion)
                    },
                )
            },
        )
        val first = repository.findOrCreateActive(task.id, task.data.owner.id)
        taskRepository.update(
            editable.withData {
                content.new { tests = mutableListOf(newTest.id) }
            },
        )

        val second = repository.findOrCreateActive(task.id, task.data.owner.id)

        assertNotEquals(first.id, second.id)
        assertEquals(listOf(oldTest.id), first.data.snapshot.tests.ids)
        assertEquals(author.data.expectedScore, first.data.snapshot.developerSolutions.single().expectedScore)
        assertEquals(author.data.solution.id, first.data.snapshot.developerSolutions.single().solution.id)
        assertEquals(listOf(version), first.data.snapshot.supportedTrikStudioVersions)
        assertEquals(listOf(newTest.id), second.data.snapshot.tests.ids)
        assertEquals(listOf(oldTest.id), repository.findById(first.id)?.data?.snapshot?.tests?.ids)
        assertEquals(2, repository.findHistory(task.id).size)
    }

    @Test
    fun `should ignore metadata and uploaded unattached resources when deduplicating`() {
        val task = fixtures.workingTask()
        val first = repository.findOrCreateActive(task.id, task.data.owner.id)
        val unrelated = fixtures.polygon()
        taskRepository.update(
            task.withData {
                name = fixtures.unique("Renamed")
                description = "Updated"
                uploadedResources += unrelated.data.versionBucket
            },
        )

        val repeated = repository.findOrCreateActive(task.id, task.data.owner.id)

        assertEquals(first.id, repeated.id)
        assertEquals(1, repository.findHistory(task.id).size)
    }

    @Test
    fun `should ignore polygon order when deduplicating`() {
        val task = fixtures.workingTask()
        val first = fixtures.polygon()
        val second = fixtures.polygon()
        val editable = taskRepository.update(task.withData { content.new { tests = mutableListOf(first.id, second.id) } })
        val request = repository.findOrCreateActive(task.id, task.data.owner.id)
        taskRepository.update(editable.withData { content.new { tests = mutableListOf(second.id, first.id) } })

        val repeated = repository.findOrCreateActive(task.id, task.data.owner.id)

        assertEquals(request.id, repeated.id)
        assertEquals(1, repository.findHistory(task.id).size)
    }

    @Test
    fun `should pin one complete snapshot when task editing races request creation`() {
        val task = fixtures.workingTask()
        val oldTest = fixtures.polygon()
        val newTest = fixtures.polygon()
        val oldAuthor = fixtures.developerSolution()
        val newAuthor = fixtures.developerSolution()
        val oldVersion = fixtures.trikStudioVersion()
        val newVersion = fixtures.trikStudioVersion()
        val editable = taskRepository.update(
            task.withData {
                content.new {
                    tests = mutableListOf(oldTest.id)
                    developerSolutions = mutableListOf(oldAuthor.id)
                    supportedTrikStudioVersions = mutableListOf(oldVersion)
                }
            },
        )
        val gate = CountDownLatch(2)
        val executor = Executors.newFixedThreadPool(2)

        val request = executor.use { pool ->
            val edit = pool.submit(
                Callable {
                    gate.countDown()
                    check(gate.await(10, TimeUnit.SECONDS))
                    taskRepository.update(
                        editable.withData {
                            content.new {
                                tests = mutableListOf(newTest.id)
                                developerSolutions = mutableListOf(newAuthor.id)
                                supportedTrikStudioVersions = mutableListOf(newVersion)
                            }
                        },
                    )
                },
            )
            val create = pool.submit(
                Callable {
                    gate.countDown()
                    check(gate.await(10, TimeUnit.SECONDS))
                    repository.findOrCreateActive(task.id, task.data.owner.id)
                },
            )
            edit.get()
            create.get()
        }

        val actual = Triple(
            request.data.snapshot.tests.ids,
            request.data.snapshot.developerSolutions.map { it.developerSolution.id },
            request.data.snapshot.supportedTrikStudioVersions,
        )
        assertTrue(
            actual in setOf(
                Triple(listOf(oldTest.id), listOf(oldAuthor.id), listOf(oldVersion)),
                Triple(listOf(newTest.id), listOf(newAuthor.id), listOf(newVersion)),
            ),
        )
    }

    @Test
    fun `should retain partial progress without exposing a completed diagnostic stage`() {
        val request = repository.save(newData())
        repository.startDiagnostics(request.id)
        val result = testDiagnosticResult { testId = request.data.snapshot.tests.ids.single() }

        repository.saveDiagnosticProgress(requestId = request.id, result = result)

        val stored = requireNotNull(repository.findById(request.id))
        assertEquals(TaskValidationExecution.PendingDiagnostics, stored.data.execution)
        assertEquals(request.version, stored.version)
        assertEquals(listOf(result), repository.findDiagnosticProgress(request.id))
        assertEquals(TaskValidationExecution.PendingDiagnostics, repository.startDiagnostics(request.id)?.data?.execution)
    }

    @Test
    fun `should preserve the first result on repeated and concurrent progress writes`() {
        val request = repository.save(newData())
        repository.startDiagnostics(request.id)
        val result = testDiagnosticResult {
            testId = request.data.snapshot.tests.ids.single()
            reports += diagnosticReport {
                severity = DiagnosticSeverity.Warning
                data.missingScoreOutput()
            }
        }
        val executor = Executors.newFixedThreadPool(2)

        val written = executor.use {
            it.invokeAll(
                listOf(
                    Callable { repository.saveDiagnosticProgress(request.id, result) },
                    Callable { repository.saveDiagnosticProgress(request.id, result) },
                ),
            ).map { future -> future.get() }
        }

        assertEquals(listOf(result, result), written)
        assertEquals(listOf(result), repository.findDiagnosticProgress(request.id))
    }

    @Test
    fun `should reject stage completion before all polygon results are saved`() {
        val request = repository.save(newData())
        repository.startDiagnostics(request.id)

        assertThrows(IllegalStateException::class.java) { repository.completeDiagnostics(request.id) }

        assertEquals(TaskValidationExecution.PendingDiagnostics, repository.findById(request.id)?.data?.execution)
    }

    @Test
    fun `should stop on Error only after all polygons have saved results`() {
        val request = repository.save(newData())
        repository.startDiagnostics(request.id)
        val result = testDiagnosticResult {
            testId = request.data.snapshot.tests.ids.single()
            reports += diagnosticReport {
                severity = DiagnosticSeverity.Error
                data.missingTimeLimit()
            }
        }
        repository.saveDiagnosticProgress(requestId = request.id, result = result)

        val completed = repository.completeDiagnostics(request.id)

        val state = assertInstanceOf(TaskValidationExecution.StoppedByDiagnostics::class.java, completed.data.execution)
        assertFalse(completed.data.isActive)
        assertNotNull(state.completedAt)
        assertEquals(listOf(result), state.diagnostics)
        assertNull(repository.startDiagnostics(request.id))
    }

    @Test
    fun `should preserve progress after a technical stop and create a new request on the next launch`() {
        val task = fixtures.workingTask()
        val test = fixtures.polygon()
        val editable = taskRepository.update(task.withData { content.new { tests = mutableListOf(test.id) } })
        val request = repository.findOrCreateActive(task.id, task.data.owner.id)
        repository.startDiagnostics(request.id)
        val result = testDiagnosticResult { testId = test.id }
        repository.saveDiagnosticProgress(requestId = request.id, result = result)
        val failure = taskValidationTechnicalFailure {
            description = "Storage failure"
            occurredAt = Instant.EPOCH
        }

        val stopped = repository.recordTechnicalFailure(requestId = request.id, failure = failure)

        val state =
            assertInstanceOf(TaskValidationExecution.TechnicalFailure.IncompleteDiagnostics::class.java, stopped.data.execution)
        assertEquals(listOf(result), repository.findDiagnosticProgress(request.id))
        assertEquals(failure, state.failure)
        assertNull(repository.startDiagnostics(request.id))
        assertNotEquals(request.id, repository.findOrCreateActive(editable.id, editable.data.owner.id).id)
        assertEquals(2, repository.findHistory(task.id).size)
    }

    @Test
    fun `should find requests by ids with the same statement count for one and twenty ids`() {
        val data = newData()
        val testId = data.snapshot.tests.ids.single()
        val submissionId = fixtures.submission().id
        val created = taskValidationRequestData {
            task = data.task.id
            requestedBy = data.requestedBy.id
            snapshot = data.snapshot
            execution.submissionsCreated {
                diagnostics = mutableListOf(reportedResult(testId))
                submissions = mutableListOf(submissionId)
            }
        }
        val ids = List(20) { repository.save(created).id }

        val (one, oneIdStatements) = withStatementCount { repository.findByIds(ids.take(1)) }
        val (twenty, twentyIdsStatements) = withStatementCount { repository.findByIds(ids) }

        assertEquals(ids.take(1), one.map { request -> request.id })
        assertEquals(ids.toSet(), twenty.map { request -> request.id }.toSet())
        assertEquals(
            List(20) { listOf(reportedResult(testId)) to listOf(submissionId) },
            twenty.map { request ->
                val state =
                    assertInstanceOf(TaskValidationExecution.SubmissionsCreated::class.java, request.data.execution)
                state.diagnostics to state.submissions.ids
            },
        )
        assertEquals(
            List(20) { data.snapshot.supportedTrikStudioVersions },
            twenty.map { request -> request.data.snapshot.supportedTrikStudioVersions },
        )
        assertEquals(oneIdStatements, twentyIdsStatements)
    }

    @Test
    fun `should delete diagnostic results reports and submission links together with the removed request`() {
        val data = newData()
        val testId = data.snapshot.tests.ids.single()
        val submissionId = fixtures.submission().id
        val saved = repository.save(
            taskValidationRequestData {
                task = data.task.id
                requestedBy = data.requestedBy.id
                snapshot = data.snapshot
                execution.submissionsCreated {
                    diagnostics = mutableListOf(reportedResult(testId))
                    submissions = mutableListOf(submissionId)
                }
            },
        )

        repository.removeById(saved.id)

        val requestId = saved.id.value
        assertNull(repository.findById(saved.id))
        assertEquals(emptyList<Long>(), results.findAllByIdRequestId(requestId).map { result -> result.id.testId })
        assertEquals(
            emptyList<Int>(),
            reports.findAllByRequestIdAndTestIdOrderByPositionAsc(requestId, testId.value).map { report -> report.position },
        )
        assertEquals(
            emptyList<Long>(),
            submissionLinks.findAllByIdRequestIdOrderByPositionAsc(requestId).map { link -> link.id.submissionId },
        )
        assertEquals(submissionId, savedSubmissions.findById(submissionId)?.id)
    }

    @Test
    fun `should find the history with the same statement count for one and twenty requests`() {
        val oneRequestData = newData()
        val twentyRequestsData = newData()
        val single = repository.save(oneRequestData).id
        val twentyIds = List(20) { repository.save(twentyRequestsData).id }

        val (one, oneRequestStatements) = withStatementCount { repository.findHistory(oneRequestData.task.id) }
        val (twenty, twentyRequestsStatements) = withStatementCount { repository.findHistory(twentyRequestsData.task.id) }

        assertEquals(listOf(single), one.map { request -> request.id })
        assertEquals(twentyIds, twenty.map { request -> request.id })
        assertEquals(oneRequestStatements, twentyRequestsStatements)
    }

    @Test
    fun `should find diagnostic progress with the same statement count for one and twenty polygons`() {
        val onePolygonRequest = requestWithDiagnostics(polygonCount = 1)
        val twentyPolygonsRequest = requestWithDiagnostics(polygonCount = 20)

        val (one, onePolygonStatements) = withStatementCount { repository.findDiagnosticProgress(onePolygonRequest.id) }
        val (twenty, twentyPolygonsStatements) = withStatementCount { repository.findDiagnosticProgress(twentyPolygonsRequest.id) }

        assertEquals(onePolygonRequest.data.snapshot.tests.ids.map(::reportedResult), one)
        assertEquals(twentyPolygonsRequest.data.snapshot.tests.ids.map(::reportedResult), twenty)
        assertEquals(onePolygonStatements, twentyPolygonsStatements)
    }

    private fun requestWithDiagnostics(polygonCount: Int): TaskValidationRequest {
        val task = fixtures.task()
        val testIds = List(polygonCount) { fixtures.polygon().id }.sortedBy { id -> id.value }
        return repository.save(
            taskValidationRequestData {
                this.task = task.id
                requestedBy = task.data.owner.id
                snapshot = taskValidationSnapshot { tests = testIds.toMutableList() }
                execution.awaitingSubmissions { diagnostics = testIds.map(::reportedResult).toMutableList() }
            },
        )
    }

    private fun reportedResult(testId: TestId): TestDiagnosticResult = testDiagnosticResult {
        this.testId = testId
        reports += diagnosticReport {
            severity = DiagnosticSeverity.Warning
            data.missingScoreOutput()
        }
    }

    private fun readyRequest(): TaskValidationRequest {
        val task = fixtures.workingTask()
        val polygon = fixtures.polygon()
        val author = fixtures.developerSolution()
        val firstVersion = fixtures.trikStudioVersion("v1-" + fixtures.unique("version"))
        val secondVersion = fixtures.trikStudioVersion("v2-" + fixtures.unique("version"))
        val edited = taskRepository.update(
            task.withData {
                content.new {
                    tests = mutableListOf(polygon.id)
                    developerSolutions = mutableListOf(author.id)
                    supportedTrikStudioVersions = mutableListOf(firstVersion, secondVersion)
                }
            },
        )
        val request = repository.findOrCreateActive(edited.id, edited.data.owner.id)
        repository.startDiagnostics(request.id)
        repository.saveDiagnosticProgress(request.id, testDiagnosticResult { testId = polygon.id })
        return repository.completeDiagnostics(request.id)
    }

    // The matcher returns null while stubbing; the placeholder only satisfies the non-null Kotlin parameter.
    private fun submissionIn(version: TrikStudioVersion): SubmissionData =
        argThat<SubmissionData> { data -> (data?.kind as? SubmissionKind.DeveloperSolutionTest)?.trikStudioVersion == version }
            ?: submissionData {
                author(0)
                task(0)
                solution(0)
                status.queued()
                kind.developerSolutionTest { trikStudioVersion = version }
            }
}
