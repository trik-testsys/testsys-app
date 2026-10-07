package tech.testsys.operation

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.api.*
import tech.testsys.domain.contract.PolygonDiagnostics
import tech.testsys.domain.contract.persistence.repository.TaskValidationRequestRepository
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.model.task.*
import tech.testsys.operation.util.testTaskValidationRequest
import java.time.Instant
import java.util.UUID

class TaskValidationOperationsTests {
    private val requests = mockk<TaskValidationRequestRepository>()
    private val tests = mockk<TestRepository>()
    private val diagnostics = mockk<PolygonDiagnostics>()
    private val operations = TaskValidationOperations(requests = requests, tests = tests, diagnostics = diagnostics)
    private val requestId = TaskValidationRequestId(11)

    @Test
    fun `should skip a terminal or missing caller-selected request`() {
        every { requests.startDiagnostics(requestId) } returns null

        val result = operations.processDiagnostics(requestId)

        assertNull(result)
        verify(exactly = 0) { diagnostics.diagnose(any()) }
    }

    @Test
    fun `should complete an empty polygon snapshot without invoking the adapter`() {
        val request = testTaskValidationRequest { execution.diagnosticsInProgress() }
        val complete = request.withData {
            execution.awaitingSubmissions {}
        }
        every { requests.startDiagnostics(requestId) } returns request
        every { requests.findDiagnosticProgress(requestId) } returns emptyList()
        every { requests.completeDiagnostics(requestId) } returns complete

        val result = operations.processDiagnostics(requestId)

        assertSame(complete, result)
        verify(exactly = 0) { diagnostics.diagnose(any()) }
    }

    @Test
    fun `should resume saved progress and diagnose all remaining polygons despite Error`() {
        val request = testTaskValidationRequest {
            snapshot = taskValidationSnapshot { tests(listOf(1, 2, 3)) }
            execution.diagnosticsInProgress()
        }
        val saved = testDiagnosticResult { testId(1) }
        val failed = testDiagnosticResult {
            testId(2)
            reports += diagnosticReport {
                severity = DiagnosticSeverity.Error
                data.missingTimeLimit()
            }
        }
        val clean = testDiagnosticResult { testId(3) }
        val second = polygon(2)
        val third = polygon(3)
        val complete = request.withData {
            execution.stoppedByDiagnostics {
                diagnostics = mutableListOf(saved, failed, clean)
                completedAt = Instant.EPOCH
            }
        }
        every { requests.startDiagnostics(requestId) } returns request
        every { requests.findDiagnosticProgress(requestId) } returns listOf(saved)
        every { tests.findById(TestId(2)) } returns second
        every { tests.findById(TestId(3)) } returns third
        every { diagnostics.diagnose(second) } returns failed
        every { diagnostics.diagnose(third) } returns clean
        every { requests.saveDiagnosticProgress(requestId, failed) } returns failed
        every { requests.saveDiagnosticProgress(requestId, clean) } returns clean
        every { requests.completeDiagnostics(requestId) } returns complete

        val result = operations.processDiagnostics(requestId)

        assertSame(complete, result)
        verify(exactly = 0) { tests.findById(TestId(1)) }
        verify(exactly = 1) { requests.saveDiagnosticProgress(requestId, failed) }
        verify(exactly = 1) { requests.saveDiagnosticProgress(requestId, clean) }
    }

    @Test
    fun `should propagate adapter exceptions without completing diagnostics or converting the failure`() {
        val request = testTaskValidationRequest {
            snapshot = taskValidationSnapshot { tests(listOf(2)) }
            execution.diagnosticsInProgress()
        }
        val polygon = polygon(2)
        val failure = IllegalStateException("Storage unavailable")
        every { requests.startDiagnostics(requestId) } returns request
        every { requests.findDiagnosticProgress(requestId) } returns emptyList()
        every { tests.findById(TestId(2)) } returns polygon
        every { diagnostics.diagnose(polygon) } throws failure

        val actual = assertThrows(IllegalStateException::class.java) { operations.processDiagnostics(requestId) }

        assertSame(failure, actual)
        verify(exactly = 0) { requests.completeDiagnostics(any()) }
        verify(exactly = 0) { requests.recordTechnicalFailure(any(), any()) }
    }

    @Test
    fun `should reject a result for another polygon before persisting it`() {
        val request = testTaskValidationRequest {
            snapshot = taskValidationSnapshot { tests(listOf(2)) }
            execution.diagnosticsInProgress()
        }
        val polygon = polygon(2)
        every { requests.startDiagnostics(requestId) } returns request
        every { requests.findDiagnosticProgress(requestId) } returns emptyList()
        every { tests.findById(TestId(2)) } returns polygon
        every { diagnostics.diagnose(polygon) } returns testDiagnosticResult { testId(99) }

        assertThrows(IllegalStateException::class.java) { operations.processDiagnostics(requestId) }

        verify(exactly = 0) { requests.saveDiagnosticProgress(any(), any()) }
    }

    private fun polygon(identifier: Long): tech.testsys.domain.model.task.Test = test {
        id = identifier
        createdAt = Instant.EPOCH
        data = testData {
            name = "Polygon"
            description = ""
            versionBucket = VersionBucket(UUID(0, identifier))
            file("world.xml", "<root/>".toByteArray())
        }
    }
}
