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
import tech.testsys.infra.database.internal.jpa.entity.task.AuthorSubmissionFailureJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionToTaskValidationRequestId
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionToTaskValidationRequestJpaEntity
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

    @Test
    fun `should create one author input row per developer solution keeping its first position and last values`() {
        val inputs = listOf(
            developerSolutionValidationInput {
                developerSolution(3)
                solution(4)
                expectedScore = Score(10)
            },
            developerSolutionValidationInput {
                developerSolution(5)
                solution(6)
                expectedScore = Score(20)
            },
            developerSolutionValidationInput {
                developerSolution(3)
                solution(7)
                expectedScore = Score(30)
            },
        )

        val rows = mapping.toDeveloperSolutionAssociations(requestId = 1, inputs = inputs)

        assertEquals(listOf(3L, 5L), rows.map { it.id.developerSolutionId })
        assertEquals(listOf(7L, 6L), rows.map { it.solutionId })
        assertEquals(listOf(30, 20), rows.map { it.expectedScore })
    }

    @ParameterizedTest
    @MethodSource("states")
    fun `should restore every state with its required stage payload`(source: TaskValidationRequestData) {
        val row = mapping.toJpaEntity(source)
        val links = mapping.toSubmissionAssociations(
            requestId = 7,
            submissionIds = (source.execution as? TaskValidationExecution.WithSubmissions)?.submissions?.ids.orEmpty(),
            failures = (source.execution as? TaskValidationExecution.Completed)?.failures.orEmpty(),
        )

        val restored = taskValidationRequestData {
            task(1)
            requestedBy(2)
            snapshot = source.snapshot
            mapping.decodeExecution(
                row = row,
                diagnostics = (source.execution as? TaskValidationExecution.WithDiagnostics)?.diagnostics.orEmpty(),
                submissionIds = links.map { SubmissionId(it.id.submissionId) },
                failures = links.mapNotNull { mapping.toFailure(it) },
                builder = this,
            )
        }

        assertEquals(source.execution::class, restored.execution::class)
        assertEquals(
            (source.execution as? TaskValidationExecution.Completed)?.completedAt,
            (restored.execution as? TaskValidationExecution.Completed)?.completedAt,
        )
        assertEquals(
            (source.execution as? TaskValidationExecution.Completed)?.failures,
            (restored.execution as? TaskValidationExecution.Completed)?.failures,
        )
        assertEquals(source.isActive, restored.isActive)
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
                    failures = emptyList(),
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
                    failures = emptyList(),
                    builder = this,
                )
            }
        }
    }

    @Test
    fun `should reject stored submission failures before testing is completed`() {
        val row = mapping.toJpaEntity(
            data {
                execution.submissionsCreated {
                    diagnostics = mutableListOf(testDiagnosticResult { testId(3) })
                    submissions = mutableListOf(SubmissionId(4))
                }
            },
        )

        assertThrows(IllegalStateException::class.java) {
            taskValidationRequestData {
                task(1)
                requestedBy(2)
                snapshot = taskValidationSnapshot { tests(listOf(3)) }
                mapping.decodeExecution(
                    row = row,
                    diagnostics = listOf(testDiagnosticResult { testId(3) }),
                    submissionIds = listOf(SubmissionId(4)),
                    failures = listOf(AuthorSubmissionFailure.GradingFailed(SubmissionId(4))),
                    builder = this,
                )
            }
        }
    }

    @ParameterizedTest
    @MethodSource("invalidFailureLinks")
    fun `should reject a stored failure with an inconsistent score`(link: SubmissionToTaskValidationRequestJpaEntity) {
        assertThrows(IllegalStateException::class.java) { mapping.toFailure(link) }
    }

    @ParameterizedTest
    @MethodSource("misplacedFailures")
    fun `should reject failures that are not distinct linked submissions in submission order`(failures: List<AuthorSubmissionFailure>) {
        assertThrows(IllegalArgumentException::class.java) {
            mapping.toSubmissionAssociations(requestId = 7, submissionIds = listOf(SubmissionId(4), SubmissionId(5)), failures = failures)
        }
    }

    companion object {
        @JvmStatic
        fun states(): List<TaskValidationRequestData> = listOf(
            data { execution.pendingDiagnostics() },
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
            completed(),
            completed(
                AuthorSubmissionFailure.GradingFailed(SubmissionId(5)),
                AuthorSubmissionFailure.ScoreMismatch(submission = SubmissionId(4), actualScore = 4_294_967_294L),
            ),
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
            row(TaskValidationExecutionJpaEnum.PENDING_DIAGNOSTICS, submitted = true),
            row(TaskValidationExecutionJpaEnum.AWAITING_SUBMISSIONS),
            row(TaskValidationExecutionJpaEnum.AWAITING_SUBMISSIONS, complete = true, submitted = true),
            row(TaskValidationExecutionJpaEnum.STOPPED_BY_DIAGNOSTICS, complete = true),
            row(TaskValidationExecutionJpaEnum.SUBMISSIONS_CREATED, complete = true),
            row(TaskValidationExecutionJpaEnum.SUBMISSIONS_CREATED, submitted = true),
            row(TaskValidationExecutionJpaEnum.TECHNICAL_FAILURE),
            row(TaskValidationExecutionJpaEnum.COMPLETED, complete = true, submitted = true),
        )

        @JvmStatic
        fun invalidFailureLinks(): List<SubmissionToTaskValidationRequestJpaEntity> = listOf(
            link(failure = AuthorSubmissionFailureJpaEnum.SCORE_MISMATCH, actualScore = null),
            link(failure = AuthorSubmissionFailureJpaEnum.GRADING_FAILED, actualScore = 3),
            link(failure = null, actualScore = 3),
        )

        @JvmStatic
        fun misplacedFailures(): List<List<AuthorSubmissionFailure>> = listOf(
            listOf(AuthorSubmissionFailure.GradingFailed(SubmissionId(5)), AuthorSubmissionFailure.GradingFailed(SubmissionId(4))),
            listOf(AuthorSubmissionFailure.GradingFailed(SubmissionId(4)), AuthorSubmissionFailure.GradingFailed(SubmissionId(4))),
            listOf(AuthorSubmissionFailure.GradingFailed(SubmissionId(9))),
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

        private fun link(failure: AuthorSubmissionFailureJpaEnum?, actualScore: Long?) = SubmissionToTaskValidationRequestJpaEntity(
            id = SubmissionToTaskValidationRequestId(submissionId = 4, requestId = 7),
            position = 0,
            failure = failure,
            actualScore = actualScore,
        )

        private fun completed(vararg failures: AuthorSubmissionFailure): TaskValidationRequestData = data {
            execution.completed {
                diagnostics = mutableListOf(testDiagnosticResult { testId(3) })
                submissions = mutableListOf(SubmissionId(5), SubmissionId(4))
                this.failures = failures.toMutableList()
                completedAt = Instant.EPOCH
            }
        }

        private fun data(builder: TaskValidationRequestDataBuilder.() -> Unit): TaskValidationRequestData = taskValidationRequestData {
            task(1)
            requestedBy(2)
            snapshot = taskValidationSnapshot { tests(listOf(3)) }
            builder()
        }
    }
}
