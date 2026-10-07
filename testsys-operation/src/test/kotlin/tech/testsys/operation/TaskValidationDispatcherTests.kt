package tech.testsys.operation

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.api.taskValidationRequest
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.Grader
import tech.testsys.domain.contract.persistence.repository.TaskValidationRequestRepository
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskValidationRequest
import tech.testsys.domain.model.task.TaskValidationTechnicalFailure
import tech.testsys.operation.util.testTaskValidationRequest
import java.time.Instant
import java.util.concurrent.Executor

class TaskValidationDispatcherTests {
    private val requests = mockk<TaskValidationRequestRepository>()
    private val operations = mockk<TaskValidationOperations>()
    private val grader = mockk<Grader>()
    private val queued = ArrayDeque<Runnable>()
    private val dispatcher = TaskValidationDispatcher(
        requests = requests,
        operations = operations,
        grader = grader,
        executor = Executor { task -> queued.addLast(task) },
    )
    private val request = testTaskValidationRequest()

    @Nested
    inner class ScheduleTests {
        @Test
        fun `should process a scheduled request on the executor`() {
            every { operations.proceed(request.id) } returns request

            dispatcher.schedule(request.id)
            runQueued()

            verify(exactly = 1) { operations.proceed(request.id) }
        }

        @Test
        fun `should process a request once if it is scheduled again before processing`() {
            every { operations.proceed(request.id) } returns request

            dispatcher.schedule(request.id)
            dispatcher.schedule(request.id)
            runQueued()

            verify(exactly = 1) { operations.proceed(request.id) }
        }

        @Test
        fun `should process a request again if it is scheduled while being processed`() {
            var calls = 0
            every { operations.proceed(request.id) } answers {
                if (++calls == 1) dispatcher.schedule(request.id)
                request
            }

            dispatcher.schedule(request.id)
            runQueued()

            verify(exactly = 2) { operations.proceed(request.id) }
        }

        @Test
        fun `should record a technical failure if processing throws`() {
            val failure = slot<TaskValidationTechnicalFailure>()
            every { operations.proceed(request.id) } throws IllegalStateException("Grader unavailable")
            every { requests.recordTechnicalFailure(request.id, capture(failure)) } returns request

            dispatcher.schedule(request.id)
            runQueued()

            assertEquals("java.lang.IllegalStateException: Grader unavailable", failure.captured.description)
        }
    }

    @Nested
    inner class StartTests {
        private val onGraded = slot<(SubmissionId) -> Unit>()

        @Test
        fun `should resend unfinished submissions of every active request before processing it`() {
            val other = taskValidationRequest {
                id = 12
                createdAt = Instant.EPOCH
                data = request.data
            }
            subscribe(active = listOf(request, other))
            every { operations.resendUnfinishedSubmissions(any()) } returns request
            every { operations.proceed(any()) } returns request

            dispatcher.start()
            runQueued()

            verifyOrder {
                operations.resendUnfinishedSubmissions(request.id)
                operations.resendUnfinishedSubmissions(other.id)
                operations.proceed(request.id)
                operations.proceed(other.id)
            }
        }

        @Test
        fun `should record a technical failure and still process the request if resending throws`() {
            subscribe(active = listOf(request))
            every { operations.resendUnfinishedSubmissions(request.id) } throws IllegalStateException("No nodes")
            every { requests.recordTechnicalFailure(request.id, any()) } returns request
            every { operations.proceed(request.id) } returns request

            dispatcher.start()
            runQueued()

            verify(exactly = 1) { requests.recordTechnicalFailure(request.id, any()) }
            verify(exactly = 1) { operations.proceed(request.id) }
        }

        @Test
        fun `should process the active request linked to a graded submission`() {
            subscribe(active = emptyList())
            every { requests.findBySubmissionId(SubmissionId(101)) } returns request
            every { operations.proceed(request.id) } returns request
            dispatcher.start()
            runQueued()

            onGraded.captured(SubmissionId(101))
            runQueued()

            verify(exactly = 1) { operations.proceed(request.id) }
        }

        @Test
        fun `should ignore a graded submission without an active validation request`() {
            val completed = request.withData {
                execution.completed { completedAt = Instant.EPOCH }
            }
            subscribe(active = emptyList())
            every { requests.findBySubmissionId(SubmissionId(101)) } returns null
            every { requests.findBySubmissionId(SubmissionId(102)) } returns completed
            dispatcher.start()
            runQueued()

            onGraded.captured(SubmissionId(101))
            onGraded.captured(SubmissionId(102))
            runQueued()

            verify(exactly = 0) { operations.proceed(any()) }
        }

        private fun subscribe(active: List<TaskValidationRequest>) {
            every { grader.subscribeOnGraded(capture(onGraded)) } just Runs
            every { requests.findActive() } returns active
        }
    }

    private fun runQueued() {
        while (queued.isNotEmpty()) queued.removeFirst().run()
    }
}
