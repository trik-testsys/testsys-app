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
    private val reported = mutableListOf<Exception>()
    private val dispatcher = TaskValidationDispatcher(
        requests = requests,
        operations = operations,
        grader = grader,
        executor = Executor { task -> queued.addLast(task) },
        onTechnicalFailure = { exception -> reported.add(exception) },
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

        @Test
        fun `should report the exception of a recorded technical failure`() {
            val exception = IllegalStateException("Grader unavailable")
            every { operations.proceed(request.id) } throws exception
            every { requests.recordTechnicalFailure(request.id, any()) } returns request

            dispatcher.schedule(request.id)
            runQueued()

            assertEquals(listOf(exception), reported)
        }

        @Test
        fun `should report the processing and the recording exceptions if recording the technical failure throws`() {
            val processing = IllegalStateException("Grader unavailable")
            val recording = IllegalStateException("Database unavailable")
            every { operations.proceed(request.id) } throws processing
            every { requests.recordTechnicalFailure(request.id, any()) } throws recording

            dispatcher.schedule(request.id)
            runQueued()

            assertEquals(listOf(processing, recording), reported)
        }

        @Test
        fun `should process a later request after recording a technical failure throws`() {
            val other = taskValidationRequest {
                id = 12
                createdAt = Instant.EPOCH
                data = request.data
            }
            every { operations.proceed(request.id) } throws IllegalStateException("Grader unavailable")
            every { requests.recordTechnicalFailure(request.id, any()) } throws IllegalStateException("Database unavailable")
            every { operations.proceed(other.id) } returns other

            dispatcher.schedule(request.id)
            dispatcher.schedule(other.id)
            runQueued()

            verify(exactly = 1) { operations.proceed(other.id) }
        }

        @Test
        fun `should report nothing if processing succeeds`() {
            every { operations.proceed(request.id) } returns request

            dispatcher.schedule(request.id)
            runQueued()

            assertEquals(emptyList<Exception>(), reported)
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
        fun `should resend and process every active request if recording the failure of an earlier one throws`() {
            val other = taskValidationRequest {
                id = 12
                createdAt = Instant.EPOCH
                data = request.data
            }
            subscribe(active = listOf(request, other))
            every { operations.resendUnfinishedSubmissions(request.id) } throws IllegalStateException("No nodes")
            every { requests.recordTechnicalFailure(request.id, any()) } throws IllegalStateException("Database unavailable")
            every { operations.resendUnfinishedSubmissions(other.id) } returns other
            every { operations.proceed(any()) } returns request

            dispatcher.start()
            runQueued()

            verify(exactly = 1) { operations.resendUnfinishedSubmissions(other.id) }
            verify(exactly = 1) { operations.proceed(request.id) }
            verify(exactly = 1) { operations.proceed(other.id) }
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

        @Test
        fun `should report the exception if the request of a graded submission cannot be loaded`() {
            val exception = IllegalStateException("Database unavailable")
            subscribe(active = emptyList())
            every { requests.findBySubmissionId(SubmissionId(101)) } throws exception
            dispatcher.start()
            runQueued()

            onGraded.captured(SubmissionId(101))
            runQueued()

            assertEquals(listOf(exception), reported)
        }

        @Test
        fun `should report the exception if active requests cannot be loaded`() {
            val exception = IllegalStateException("Database unavailable")
            every { grader.subscribeOnGraded(capture(onGraded)) } just Runs
            every { requests.findActive() } throws exception

            dispatcher.start()
            runQueued()

            assertEquals(listOf(exception), reported)
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
