package tech.testsys.domain.builder.util.chooser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import tech.testsys.domain.builder.api.*
import tech.testsys.domain.builder.task.TaskValidationRequestDataBuilder
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.task.*
import java.time.Instant

class TaskValidationExecutionChooserTests {
    @ParameterizedTest
    @MethodSource("states")
    fun `should retain every state payload and persistence version when copying a request`(data: TaskValidationRequestData) {
        val request = taskValidationRequest {
            id = 7
            createdAt = Instant.EPOCH
            version = EntityVersion(9)
            this.data = data
        }

        val copied = request.withData {}

        assertEquals(request.id, copied.id)
        assertEquals(request.createdAt, copied.createdAt)
        assertEquals(request.version, copied.version)
        assertEquals(data.execution::class, copied.data.execution::class)
        assertEquals(
            (data.execution as? TaskValidationExecution.WithDiagnostics)?.diagnostics,
            (copied.data.execution as? TaskValidationExecution.WithDiagnostics)?.diagnostics,
        )
        assertEquals(
            (data.execution as? TaskValidationExecution.WithSubmissions)?.submissions?.ids,
            (copied.data.execution as? TaskValidationExecution.WithSubmissions)?.submissions?.ids,
        )
        assertEquals(
            (data.execution as? TaskValidationExecution.TechnicalFailure)?.failure,
            (copied.data.execution as? TaskValidationExecution.TechnicalFailure)?.failure,
        )
        assertEquals(
            (data.execution as? TaskValidationExecution.StoppedByDiagnostics)?.completedAt,
            (copied.data.execution as? TaskValidationExecution.StoppedByDiagnostics)?.completedAt,
        )
    }

    @Test
    fun `should reject a diagnostic stop without its completion time`() {
        val chooser = TaskValidationExecutionChooser()
        chooser.stoppedByDiagnostics {}

        assertThrows(IllegalArgumentException::class.java) { chooser.build() }
    }

    @Test
    fun `should reject an incomplete diagnostic failure without its failure details`() {
        val chooser = TaskValidationExecutionChooser()
        chooser.incompleteDiagnosticsFailure {}

        assertThrows(IllegalArgumentException::class.java) { chooser.build() }
    }

    @Test
    fun `should reject a completed diagnostic failure without its failure details`() {
        val chooser = TaskValidationExecutionChooser()
        chooser.completedDiagnosticsFailure {}

        assertThrows(IllegalArgumentException::class.java) { chooser.build() }
    }

    @Test
    fun `should reject a submitted failure without its failure details`() {
        val chooser = TaskValidationExecutionChooser()
        chooser.createdSubmissionsFailure {}

        assertThrows(IllegalArgumentException::class.java) { chooser.build() }
    }

    companion object {
        @JvmStatic
        fun states(): List<TaskValidationRequestData> = listOf(
            data { execution.pendingDiagnostics() },
            data { execution.diagnosticsInProgress() },
            data {
                execution.awaitingSubmissions {
                    diagnostics = mutableListOf(testDiagnosticResult { testId(3) })
                }
            },
            data {
                execution.stoppedByDiagnostics {
                    diagnostics = mutableListOf(testDiagnosticResult { testId(3) })
                    completedAt = Instant.EPOCH
                }
            },
            data {
                execution.submissionsCreated {
                    diagnostics = mutableListOf(testDiagnosticResult { testId(3) })
                    submissions = mutableListOf(SubmissionId(4))
                }
            },
            data {
                execution.incompleteDiagnosticsFailure {
                    failure = taskValidationTechnicalFailure {
                        description = "Failure"
                        occurredAt = Instant.EPOCH
                    }
                }
            },
            data {
                execution.completedDiagnosticsFailure {
                    diagnostics = mutableListOf(testDiagnosticResult { testId(3) })
                    failure = taskValidationTechnicalFailure {
                        description = "Failure"
                        occurredAt = Instant.EPOCH
                    }
                }
            },
            data {
                execution.createdSubmissionsFailure {
                    diagnostics = mutableListOf(testDiagnosticResult { testId(3) })
                    submissions = mutableListOf(SubmissionId(4))
                    failure = taskValidationTechnicalFailure {
                        description = "Failure"
                        occurredAt = Instant.EPOCH
                    }
                }
            },
        )

        private fun data(builder: TaskValidationRequestDataBuilder.() -> Unit): TaskValidationRequestData = taskValidationRequestData {
            task(1)
            requestedBy(2)
            snapshot = taskValidationSnapshot { tests(listOf(3)) }
            builder()
        }
    }
}
