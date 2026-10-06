package tech.testsys.infra.database.internal.mapping.task

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import tech.testsys.domain.builder.api.*
import tech.testsys.domain.builder.task.TaskValidationRequestDataBuilder
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.task.*
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.TaskValidationExecutionJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.task.TaskValidationRequestJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMappingTests
import java.time.Instant

@OptIn(InternalDatabaseApi::class)
class TaskValidationRequestMappingTests : EntityMappingTests<TaskValidationRequestMapping>() {
    override val mapping = TaskValidationRequestMapping

    @Test
    fun `should preserve fixed identifiers creation time and persistence version when mapping updates`() {
        val current = mapping.toJpaEntity(
            taskValidationRequestData {
                task(1)
                requestedBy(2)
                snapshot = taskValidationSnapshot {}
                execution.pendingDiagnostics()
            },
        )
        current.createdAt = Instant.EPOCH
        val entity = taskValidationRequest {
            id = 5
            createdAt = Instant.ofEpochSecond(7)
            version = EntityVersion(9)
            data = taskValidationRequestData {
                task(99)
                requestedBy(88)
                snapshot = taskValidationSnapshot {}
                execution.submissionsCreated {}
            }
        }

        val row = mapping.toJpaEntity(entity = entity, current = current)

        assertEquals(1, row.taskId)
        assertEquals(2, row.requestedById)
        assertEquals(Instant.EPOCH, row.createdAt)
        assertEquals(9, row.version)
        assertTrue(row.areDiagnosticsComplete)
        assertTrue(row.areSubmissionsCreated)
    }

    @ParameterizedTest
    @MethodSource("states")
    fun `should restore every state with its required stage payload`(source: TaskValidationRequestData) {
        val row = mapping.toJpaEntity(source)

        val restored = taskValidationRequestData {
            task(1)
            requestedBy(2)
            snapshot = source.snapshot
            mapping.decodeExecution(
                row = row,
                diagnostics = (source.execution as? TaskValidationExecution.WithDiagnostics)?.diagnostics.orEmpty(),
                submissionIds = (source.execution as? TaskValidationExecution.WithSubmissions)?.submissions?.ids.orEmpty(),
                builder = this,
            )
        }

        assertEquals(source.execution::class, restored.execution::class)
        assertEquals(
            (source.execution as? TaskValidationExecution.WithDiagnostics)?.diagnostics,
            (restored.execution as? TaskValidationExecution.WithDiagnostics)?.diagnostics,
        )
        assertEquals(
            (source.execution as? TaskValidationExecution.WithSubmissions)?.submissions?.ids,
            (restored.execution as? TaskValidationExecution.WithSubmissions)?.submissions?.ids,
        )
        assertEquals(
            (source.execution as? TaskValidationExecution.TechnicalFailure)?.failure,
            (restored.execution as? TaskValidationExecution.TechnicalFailure)?.failure,
        )
        assertEquals(
            (source.execution as? TaskValidationExecution.StoppedByDiagnostics)?.completedAt,
            (restored.execution as? TaskValidationExecution.StoppedByDiagnostics)?.completedAt,
        )
    }

    @ParameterizedTest
    @MethodSource("invalidRows")
    fun `should reject stored states with contradictory flags or missing terminal data`(row: TaskValidationRequestJpaEntity) {
        assertThrows(IllegalStateException::class.java) {
            taskValidationRequestData {
                task(1)
                requestedBy(2)
                snapshot = taskValidationSnapshot {}
                mapping.decodeExecution(
                    row = row,
                    diagnostics = emptyList(),
                    submissionIds = emptyList(),
                    builder = this,
                )
            }
        }
    }

    @Test
    fun `should reject stored submission links before the submission stage`() {
        val row = mapping.toJpaEntity(data { execution.pendingDiagnostics() })

        assertThrows(IllegalStateException::class.java) {
            taskValidationRequestData {
                task(1)
                requestedBy(2)
                snapshot = taskValidationSnapshot {}
                mapping.decodeExecution(
                    row = row,
                    diagnostics = emptyList(),
                    submissionIds = listOf(SubmissionId(4)),
                    builder = this,
                )
            }
        }
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
                    diagnostics = mutableListOf(
                        testDiagnosticResult {
                            testId(3)
                            reports += diagnosticReport {
                                severity = DiagnosticSeverity.Error
                                data.missingTimeLimit()
                            }
                        },
                    )
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

        @JvmStatic
        fun invalidRows(): List<TaskValidationRequestJpaEntity> = listOf(
            row(TaskValidationExecutionJpaEnum.PENDING_DIAGNOSTICS, complete = true),
            row(TaskValidationExecutionJpaEnum.DIAGNOSTICS_IN_PROGRESS, submitted = true),
            row(TaskValidationExecutionJpaEnum.AWAITING_SUBMISSIONS),
            row(TaskValidationExecutionJpaEnum.AWAITING_SUBMISSIONS, complete = true, submitted = true),
            row(TaskValidationExecutionJpaEnum.STOPPED_BY_DIAGNOSTICS, complete = true),
            row(TaskValidationExecutionJpaEnum.SUBMISSIONS_CREATED, complete = true),
            row(TaskValidationExecutionJpaEnum.SUBMISSIONS_CREATED, submitted = true),
            row(TaskValidationExecutionJpaEnum.TECHNICAL_FAILURE),
        )

        private fun row(execution: TaskValidationExecutionJpaEnum, complete: Boolean = false, submitted: Boolean = false) =
            TaskValidationRequestJpaEntity(
                taskId = 1,
                requestedById = 2,
                execution = execution,
                areDiagnosticsComplete = complete,
                areSubmissionsCreated = submitted,
                failureDescription = null,
                failureOccurredAt = null,
                completedAt = null,
            )

        private fun data(builder: TaskValidationRequestDataBuilder.() -> Unit): TaskValidationRequestData = taskValidationRequestData {
            task(1)
            requestedBy(2)
            snapshot = taskValidationSnapshot { tests(listOf(3)) }
            builder()
        }
    }
}
