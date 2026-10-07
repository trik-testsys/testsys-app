@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import com.google.protobuf.ByteString
import io.mockk.every
import io.mockk.mockk
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.SimpleTransactionStatus
import tech.testsys.domain.builder.api.contest
import tech.testsys.domain.builder.api.developerSolutionValidationInput
import tech.testsys.domain.builder.api.logs
import tech.testsys.domain.builder.api.recording
import tech.testsys.domain.builder.api.solution
import tech.testsys.domain.builder.api.submission
import tech.testsys.domain.builder.api.task
import tech.testsys.domain.builder.api.taskValidationRequest
import tech.testsys.domain.builder.api.taskValidationSnapshot
import tech.testsys.domain.builder.api.test
import tech.testsys.domain.builder.api.testDiagnosticResult
import tech.testsys.domain.builder.api.verdict
import tech.testsys.domain.builder.data
import tech.testsys.domain.contract.GradingNodeStatus
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.LogsRepository
import tech.testsys.domain.contract.persistence.repository.RecordingRepository
import tech.testsys.domain.contract.persistence.repository.SolutionRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.TaskValidationRequestRepository
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.LogsData
import tech.testsys.domain.model.task.RecordingData
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionKind
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TaskValidationRequest
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData
import tech.testsys.domain.model.task.VersionBucket
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import trik.testsys.grading.GradingNodeOuterClass as Proto

internal val settings = GradingSettings(
    maxAttempts = 3,
    totalTimeout = Duration.ofMinutes(30),
    rpcTimeout = Duration.ofMinutes(10),
    statusTimeout = Duration.ofSeconds(1),
    pollInterval = Duration.ofSeconds(5),
    maxMessageBytes = 400_000_000,
    shouldRecordVideo = true,
)

internal fun testSubmission(id: Long = 42) = submission {
    this.id = id
    createdAt = Instant.EPOCH
    data {
        author(1)
        solution(2)
        task(3)
        kind.developerSolutionTest { trikStudioVersion("2025.1") }
        status.queued()
    }
}

internal fun validationRequest(
    submitted: Submission = testSubmission(),
    version: TrikStudioVersion = TrikStudioVersion("2025.1"),
): TaskValidationRequest = taskValidationRequest {
    id = 11
    createdAt = Instant.EPOCH
    data {
        task = submitted.data.task.id
        requestedBy(submitted.data.author.id.value)
        snapshot = taskValidationSnapshot {
            tests(listOf(4))
            developerSolutions = mutableListOf(
                developerSolutionValidationInput {
                    developerSolution(7)
                    solution = submitted.data.solution.id
                    expectedScore = Score(5)
                },
            )
            supportedTrikStudioVersions = mutableListOf(version)
        }
        execution.submissionsCreated {
            diagnostics = mutableListOf(testDiagnosticResult { testId(4) })
            submissions = mutableListOf(submitted.id)
        }
    }
}

internal fun field(name: String = "4", content: String = """[{"level":"info","message":"Набрано баллов: 17"}]"""): Proto.FieldResult =
    Proto.FieldResult.newBuilder().setName(name).setVerdict(
        Proto.File.newBuilder().setName("logs.json").setContent(ByteString.copyFromUtf8(content)),
    ).build()

internal fun result(id: Long = 42, fields: List<Proto.FieldResult> = listOf(field())): Proto.Result =
    Proto.Result.newBuilder().setId(id).setOk(Proto.OkResult.newBuilder().addAllResults(fields)).build()

internal class RepositoryFixture(val initial: Submission = testSubmission()) {
    val submissions = mockk<SubmissionRepository>()
    val solutions = mockk<SolutionRepository>()
    val tasks = mockk<TaskRepository>()
    val contests = mockk<ContestRepository>()
    val tests = mockk<TestRepository>()
    val logs = mockk<LogsRepository>()
    val recordings = mockk<RecordingRepository>()
    val verdicts = mockk<VerdictRepository>()
    val validationRequests = mockk<TaskValidationRequestRepository>()
    val savedVerdicts = mutableListOf<VerdictData>()
    val savedVerdictEntities = mutableListOf<Verdict>()
    val savedLogs = mutableListOf<LogsData>()
    val savedRecordings = mutableListOf<RecordingData>()
    val current = AtomicReference(initial)
    val hasCommitted = AtomicBoolean(false)
    private val transactions = mockk<PlatformTransactionManager>()
    val persistence = GradingPersistenceService(
        submissions = submissions,
        solutions = solutions,
        tasks = tasks,
        contests = contests,
        tests = tests,
        logs = logs,
        recordings = recordings,
        verdicts = verdicts,
        validationRequests = validationRequests,
        transactionManager = transactions,
    )

    init {
        every { validationRequests.findBySubmissionId(initial.id) } returns when (val kind = initial.data.kind) {
            is SubmissionKind.DeveloperSolutionTest -> validationRequest(submitted = initial, version = kind.trikStudioVersion)
            is SubmissionKind.Grading -> null
        }
        every { transactions.getTransaction(any()) } answers {
            hasCommitted.set(false)
            SimpleTransactionStatus()
        }
        every { transactions.commit(any()) } answers { hasCommitted.set(true) }
        every { transactions.rollback(any()) } returns Unit
        every { submissions.findById(initial.id) } answers { current.get() }
        every { submissions.update(any<Submission>()) } answers { firstArg<Submission>().also(current::set) }
        every { contests.load(any<LazyEntity<ContestId, Contest>>()) } returns contest {
            id = 10
            createdAt = Instant.EPOCH
            data {
                owner(1)
                name = "Contest"
                description = ""
                trikStudioVersion("contest-version")
            }
        }
        every {
            solutions.load(match<LazyEntity<SolutionId, Solution>> { reference -> reference.id == initial.data.solution.id })
        } returns solution {
            id = 2
            createdAt = Instant.EPOCH
            data {
                file("solution.py", "program".toByteArray())
                language.python()
            }
        }
        every {
            tasks.load(match<LazyEntity<TaskId, Task>> { reference -> reference.id == initial.data.task.id })
        } returns task {
            id = 3
            createdAt = Instant.EPOCH
            data {
                owner(1)
                name = "Task"
                description = ""
                content.new { tests(listOf(4)) }
            }
        }
        every { tests.load(any<LazyEntityList<TestId, Test>>()) } returns
            listOf(
                test {
                    id = 4
                    createdAt = Instant.EPOCH
                    data {
                        name = "World"
                        description = ""
                        versionBucket = VersionBucket(UUID(0, 0))
                        file("same-name.xml", "world".toByteArray())
                    }
                },
            )
        every { logs.save(capture(savedLogs)) } answers {
            logs {
                id = 5
                createdAt = Instant.EPOCH
                data = firstArg<LogsData>()
            }
        }
        every { recordings.save(capture(savedRecordings)) } answers {
            recording {
                id = 6
                createdAt = Instant.EPOCH
                data = firstArg<RecordingData>()
            }
        }
        every { verdicts.save(capture(savedVerdicts)) } answers {
            verdict {
                id = 7L + savedVerdictEntities.size
                createdAt = Instant.EPOCH
                data = firstArg<VerdictData>()
            }.also(savedVerdictEntities::add)
        }
    }
}

internal class ManualExecutor {
    val service = mockk<ScheduledExecutorService>(relaxed = true)
    private val work = LinkedBlockingQueue<Runnable>()
    private val timers = mutableListOf<Runnable>()
    val scheduled = CopyOnWriteArrayList<ManualTimer>()

    init {
        every { service.execute(any()) } answers { work.add(firstArg()) }
        every { service.scheduleWithFixedDelay(any(), any(), any(), any()) } answers {
            timers.add(firstArg())
            mockk(relaxed = true)
        }
        every { service.schedule(any<Runnable>(), any<Long>(), any<TimeUnit>()) } answers {
            ManualTimer(firstArg()).also(scheduled::add).future
        }
    }

    fun tick() {
        timers.forEach { timer -> timer.run() }
        drain()
    }

    fun drain() {
        while (true) {
            val action = work.poll() ?: return
            action.run()
        }
    }

    fun await(condition: () -> Boolean) {
        while (!condition()) {
            val action = checkNotNull(work.poll(5, TimeUnit.SECONDS)) { "Worker did not submit expected grading work" }
            action.run()
        }
        drain()
    }

    fun runNext() = checkNotNull(work.poll(5, TimeUnit.SECONDS)) { "Worker did not submit grading work" }.run()
}

internal class ManualTimer(private val action: Runnable) {
    val future = mockk<ScheduledFuture<*>>(relaxed = true)
    val isCancelled = AtomicBoolean(false)

    init {
        every { future.cancel(any()) } answers { isCancelled.compareAndSet(false, true) }
    }

    fun fire() = action.run()
}

internal class FakeNode : NodeClient {
    val status = AtomicReference<GradingNodeStatus>(GradingNodeStatus.Available(queued = 0, capacity = 1))
    val results = mutableListOf<(Proto.Result) -> Unit>()
    val errors = mutableListOf<(Throwable) -> Unit>()
    val messages = mutableListOf<Proto.Submission>()
    val timeouts = mutableListOf<Duration>()
    val isClosed = AtomicBoolean(false)
    val cancellations = AtomicReference(0)
    val polls = AtomicReference(0)

    override fun poll(onStatus: (GradingNodeStatus) -> Unit) {
        polls.updateAndGet { value -> value + 1 }
        onStatus(status.get())
    }

    override fun grade(
        submission: Proto.Submission,
        timeout: Duration,
        onResult: (Proto.Result) -> Unit,
        onError: (Throwable) -> Unit,
    ): AutoCloseable {
        messages.add(submission)
        timeouts.add(timeout)
        results.add(onResult)
        errors.add(onError)
        return AutoCloseable { cancellations.updateAndGet { value -> value + 1 } }
    }

    override fun close() {
        isClosed.set(true)
    }
}
