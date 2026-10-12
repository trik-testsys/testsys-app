@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import io.grpc.Status
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.api.submission
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.GradingAdmission
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.domain.contract.GradingNodeStatus
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.infra.grpc.api.BalancingGrader
import tech.testsys.infra.grpc.api.GradingQueueSnapshot
import tech.testsys.infra.monitoring.api.TechnicalFailures
import tech.testsys.infra.monitoring.api.TestSysMetrics
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

internal class GradingCoordinatorTests {
    private val repository = RepositoryFixture()
    private val node = FakeNode()
    private val queue = ManualExecutor()
    private val polling = ManualExecutor()
    private val sender = Executors.newSingleThreadExecutor()
    private val nextNode = AtomicReference(node)
    private val manager = GradingNodeManager(NodeClientFactory { nextNode.get() })
    private val now = AtomicReference(Instant.EPOCH)
    private val clock = mockk<Clock> {
        every { instant() } answers { now.get() }
    }
    private val meters = SimpleMeterRegistry()
    private val coordinator = GradingCoordinator(
        persistence = repository.persistence,
        nodes = manager,
        settings = settings,
        executor = queue.service,
        senderExecutor = sender,
        pollingExecutor = polling.service,
        clock = clock,
        parser = JsonLogParser(),
        failures = TechnicalFailures(meters),
        metrics = meters,
    )
    private val grader = BalancingGrader(coordinator)
    private val address = GradingNodeAddress("node")

    @AfterEach
    fun stopWorker() {
        coordinator.close()
        assertTrue(sender.awaitTermination(5, TimeUnit.SECONDS))
    }

    @Test
    fun `should reserve a submission by id before preparation and preserve its accepted snapshot`() {
        grader.sendToGrade(repository.initial)
        val changed = repository.initial.withData { kind.developerSolutionTest { trikStudioVersion("other") } }

        assertEquals(GradingAdmission.AlreadyPending, grader.sendToGrade(repository.initial))
        assertEquals(
            GradingAdmission.AlreadyPending,
            grader.sendToGrade(changed),
        )
        verify(exactly = 0) { repository.solutions.load(any<LazyEntity<SolutionId, Solution>>()) }
        coordinator.start()
        grader.addNode(address)
        polling.tick()
        queue.await { node.messages.isNotEmpty() }
        assertEquals("2025.1", node.messages.single().options.dockerImage)
        assertTrue(node.messages.single().options.recordVideo)
    }

    @Test
    fun `should save an error without a verdict and release reservation when the contest is missing`() {
        coordinator.start()
        val submitted = repository.initial.withData { kind.grading { contest(10) } }
        every { repository.contests.load(any<LazyEntity<ContestId, Contest>>()) } throws IllegalArgumentException("Missing contest 10")
        val completions = mutableListOf<Long>()
        grader.subscribeOnGraded { id -> completions.add(id.value) }

        grader.sendToGrade(submitted)
        queue.drain()

        val status = assertInstanceOf(SubmissionStatus.Graded::class.java, repository.current.get().data.status)
        val error = assertInstanceOf(GradingResult.GradingError::class.java, status.grade)
        assertTrue(error.description.contains("Missing contest 10"))
        assertEquals(listOf(42L), completions)
        assertEquals(0, repository.savedVerdicts.size)
        assertEquals(0, node.messages.size)
        assertEquals(GradingAdmission.Accepted, grader.sendToGrade(submitted))
    }

    @Test
    fun `should save a grading error and release the reservation when preparation fails`() {
        coordinator.start()
        every { repository.solutions.load(any<LazyEntity<SolutionId, Solution>>()) } throws IllegalStateException("Solution unavailable")
        val completions = mutableListOf<Long>()
        grader.subscribeOnGraded { id -> completions.add(id.value) }

        grader.sendToGrade(repository.initial)
        queue.drain()

        val status = assertInstanceOf(SubmissionStatus.Graded::class.java, repository.current.get().data.status)
        assertInstanceOf(GradingResult.GradingError::class.java, status.grade)
        assertEquals(listOf(42L), completions)
        assertEquals(0, repository.savedVerdicts.size)
        assertEquals(0, node.messages.size)
        assertTrue(queue.scheduled.single().isCancelled.get())
        assertEquals(GradingAdmission.Accepted, grader.sendToGrade(repository.initial))
    }

    @Test
    fun `should expire a submission when preparation consumes the total deadline`() {
        coordinator.start()
        grader.addNode(address)
        polling.tick()
        every { repository.submissions.findById(repository.initial.id) } answers {
            now.set(Instant.EPOCH.plusSeconds(1_800))
            repository.current.get()
        }
        val completions = mutableListOf<Long>()
        grader.subscribeOnGraded { id -> completions.add(id.value) }

        grader.sendToGrade(repository.initial)
        queue.runNext()
        queue.scheduled.single().fire()
        queue.drain()

        val status = assertInstanceOf(SubmissionStatus.Graded::class.java, repository.current.get().data.status)
        assertEquals(GradingResult.Timeout, status.grade)
        assertEquals(listOf(42L), completions)
        assertEquals(0, repository.savedVerdicts.size)
        assertEquals(0, node.messages.size)
        assertEquals(0, node.cancellations.get())
    }

    @Test
    fun `should cancel an active RPC and ignore its reply after the total deadline`() {
        startRun()
        val completions = mutableListOf<Long>()
        grader.subscribeOnGraded { id -> completions.add(id.value) }
        now.set(Instant.EPOCH.plusSeconds(1_800))

        queue.scheduled.single().fire()
        node.results.single()(result())
        queue.drain()

        val status = assertInstanceOf(SubmissionStatus.Graded::class.java, repository.current.get().data.status)
        assertEquals(GradingResult.Timeout, status.grade)
        assertEquals(listOf(42L), completions)
        assertEquals(0, repository.savedVerdicts.size)
        assertEquals(1, node.cancellations.get())
        assertEquals(GradingAdmission.Accepted, grader.sendToGrade(repository.initial))
    }

    @Test
    fun `should ignore previous RPC callbacks while a new regrade is in progress`() {
        startRun()
        val completions = mutableListOf<Long>()
        grader.subscribeOnGraded { id -> completions.add(id.value) }
        node.results[0](result())
        queue.drain()
        grader.sendToGrade(repository.initial)
        queue.await { node.messages.size == 2 }

        node.results[0](result())
        node.errors[0](Status.UNAVAILABLE.asRuntimeException())
        queue.drain()

        assertInstanceOf(SubmissionStatus.InProgress::class.java, repository.current.get().data.status)
        assertEquals(listOf(42L), completions)
        assertEquals(1, repository.savedVerdicts.size)
        assertEquals(1, node.cancellations.get())
        assertInstanceOf(GradingNodeStatus.Available::class.java, grader.getNodeStatuses()[address])
        assertEquals(GradingAdmission.AlreadyPending, grader.sendToGrade(repository.initial))
        node.results[1](result())
        queue.drain()
        assertEquals(listOf(42L, 42L), completions)
        assertEquals(2, repository.savedVerdicts.size)
    }

    @Test
    fun `should ignore a previous save retry timer while a new regrade is in progress`() {
        startRun()
        every { repository.submissions.update(any<Submission>()) } throws IllegalStateException("Storage unavailable")
        val completions = mutableListOf<Long>()
        grader.subscribeOnGraded { id -> completions.add(id.value) }
        node.errors[0](Status.INTERNAL.asRuntimeException())
        queue.drain()
        val saveRetry = queue.scheduled[1]
        every { repository.submissions.update(any<Submission>()) } answers { firstArg<Submission>().also(repository.current::set) }
        saveRetry.fire()
        grader.sendToGrade(repository.initial)
        queue.await { node.messages.size == 2 }

        saveRetry.fire()
        queue.drain()

        assertInstanceOf(SubmissionStatus.InProgress::class.java, repository.current.get().data.status)
        assertEquals(listOf(42L), completions)
        assertEquals(0, repository.savedVerdicts.size)
        assertTrue(saveRetry.isCancelled.get())
        assertEquals(GradingAdmission.AlreadyPending, grader.sendToGrade(repository.initial))
        node.results[1](result())
        queue.drain()
        assertEquals(listOf(42L, 42L), completions)
        assertEquals(1, repository.savedVerdicts.size)
    }

    @Test
    fun `should ignore late and duplicate replies after a transport retry`() {
        startRun()
        node.errors[0](Status.UNAVAILABLE.asRuntimeException())
        queue.drain()
        polling.tick()
        queue.await { node.messages.size == 2 }
        node.results[0](result())
        queue.drain()
        val completions = mutableListOf<Long>()
        grader.subscribeOnGraded { id ->
            assertTrue(repository.hasCommitted.get())
            completions.add(id.value)
        }

        node.results[1](result())
        node.results[1](result())
        queue.drain()

        assertEquals(listOf(42L), completions)
        assertEquals(1, repository.savedVerdicts.size)
        assertEquals(2, node.messages.size)
        assertEquals(node.messages[0], node.messages[1])
        verify(exactly = 1) { repository.solutions.load(any<LazyEntity<SolutionId, Solution>>()) }
        assertEquals(GradingAdmission.Accepted, grader.sendToGrade(repository.initial))
    }

    @Test
    fun `should create a new verdict after a completed successful regrade`() {
        startRun()
        node.results[0](result())
        queue.drain()
        val firstVerdict = repository.savedVerdictEntities.single()
        val firstStatus = assertInstanceOf(SubmissionStatus.Graded::class.java, repository.current.get().data.status)
        val firstGrade = assertInstanceOf(GradingResult.Success::class.java, firstStatus.grade)

        val admission = grader.sendToGrade(repository.initial)
        queue.drain()
        queue.await { node.messages.size == 2 }
        node.results[1](result(fields = listOf(field(content = """[{"level":"info","message":"Набрано баллов: 31"}]"""))))
        queue.drain()

        assertEquals(GradingAdmission.Accepted, admission)
        assertEquals(listOf(VerdictId(7), VerdictId(8)), repository.savedVerdictEntities.map { verdict -> verdict.id })
        assertSame(firstVerdict, repository.savedVerdictEntities.first())
        assertEquals(17, firstVerdict.data.testVerdicts.single().score.value)
        assertEquals(VerdictId(7), firstGrade.verdict.id)
        assertEquals(31, repository.savedVerdictEntities.last().data.testVerdicts.single().score.value)
        val status = assertInstanceOf(SubmissionStatus.Graded::class.java, repository.current.get().data.status)
        val grade = assertInstanceOf(GradingResult.Success::class.java, status.grade)
        assertEquals(VerdictId(8), grade.verdict.id)
    }

    @Test
    fun `should close a removed node and retry its unfinished check`() {
        startRun()

        grader.removeNode(address)
        queue.drain()

        assertTrue(node.isClosed.get())
        assertEquals(emptyMap<GradingNodeAddress, tech.testsys.domain.contract.GradingNodeStatus>(), grader.getNodeStatuses())
        assertEquals(1, node.cancellations.get())
        assertEquals(GradingAdmission.AlreadyPending, grader.sendToGrade(repository.initial))
        val replacement = FakeNode()
        nextNode.set(replacement)
        grader.addNode(address)
        polling.tick()
        queue.await { replacement.messages.size == 1 }
        assertEquals(node.messages.single(), replacement.messages.single())
        verify(exactly = 1) { repository.solutions.load(any<LazyEntity<SolutionId, Solution>>()) }
    }

    @Test
    fun `should poll an observed node without sending it submissions`() {
        coordinator.start()
        val observed = GradingNodeAddress("a")
        val participant = GradingNodeAddress("b")
        coordinator.observe(observed)
        val other = FakeNode()
        nextNode.set(other)
        grader.addNode(participant)
        polling.tick()

        grader.sendToGrade(repository.initial)
        queue.await { other.messages.size == 1 }

        assertEquals(1, node.polls.get())
        assertEquals(setOf(observed, participant), coordinator.observedStatuses().keys)
        assertEquals(setOf(participant), grader.getNodeStatuses().keys)
        assertEquals(emptyList<trik.testsys.grading.GradingNodeOuterClass.Submission>(), node.messages)
    }

    @Test
    fun `should keep a removed node observed without sending when its availability recovers`() {
        coordinator.start()
        val removed = GradingNodeAddress("a")
        val participant = GradingNodeAddress("b")
        grader.addNode(removed)
        val other = FakeNode()
        other.status.set(GradingNodeStatus.Available(queued = 1, capacity = 1))
        nextNode.set(other)
        grader.addNode(participant)
        polling.tick()
        grader.sendToGrade(repository.initial)
        queue.await { node.messages.size == 1 }
        node.errors[0](Status.UNAVAILABLE.asRuntimeException())
        queue.drain()
        grader.removeNode(removed)
        queue.drain()
        polling.tick()
        other.status.set(GradingNodeStatus.Available(queued = 0, capacity = 1))

        polling.tick()
        queue.await { other.messages.size == 1 }

        assertEquals(GradingNodeStatus.Available(queued = 0, capacity = 1), coordinator.observedStatuses()[removed])
        assertEquals(1, node.messages.size)
        assertEquals(node.messages.single(), other.messages.single())
    }

    @Test
    fun `should wait for a fresh poll after participation is enabled again`() {
        startRun()
        grader.removeNode(address)
        queue.drain()

        grader.addNode(address)
        queue.drain()

        assertEquals(mapOf(address to GradingNodeStatus.Unknown), grader.getNodeStatuses())
        assertEquals(1, node.messages.size)
        polling.tick()
        queue.await { node.messages.size == 2 }
    }

    @Test
    fun `should ignore a late reply of a disabled participation and save the retried result`() {
        startRun()
        grader.removeNode(address)
        queue.drain()
        grader.addNode(address)
        polling.tick()
        queue.await { node.messages.size == 2 }

        node.results[0](result())
        queue.drain()

        assertEquals(0, repository.savedVerdicts.size)
        assertInstanceOf(SubmissionStatus.InProgress::class.java, repository.current.get().data.status)
        node.results[1](result())
        queue.drain()
        assertEquals(1, repository.savedVerdicts.size)
    }

    @Test
    fun `should retry unfinished work and stop polling a forgotten node`() {
        startRun()

        coordinator.forget(address)
        queue.drain()
        polling.tick()

        assertEquals(1, node.polls.get())
        assertEquals(1, node.cancellations.get())
        assertEquals(emptyMap<GradingNodeAddress, GradingNodeStatus>(), coordinator.observedStatuses())
        assertEquals(GradingAdmission.AlreadyPending, grader.sendToGrade(repository.initial))
    }

    @Test
    fun `should read statuses without additional polls when accepting more work`() {
        startRun()

        grader.getNodeStatuses()
        grader.getNodeStatuses()
        grader.sendToGrade(repository.initial)

        assertEquals(1, node.polls.get())
    }

    @Test
    fun `should stop retrying after three transport failures`() {
        startRun()
        node.errors[0](Status.UNAVAILABLE.asRuntimeException())
        queue.drain()
        polling.tick()
        queue.await { node.messages.size == 2 }
        node.errors[1](Status.UNAVAILABLE.asRuntimeException())
        queue.drain()
        polling.tick()
        queue.await { node.messages.size == 3 }

        node.errors[2](Status.UNAVAILABLE.asRuntimeException())
        queue.drain()

        val graded = repository.current.get().data.status as? SubmissionStatus.Graded
        assertTrue(graded?.grade is GradingResult.GradingError)
        assertEquals(3, node.messages.size)
        assertEquals(0, repository.savedVerdicts.size)
    }

    @Test
    fun `should mark a node unreachable with the status code when sending fails`() {
        startRun()

        node.errors[0](Status.UNAVAILABLE.withDescription("Connection closed").asRuntimeException())
        queue.drain()

        assertEquals(GradingNodeStatus.Unreachable("UNAVAILABLE"), grader.getNodeStatuses()[address])
    }

    @Test
    fun `should keep the transport failure description in the result after the last attempt`() {
        startRun()
        node.errors[0](Status.UNAVAILABLE.asRuntimeException())
        queue.drain()
        polling.tick()
        queue.await { node.messages.size == 2 }
        node.errors[1](Status.UNAVAILABLE.asRuntimeException())
        queue.drain()
        polling.tick()
        queue.await { node.messages.size == 3 }

        node.errors[2](Status.UNAVAILABLE.asRuntimeException())
        queue.drain()

        val graded = assertInstanceOf(SubmissionStatus.Graded::class.java, repository.current.get().data.status)
        assertEquals(GradingResult.GradingError("Checking node unavailable: UNAVAILABLE"), graded.grade)
    }

    @Test
    @Tag("regression")
    fun `should retry on another available node before the next status poll`() {
        coordinator.start()
        val first = GradingNodeAddress("a")
        val second = GradingNodeAddress("b")
        grader.addNode(first)
        val other = FakeNode()
        nextNode.set(other)
        grader.addNode(second)
        polling.tick()
        grader.sendToGrade(repository.initial)
        queue.await { node.messages.size == 1 }

        node.errors[0](Status.UNAVAILABLE.asRuntimeException())
        queue.await { other.messages.size == 1 }

        assertInstanceOf(GradingNodeStatus.Unreachable::class.java, grader.getNodeStatuses()[first])
        assertEquals(1, node.messages.size)
        assertEquals(node.messages.single(), other.messages.single())
        assertEquals(1, node.polls.get())
        assertEquals(1, other.polls.get())
        other.results.single()(result())
        queue.drain()
        assertEquals(1, repository.savedVerdicts.size)
    }

    @Test
    @Tag("regression")
    fun `should wait for a fresh status poll before retrying the only unavailable node`() {
        startRun()

        node.errors[0](Status.UNAVAILABLE.asRuntimeException())
        queue.drain()

        assertInstanceOf(GradingNodeStatus.Unreachable::class.java, grader.getNodeStatuses()[address])
        assertEquals(1, node.messages.size)
        assertEquals(GradingAdmission.AlreadyPending, grader.sendToGrade(repository.initial))
        polling.tick()
        queue.await { node.messages.size == 2 }
        assertInstanceOf(GradingNodeStatus.Available::class.java, grader.getNodeStatuses()[address])
        assertEquals(node.messages[0], node.messages[1])
    }

    @Test
    @Tag("regression")
    fun `should expire the retry when the only unavailable node does not recover`() {
        startRun()
        node.errors[0](Status.UNAVAILABLE.asRuntimeException())
        queue.drain()
        now.set(Instant.EPOCH.plusSeconds(1_800))

        queue.scheduled.single().fire()

        val status = assertInstanceOf(SubmissionStatus.Graded::class.java, repository.current.get().data.status)
        assertEquals(GradingResult.Timeout, status.grade)
        assertEquals(1, node.messages.size)
        assertEquals(0, repository.savedVerdicts.size)
    }

    @Test
    fun `should preserve a terminal result while storage is unavailable`() {
        startRun()
        every { repository.submissions.update(any<Submission>()) } throws IllegalStateException("Storage unavailable")
        val completions = mutableListOf<Long>()
        grader.subscribeOnGraded { id -> completions.add(id.value) }

        node.results[0](result())
        queue.drain()

        assertEquals(emptyList<Long>(), completions)
        assertEquals(GradingAdmission.AlreadyPending, grader.sendToGrade(repository.initial))
        assertEquals(1, node.messages.size)
    }

    @Test
    fun `should save the accepted result after storage recovers beyond the total deadline`() {
        startRun()
        every { repository.submissions.update(any<Submission>()) } throws IllegalStateException("Storage unavailable")
        val completions = mutableListOf<Long>()
        grader.subscribeOnGraded { id -> completions.add(id.value) }
        node.results[0](result())
        queue.drain()
        now.set(Instant.EPOCH.plusSeconds(1_801))
        queue.scheduled[0].fire()
        every { repository.submissions.update(any<Submission>()) } answers { firstArg<Submission>().also(repository.current::set) }

        queue.scheduled[1].fire()

        assertTrue((repository.current.get().data.status as? SubmissionStatus.Graded)?.grade is GradingResult.Success)
        assertEquals(listOf(42L), completions)
        assertEquals(1, node.messages.size)
        assertTrue(queue.scheduled.all { timer -> timer.isCancelled.get() })
        assertEquals(GradingAdmission.Accepted, grader.sendToGrade(repository.initial))
    }

    @Test
    fun `should wait for cached capacity before sending a prepared submission`() {
        coordinator.start()
        grader.addNode(address)
        node.status.set(GradingNodeStatus.Available(queued = 1, capacity = 1))
        polling.tick()
        grader.sendToGrade(repository.initial)
        queue.runNext()
        queue.runNext()
        assertEquals(emptyList<trik.testsys.grading.GradingNodeOuterClass.Submission>(), node.messages)

        node.status.set(GradingNodeStatus.Available(queued = 0, capacity = 1))
        polling.tick()
        queue.await { node.messages.size == 1 }

        assertEquals(42L, node.messages.single().id)
        verify(exactly = 1) { repository.solutions.load(any<LazyEntity<SolutionId, Solution>>()) }
    }

    @Test
    fun `should preserve acceptance order when prepared submissions wait for a free node`() {
        coordinator.start()
        grader.addNode(address)
        node.status.set(GradingNodeStatus.Available(queued = 2, capacity = 2))
        polling.tick()
        val second = submission {
            id = 41
            createdAt = Instant.EPOCH
            data = repository.initial.data
        }
        every { repository.submissions.findById(SubmissionId(41)) } returns second
        every { repository.validationRequests.findBySubmissionId(second.id) } returns validationRequest(submitted = second)
        grader.sendToGrade(repository.initial)
        queue.runNext()
        queue.runNext()
        grader.sendToGrade(second)
        queue.drain()

        node.status.set(GradingNodeStatus.Available(queued = 0, capacity = 2))
        polling.tick()
        queue.await { node.messages.size == 2 }

        assertEquals(listOf(42L, 41L), node.messages.map { message -> message.id })
    }

    @Test
    fun `should ignore the previous deadline when a completed submission is regraded`() {
        startRun()
        node.results[0](result())
        queue.drain()
        now.set(Instant.EPOCH.plusSeconds(1_000))
        grader.sendToGrade(repository.initial)
        queue.await { node.messages.size == 2 }
        now.set(Instant.EPOCH.plusSeconds(1_800))

        queue.scheduled[0].fire()

        assertEquals(GradingAdmission.AlreadyPending, grader.sendToGrade(repository.initial))
        assertEquals(1, repository.savedVerdicts.size)
        node.results[1](result())
        queue.drain()
        assertEquals(listOf(VerdictId(7), VerdictId(8)), repository.savedVerdictEntities.map { verdict -> verdict.id })
    }

    @Test
    fun `should limit the RPC deadline by the remaining total time`() {
        coordinator.start()
        grader.addNode(address)
        polling.tick()
        grader.sendToGrade(repository.initial)
        queue.runNext()
        now.set(Instant.EPOCH.plusSeconds(1_799))

        queue.await { node.messages.size == 1 }

        assertEquals(listOf(Duration.ofSeconds(1)), node.timeouts)
    }

    @Test
    fun `should continue sending when updating the submission throws IOException`() {
        coordinator.start()
        grader.addNode(address)
        polling.tick()
        grader.sendToGrade(repository.initial)
        queue.runNext()
        every { repository.submissions.update(any<Submission>()) } throws IOException("Storage unavailable")
        queue.runNext()
        assertEquals(emptyList<trik.testsys.grading.GradingNodeOuterClass.Submission>(), node.messages)
        every { repository.submissions.update(any<Submission>()) } answers { firstArg<Submission>().also(repository.current::set) }

        polling.tick()
        queue.await { node.messages.size == 1 }

        assertEquals(42L, node.messages.single().id)
        assertEquals(GradingAdmission.AlreadyPending, grader.sendToGrade(repository.initial))
    }

    @Test
    fun `should stop the sender when its queue is empty`() {
        coordinator.start()

        coordinator.close()

        assertTrue(sender.awaitTermination(5, TimeUnit.SECONDS))
        verify(exactly = 0) { queue.service.execute(any()) }
        verify(exactly = 0) { queue.service.scheduleWithFixedDelay(any(), any(), any(), any()) }
    }

    @Test
    fun `should close channels and executors at shutdown`() {
        startRun()

        coordinator.close()

        assertTrue(node.isClosed.get())
        verify(exactly = 1) { queue.service.shutdownNow() }
        verify(exactly = 1) { polling.service.shutdownNow() }
        assertEquals(1, node.cancellations.get())
    }

    @Test
    fun `should expire work that remains queued beyond the total deadline`() {
        coordinator.start()
        grader.sendToGrade(repository.initial)
        queue.drain()
        now.set(Instant.EPOCH.plusSeconds(1_800))

        queue.scheduled.single().fire()

        assertEquals(GradingResult.Timeout, (repository.current.get().data.status as? SubmissionStatus.Graded)?.grade)
        assertEquals(emptyList<trik.testsys.grading.GradingNodeOuterClass.Submission>(), node.messages)
    }

    @Test
    @Tag("regression")
    fun `should continue processing and notify other subscribers when one throws IOException`() {
        coordinator.start()
        val completions = mutableListOf<Long>()
        grader.subscribeOnGraded { throw IOException("Subscriber failed") }
        grader.subscribeOnGraded { id ->
            assertTrue(repository.hasCommitted.get())
            assertEquals(GradingResult.Timeout, (repository.current.get().data.status as? SubmissionStatus.Graded)?.grade)
            completions.add(id.value)
        }
        grader.sendToGrade(repository.initial)
        queue.drain()
        now.set(Instant.EPOCH.plusSeconds(1_800))

        assertDoesNotThrow { queue.scheduled[0].fire() }

        assertEquals(listOf(42L), completions)
        assertEquals(GradingAdmission.Accepted, grader.sendToGrade(repository.initial))
        queue.drain()
        now.set(Instant.EPOCH.plusSeconds(3_600))
        assertDoesNotThrow { queue.scheduled[1].fire() }
        assertEquals(listOf(42L, 42L), completions)
    }

    @Nested
    inner class QueueTests {
        @Test
        fun `should report no unfinished runs before any submission`() {
            val snapshot = coordinator.queue()

            assertEquals(GradingQueueSnapshot(preparing = 0, waiting = 0, grading = 0, saving = 0), snapshot)
            assertEquals(0, snapshot.total)
        }

        @Test
        fun `should count an accepted run as preparing until its data is loaded`() {
            coordinator.start()
            coordinator.submit(repository.initial)

            val snapshot = coordinator.queue()

            assertEquals(GradingQueueSnapshot(preparing = 1, waiting = 0, grading = 0, saving = 0), snapshot)
            assertEquals(1, snapshot.total)
        }

        @Test
        fun `should count a prepared run as waiting while no node is available`() {
            coordinator.start()
            coordinator.submit(repository.initial)
            queue.drain()

            val snapshot = coordinator.queue()

            assertEquals(GradingQueueSnapshot(preparing = 0, waiting = 1, grading = 0, saving = 0), snapshot)
        }

        @Test
        fun `should count a sent run as grading`() {
            startRun()

            val snapshot = coordinator.queue()

            assertEquals(GradingQueueSnapshot(preparing = 0, waiting = 0, grading = 1, saving = 0), snapshot)
        }

        @Test
        fun `should move a run back to waiting after a transport retry`() {
            startRun()
            node.errors[0](Status.UNAVAILABLE.asRuntimeException())
            queue.drain()

            val snapshot = coordinator.queue()

            assertEquals(GradingQueueSnapshot(preparing = 0, waiting = 1, grading = 0, saving = 0), snapshot)
            assertEquals(1, snapshot.total)
        }

        @Test
        fun `should count an accepted result as saving while storage is unavailable`() {
            startRun()
            every { repository.submissions.update(any<Submission>()) } throws IllegalStateException("Storage unavailable")
            node.results[0](result())
            queue.drain()

            val snapshot = coordinator.queue()

            assertEquals(GradingQueueSnapshot(preparing = 0, waiting = 0, grading = 0, saving = 1), snapshot)
            assertEquals(1, snapshot.total)
        }

        @Test
        fun `should report no unfinished runs after a delayed save succeeds`() {
            startRun()
            every { repository.submissions.update(any<Submission>()) } throws IllegalStateException("Storage unavailable")
            node.results[0](result())
            queue.drain()
            every { repository.submissions.update(any<Submission>()) } answers { firstArg<Submission>().also(repository.current::set) }

            queue.scheduled[1].fire()

            assertEquals(GradingQueueSnapshot(preparing = 0, waiting = 0, grading = 0, saving = 0), coordinator.queue())
        }

        @Test
        fun `should count each submission once in its own state`() {
            coordinator.start()
            coordinator.addNode(address)
            node.status.set(GradingNodeStatus.Available(queued = 0, capacity = 1))
            polling.tick()
            coordinator.submit(repository.initial)
            queue.await { node.messages.size == 1 }
            val second = testSubmission(id = 41)
            every { repository.submissions.findById(second.id) } returns second
            every { repository.validationRequests.findBySubmissionId(second.id) } returns validationRequest(submitted = second)
            coordinator.submit(second)
            queue.drain()
            val third = testSubmission(id = 40)
            coordinator.submit(third)

            val snapshot = coordinator.queue()

            assertEquals(GradingQueueSnapshot(preparing = 1, waiting = 1, grading = 1, saving = 0), snapshot)
            assertEquals(3, snapshot.total)
        }
    }

    @Nested
    inner class MetricsTests {
        @Test
        fun `should count a preparation failure and the finished failure`() {
            coordinator.start()
            every { repository.solutions.load(any<LazyEntity<SolutionId, Solution>>()) } throws IllegalStateException("Unavailable")

            grader.sendToGrade(repository.initial)
            queue.drain()

            assertEquals(1.0, failures("prepare", "IllegalStateException"))
            assertEquals(1.0, checks("failure"))
        }

        @Test
        fun `should count a created verdict without technical failures`() {
            startRun()

            node.results[0](result())
            queue.drain()

            assertEquals(1.0, checks("verdict"))
            assertEquals(0.0, allFailures())
        }

        @Test
        fun `should count each unavailable node response`() {
            startRun()

            node.errors[0](Status.UNAVAILABLE.asRuntimeException())
            queue.drain()

            assertEquals(1.0, failures("node_unavailable", "StatusRuntimeException"))
        }

        @Test
        fun `should count a node deadline before the total deadline`() {
            startRun()

            node.errors[0](Status.DEADLINE_EXCEEDED.asRuntimeException())
            queue.drain()

            assertEquals(1.0, failures("node_deadline", "StatusRuntimeException"))
        }

        @Test
        fun `should not count a node deadline once the total deadline is exhausted`() {
            startRun()
            now.set(Instant.EPOCH.plusSeconds(1_800))

            node.errors[0](Status.DEADLINE_EXCEEDED.asRuntimeException())
            queue.drain()

            assertEquals(0.0, allFailures())
            assertEquals(1.0, checks("timeout"))
        }

        @Test
        fun `should count a failed grading RPC as a node error`() {
            startRun()

            node.errors[0](Status.INTERNAL.asRuntimeException())
            queue.drain()

            assertEquals(1.0, failures("node_error", "StatusRuntimeException"))
            assertEquals(1.0, checks("failure"))
        }

        @Test
        fun `should count a rejected node result without an exception`() {
            startRun()

            node.results[0](result(id = 7))
            queue.drain()

            assertEquals(1.0, failures("invalid_result", "none"))
            assertEquals(1.0, checks("failure"))
        }

        @Test
        fun `should count every failed save attempt`() {
            startRun()
            every { repository.submissions.update(any<Submission>()) } answers { throw IllegalStateException("Storage unavailable") }
            node.results[0](result())
            queue.drain()

            queue.scheduled[1].fire()

            assertEquals(2.0, failures("save", "IllegalStateException"))
            assertEquals(0.0, checks("verdict"))
        }

        @Test
        fun `should count a failed sending step`() {
            coordinator.start()
            grader.addNode(address)
            polling.tick()
            grader.sendToGrade(repository.initial)
            queue.runNext()
            every { repository.submissions.update(any<Submission>()) } throws IOException("Storage unavailable")

            queue.runNext()

            assertEquals(1.0, failures("send", "IOException"))
        }

        @Test
        fun `should count a failed subscriber`() {
            startRun()
            grader.subscribeOnGraded { throw IOException("Subscriber failed") }

            node.results[0](result())
            queue.drain()

            assertEquals(1.0, failures("subscriber", "IOException"))
        }

        @Test
        fun `should not count a status poll that reports an unreachable node`() {
            coordinator.start()
            grader.addNode(address)
            node.status.set(GradingNodeStatus.Unreachable("UNAVAILABLE"))

            polling.tick()

            assertEquals(mapOf(address to GradingNodeStatus.Unreachable("UNAVAILABLE")), coordinator.statuses())
            assertEquals(0.0, allFailures())
        }

        @Test
        fun `should not count a status poll that throws`() {
            coordinator.start()
            grader.addNode(address)
            node.pollFailure.set(Status.UNAVAILABLE.asRuntimeException())

            polling.tick()

            assertEquals(mapOf(address to GradingNodeStatus.Unreachable("UNAVAILABLE")), coordinator.statuses())
            assertEquals(0.0, allFailures())
        }

        @Test
        fun `should count a failed grading work step`() {
            coordinator.start()
            val timerFailure = IllegalStateException("Timer unavailable")
            every { queue.service.schedule(any<Runnable>(), any<Long>(), any<TimeUnit>()) } throws timerFailure
            grader.sendToGrade(repository.initial)

            queue.drain()

            assertEquals(1.0, failures("work", "IllegalStateException"))
        }

        @Test
        fun `should count grading work rejected while the coordinator is open`() {
            coordinator.start()
            every { queue.service.execute(any()) } throws RejectedExecutionException("Queue is full")

            grader.sendToGrade(repository.initial)

            assertEquals(1.0, failures("rejected", "RejectedExecutionException"))
        }

        @Test
        fun `should not count a node removed during a check`() {
            startRun()
            grader.removeNode(address)

            queue.drain()

            assertEquals(0.0, allFailures())
            assertEquals(GradingQueueSnapshot(preparing = 0, waiting = 1, grading = 0, saving = 0), coordinator.queue())
        }

        @Test
        fun `should not count a result that arrives after its node was removed`() {
            startRun()
            node.results[0](result())
            grader.removeNode(address)

            queue.drain()

            assertEquals(0.0, allFailures())
            assertEquals(0.0, checks("verdict"))
        }

        @Test
        fun `should not count a failed grading RPC of a removed node`() {
            startRun()
            node.errors[0](Status.INTERNAL.asRuntimeException())
            grader.removeNode(address)

            queue.drain()

            assertEquals(0.0, allFailures())
        }

        private fun failures(stage: String, exception: String): Double = meters.find(TestSysMetrics.TECHNICAL_FAILURES)
            .tags(TestSysMetrics.SOURCE, "grading", TestSysMetrics.STAGE, stage, TestSysMetrics.EXCEPTION, exception)
            .counter()?.count() ?: 0.0

        private fun allFailures(): Double = meters.find(TestSysMetrics.TECHNICAL_FAILURES).counters().sumOf { counter -> counter.count() }

        private fun checks(result: String): Double = meters.find(TestSysMetrics.GRADING_CHECKS)
            .tag(TestSysMetrics.RESULT, result)
            .counter()?.count() ?: 0.0
    }

    private fun startRun() {
        coordinator.start()
        grader.addNode(address)
        polling.tick()
        grader.sendToGrade(repository.initial)
        queue.drain()
        queue.await { node.messages.size == 1 }
    }
}
