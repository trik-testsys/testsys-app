package tech.testsys.operation

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import tech.testsys.domain.builder.api.*
import tech.testsys.domain.builder.data
import tech.testsys.domain.builder.task.SubmissionDataBuilder
import tech.testsys.domain.builder.task.TaskValidationRequestDataBuilder
import tech.testsys.domain.contract.Grader
import tech.testsys.domain.contract.GradingAdmission
import tech.testsys.domain.contract.PolygonDiagnostics
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.TaskValidationRequestRepository
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.task.*
import tech.testsys.operation.util.testTaskValidationRequest
import java.time.Instant
import java.util.UUID

class TaskValidationOperationsTests {
    private val requests = mockk<TaskValidationRequestRepository>()
    private val tests = mockk<TestRepository>()
    private val diagnostics = mockk<PolygonDiagnostics>()

    // List loads answer through the stubbed findById in reverse request order.
    private val submissions = mockk<SubmissionRepository>().also { repository ->
        every { repository.load(any<LazyEntityList<SubmissionId, Submission>>()) } answers {
            firstArg<LazyEntityList<SubmissionId, Submission>>().ids.reversed().map { id ->
                requireNotNull(repository.findById(id)) { "Missing submission ${id.value}" }
            }
        }
    }
    private val verdicts = mockk<VerdictRepository>()
    private val grader = mockk<Grader>()
    private val operations = TaskValidationOperations(
        requests = requests,
        tests = tests,
        diagnostics = diagnostics,
        submissions = submissions,
        verdicts = verdicts,
        grader = grader,
    )
    private val requestId = TaskValidationRequestId(11)

    @Nested
    inner class ProceedTests {

        @Nested
        inner class HappyPathTests {
            @Test
            fun `should run diagnostics and then save and send every author submission if diagnostics are clean`() {
                val saved = stored(request { execution.pendingDiagnostics() })
                every { requests.startDiagnostics(requestId) } answers { saved.current }
                every { requests.findDiagnosticProgress(requestId) } returns diagnosticResults()
                every { requests.completeDiagnostics(requestId) } answers {
                    saved.current = awaitingRequest()
                    saved.current
                }
                queuedRuns()

                val result = requireNotNull(operations.proceed(requestId))

                assertInstanceOf(TaskValidationExecution.SubmissionsCreated::class.java, result.data.execution)
                assertTrue(result.data.isActive)
                verifyOrder {
                    requests.createSubmissions(requestId)
                    grader.sendToGrade(match { it.id == SubmissionId(101) })
                    grader.sendToGrade(match { it.id == SubmissionId(102) })
                }
            }

            @Test
            fun `should not create submissions if diagnostics stop the request with Error`() {
                val pending = request { execution.pendingDiagnostics() }
                val error = testDiagnosticResult {
                    testId(1)
                    reports += diagnosticReport {
                        severity = DiagnosticSeverity.Error
                        data.missingTimeLimit()
                    }
                }
                val stopped = pending.withData {
                    execution.stoppedByDiagnostics {
                        diagnostics = mutableListOf(error, testDiagnosticResult { testId(2) })
                        completedAt = Instant.EPOCH
                    }
                }
                stored(pending)
                every { requests.startDiagnostics(requestId) } returns pending
                every { requests.findDiagnosticProgress(requestId) } returns listOf(error, testDiagnosticResult { testId(2) })
                every { requests.completeDiagnostics(requestId) } returns stopped

                assertSame(stopped, operations.proceed(requestId))

                verify(exactly = 0) { requests.createSubmissions(any()) }
                verify(exactly = 0) { grader.sendToGrade(any()) }
            }

            @Test
            fun `should reread the request if it is no longer eligible for diagnostics`() {
                val pending = request { execution.pendingDiagnostics() }
                val stopped = pending.withData {
                    execution.incompleteDiagnosticsFailure {
                        failure = taskValidationTechnicalFailure {
                            description = "Storage unavailable"
                            occurredAt = Instant.EPOCH
                        }
                    }
                }
                every { requests.findById(requestId) } returnsMany listOf(pending, stopped)
                every { requests.startDiagnostics(requestId) } returns null

                assertSame(stopped, operations.proceed(requestId))

                verify(exactly = 0) { requests.createSubmissions(any()) }
            }

            @Test
            fun `should save and send submissions of a request awaiting them`() {
                stored(awaitingRequest())
                queuedRuns()

                val result = requireNotNull(operations.proceed(requestId))

                assertEquals(
                    listOf(SubmissionId(101), SubmissionId(102)),
                    (result.data.execution as TaskValidationExecution.WithSubmissions).submissions.ids,
                )
                verify(exactly = 1) { requests.createSubmissions(requestId) }
                verify(exactly = 2) { grader.sendToGrade(any()) }
            }

            @Test
            fun `should complete as passed if each submission totals the expected score over all polygons`() {
                val saved = stored(createdRequest())
                gradedRuns(firstScores = listOf(2, 3), secondScores = listOf(4, 1))

                operations.proceed(requestId)

                val state = assertInstanceOf(
                    TaskValidationExecution.Completed::class.java,
                    saved.current.data.execution,
                )
                assertEquals(emptyList<AuthorSubmissionFailure>(), state.failures)
                assertFalse(saved.current.data.isActive)
            }

            @Test
            fun `should complete with the mismatching submission and its total`() {
                val saved = stored(createdRequest())
                gradedRuns(firstScores = listOf(2, 3), secondScores = listOf(4, 2))

                operations.proceed(requestId)

                assertEquals(
                    listOf(AuthorSubmissionFailure.ScoreMismatch(submission = SubmissionId(102), actualScore = 6)),
                    (saved.current.data.execution as TaskValidationExecution.Completed).failures,
                )
            }

            @Test
            fun `should sum polygon scores without integer overflow`() {
                val saved = stored(createdRequest())
                gradedRuns(firstScores = listOf(Int.MAX_VALUE, Int.MAX_VALUE), secondScores = listOf(4, 1))

                operations.proceed(requestId)

                assertEquals(
                    listOf(AuthorSubmissionFailure.ScoreMismatch(submission = SubmissionId(101), actualScore = 4_294_967_294L)),
                    (saved.current.data.execution as TaskValidationExecution.Completed).failures,
                )
            }

            @Test
            fun `should compare each submission with the expected score of its own author solution when programs are shared`() {
                val saved = stored(
                    request {
                        snapshot = taskValidationSnapshot {
                            tests(listOf(1, 2))
                            developerSolutions = mutableListOf(input(authorId = 20, expected = 9), input(authorId = 10, expected = 5))
                            supportedTrikStudioVersions = mutableListOf(TrikStudioVersion("v1"))
                        }
                        execution.submissionsCreated {
                            diagnostics = diagnosticResults()
                            submissions(listOf(101, 102))
                        }
                    },
                )
                gradedRuns(firstScores = listOf(2, 3), secondScores = listOf(2, 3))

                operations.proceed(requestId)

                assertEquals(
                    listOf(AuthorSubmissionFailure.ScoreMismatch(submission = SubmissionId(102), actualScore = 5)),
                    (saved.current.data.execution as TaskValidationExecution.Completed).failures,
                )
            }

            @ParameterizedTest
            @MethodSource("tech.testsys.operation.TaskValidationOperationsTests#failedRuns")
            fun `should complete with every failed submission in submission order if grading failed`(failed: Submission) {
                val saved = stored(createdRequest())
                every { submissions.findById(SubmissionId(101)) } returns failed
                every { submissions.findById(SubmissionId(102)) } returns run(102, "v2") { status.graded { status.timeout() } }

                operations.proceed(requestId)

                assertEquals(
                    listOf(
                        AuthorSubmissionFailure.GradingFailed(SubmissionId(101)),
                        AuthorSubmissionFailure.GradingFailed(SubmissionId(102)),
                    ),
                    (saved.current.data.execution as TaskValidationExecution.Completed).failures,
                )
            }
        }

        @Nested
        inner class InvariantTests {
            @Test
            fun `should wait without creating or resending submissions if any submission is not graded even when another has failed`() {
                val request = stored(createdRequest()).current
                every { submissions.findById(SubmissionId(101)) } returns run(101, "v1") { status.graded { status.timeout() } }
                every { submissions.findById(SubmissionId(102)) } returns run(102, "v2") { status.inProgress() }

                assertSame(request, operations.proceed(requestId))

                verify(exactly = 0) { requests.createSubmissions(any()) }
                verify(exactly = 0) { requests.completeTesting(any(), any()) }
                verify(exactly = 0) { grader.sendToGrade(any()) }
            }

            @ParameterizedTest
            @MethodSource("tech.testsys.operation.TaskValidationOperationsTests#terminalRequests")
            fun `should return a terminal request unchanged`(request: TaskValidationRequest) {
                every { requests.findById(requestId) } returns request

                assertSame(request, operations.proceed(requestId))

                verify(exactly = 0) { requests.createSubmissions(any()) }
                verify(exactly = 0) { requests.completeTesting(any(), any()) }
                verify(exactly = 0) { grader.sendToGrade(any()) }
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should return null for a missing request`() {
                every { requests.findById(requestId) } returns null

                assertNull(operations.proceed(requestId))

                verify(exactly = 0) { grader.sendToGrade(any()) }
            }

            @Test
            fun `should propagate a send failure while keeping the saved submissions`() {
                val saved = stored(awaitingRequest())
                queuedRuns()
                val failure = IllegalStateException("Grader unavailable")
                every { grader.sendToGrade(any()) } throws failure

                assertSame(failure, assertThrows(IllegalStateException::class.java) { operations.proceed(requestId) })

                assertInstanceOf(TaskValidationExecution.SubmissionsCreated::class.java, saved.current.data.execution)
                verify(exactly = 0) { requests.recordTechnicalFailure(any(), any()) }
            }

            @Test
            fun `should reject saved submissions that do not cover every author run`() {
                stored(
                    request {
                        execution.submissionsCreated {
                            diagnostics = diagnosticResults()
                            submissions(listOf(101))
                        }
                    },
                )
                every { submissions.findById(SubmissionId(101)) } returns run(101, "v1") { status.graded { status.timeout() } }

                assertThrows(IllegalStateException::class.java) { operations.proceed(requestId) }

                verify(exactly = 0) { requests.completeTesting(any(), any()) }
            }

            @Test
            fun `should load submissions and verdicts with one call each and keep failures in submission order`() {
                val saved = stored(createdRequest())
                every { submissions.findById(SubmissionId(101)) } returns run(101, "v1") { status.graded { status.timeout() } }
                every { submissions.findById(SubmissionId(102)) } returns run(102, "v2") {
                    status.graded { status.success { verdict(202) } }
                }
                stubVerdicts(mapOf(VerdictId(202) to listOf(4, 2)))

                operations.proceed(requestId)

                assertEquals(
                    listOf(
                        AuthorSubmissionFailure.GradingFailed(SubmissionId(101)),
                        AuthorSubmissionFailure.ScoreMismatch(submission = SubmissionId(102), actualScore = 6),
                    ),
                    (saved.current.data.execution as TaskValidationExecution.Completed).failures,
                )
                verify(exactly = 1) {
                    submissions.load(
                        match<LazyEntityList<SubmissionId, Submission>> { list ->
                            list.ids == listOf(SubmissionId(101), SubmissionId(102))
                        },
                    )
                    verdicts.load(match<LazyEntityList<VerdictId, Verdict>> { list -> list.ids == listOf(VerdictId(202)) })
                }
            }

            @Test
            fun `should not load verdicts if grading failed for every submission`() {
                stored(createdRequest())
                every { submissions.findById(SubmissionId(101)) } returns run(101, "v1") { status.graded { status.timeout() } }
                every { submissions.findById(SubmissionId(102)) } returns run(102, "v2") {
                    status.graded { status.error { description = "Crash" } }
                }

                operations.proceed(requestId)

                verify { verdicts wasNot Called }
            }
        }
    }

    @Nested
    inner class ResendUnfinishedSubmissionsTests {

        @Nested
        inner class HappyPathTests {
            @Test
            fun `should resend only submissions that are not graded`() {
                val request = stored(createdRequest()).current
                val queued = run(101, "v1")
                every { submissions.findById(SubmissionId(101)) } returns queued
                every { submissions.findById(SubmissionId(102)) } returns run(102, "v2") { status.graded { status.timeout() } }
                every { grader.sendToGrade(queued) } returns GradingAdmission.Accepted

                assertSame(request, operations.resendUnfinishedSubmissions(requestId))

                verify(exactly = 1) { grader.sendToGrade(queued) }
                verify(exactly = 1) { grader.sendToGrade(any()) }
            }

            @Test
            fun `should resend submissions that are still in progress`() {
                stored(createdRequest())
                val pending = run(102, "v2") { status.inProgress() }
                every { submissions.findById(SubmissionId(101)) } returns run(101, "v1") { status.graded { status.timeout() } }
                every { submissions.findById(SubmissionId(102)) } returns pending
                every { grader.sendToGrade(pending) } returns GradingAdmission.AlreadyPending

                operations.resendUnfinishedSubmissions(requestId)

                verify(exactly = 1) { grader.sendToGrade(pending) }
            }

            @Test
            fun `should not send anything for a request without created submissions`() {
                val request = stored(awaitingRequest()).current

                assertSame(request, operations.resendUnfinishedSubmissions(requestId))

                verify(exactly = 0) { grader.sendToGrade(any()) }
                verify(exactly = 0) { requests.createSubmissions(any()) }
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should return null for a missing request`() {
                every { requests.findById(requestId) } returns null

                assertNull(operations.resendUnfinishedSubmissions(requestId))
            }
        }
    }

    @Nested
    inner class RunDiagnosticsTests {

        @Nested
        inner class HappyPathTests {
            @Test
            fun `should complete an empty polygon snapshot without invoking the adapter`() {
                val request = testTaskValidationRequest { execution.pendingDiagnostics() }
                val complete = request.withData {
                    execution.awaitingSubmissions {}
                }
                every { requests.startDiagnostics(requestId) } returns request
                every { requests.findDiagnosticProgress(requestId) } returns emptyList()
                every { requests.completeDiagnostics(requestId) } returns complete

                val result = operations.runDiagnostics(requestId)

                assertSame(complete, result)
                verify(exactly = 0) { diagnostics.diagnose(any()) }
            }

            @Test
            fun `should skip polygons with saved results and save the result of each remaining polygon including empty reports`() {
                val request = testTaskValidationRequest {
                    snapshot = taskValidationSnapshot { tests(listOf(1, 2, 3)) }
                    execution.pendingDiagnostics()
                }
                val saved = testDiagnosticResult { testId(1) }
                val second = testDiagnosticResult { testId(2) }
                val third = testDiagnosticResult { testId(3) }
                val secondPolygon = polygon(2)
                val thirdPolygon = polygon(3)
                val complete = request.withData {
                    execution.awaitingSubmissions { diagnostics = mutableListOf(saved, second, third) }
                }
                every { requests.startDiagnostics(requestId) } returns request
                every { requests.findDiagnosticProgress(requestId) } returns listOf(saved)
                every { tests.findById(TestId(2)) } returns secondPolygon
                every { tests.findById(TestId(3)) } returns thirdPolygon
                every { diagnostics.diagnose(secondPolygon) } returns second
                every { diagnostics.diagnose(thirdPolygon) } returns third
                every { requests.saveDiagnosticProgress(requestId, second) } returns second
                every { requests.saveDiagnosticProgress(requestId, third) } returns third
                every { requests.completeDiagnostics(requestId) } returns complete

                val result = operations.runDiagnostics(requestId)

                assertSame(complete, result)
                verify(exactly = 0) { tests.findById(TestId(1)) }
                verify(exactly = 1) { requests.saveDiagnosticProgress(requestId, second) }
                verify(exactly = 1) { requests.saveDiagnosticProgress(requestId, third) }
            }
        }

        @Nested
        inner class InvariantTests {
            @Test
            fun `should diagnose and save the remaining polygons after one polygon yields an Error`() {
                val request = testTaskValidationRequest {
                    snapshot = taskValidationSnapshot { tests(listOf(1, 2)) }
                    execution.pendingDiagnostics()
                }
                val failed = testDiagnosticResult {
                    testId(1)
                    reports += diagnosticReport {
                        severity = DiagnosticSeverity.Error
                        data.missingTimeLimit()
                    }
                }
                val clean = testDiagnosticResult { testId(2) }
                val first = polygon(1)
                val second = polygon(2)
                val complete = request.withData {
                    execution.stoppedByDiagnostics {
                        diagnostics = mutableListOf(failed, clean)
                        completedAt = Instant.EPOCH
                    }
                }
                every { requests.startDiagnostics(requestId) } returns request
                every { requests.findDiagnosticProgress(requestId) } returns emptyList()
                every { tests.findById(TestId(1)) } returns first
                every { tests.findById(TestId(2)) } returns second
                every { diagnostics.diagnose(first) } returns failed
                every { diagnostics.diagnose(second) } returns clean
                every { requests.saveDiagnosticProgress(requestId, failed) } returns failed
                every { requests.saveDiagnosticProgress(requestId, clean) } returns clean
                every { requests.completeDiagnostics(requestId) } returns complete

                val result = operations.runDiagnostics(requestId)

                assertSame(complete, result)
                verify(exactly = 1) { diagnostics.diagnose(second) }
                verify(exactly = 1) { requests.saveDiagnosticProgress(requestId, clean) }
            }

            @Test
            fun `should reject a result for another polygon before persisting it`() {
                val request = testTaskValidationRequest {
                    snapshot = taskValidationSnapshot { tests(listOf(2)) }
                    execution.pendingDiagnostics()
                }
                val polygon = polygon(2)
                every { requests.startDiagnostics(requestId) } returns request
                every { requests.findDiagnosticProgress(requestId) } returns emptyList()
                every { tests.findById(TestId(2)) } returns polygon
                every { diagnostics.diagnose(polygon) } returns testDiagnosticResult { testId(99) }

                assertThrows(IllegalStateException::class.java) { operations.runDiagnostics(requestId) }

                verify(exactly = 0) { requests.saveDiagnosticProgress(any(), any()) }
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should skip a terminal or missing caller-selected request`() {
                every { requests.startDiagnostics(requestId) } returns null

                val result = operations.runDiagnostics(requestId)

                assertNull(result)
                verify(exactly = 0) { diagnostics.diagnose(any()) }
            }

            @Test
            fun `should propagate adapter exceptions without completing diagnostics or converting the failure`() {
                val request = testTaskValidationRequest {
                    snapshot = taskValidationSnapshot { tests(listOf(2)) }
                    execution.pendingDiagnostics()
                }
                val polygon = polygon(2)
                val failure = IllegalStateException("Storage unavailable")
                every { requests.startDiagnostics(requestId) } returns request
                every { requests.findDiagnosticProgress(requestId) } returns emptyList()
                every { tests.findById(TestId(2)) } returns polygon
                every { diagnostics.diagnose(polygon) } throws failure

                val actual = assertThrows(IllegalStateException::class.java) { operations.runDiagnostics(requestId) }

                assertSame(failure, actual)
                verify(exactly = 0) { requests.completeDiagnostics(any()) }
                verify(exactly = 0) { requests.recordTechnicalFailure(any(), any()) }
            }
        }
    }

    // The request held by the mocked storage between calls.
    private class Storage(var current: TaskValidationRequest)

    // Mocks storage of a single request, applying the submission and completion state transitions.
    private fun stored(initial: TaskValidationRequest): Storage {
        val storage = Storage(initial)
        every { requests.findById(requestId) } answers { storage.current }
        every { requests.createSubmissions(requestId) } answers {
            val state = storage.current.data.execution
            if (state is TaskValidationExecution.AwaitingSubmissions) {
                storage.current = storage.current.withData {
                    execution.submissionsCreated {
                        diagnostics = state.diagnostics.toMutableList()
                        submissions(listOf(101, 102))
                    }
                }
            }
            storage.current
        }
        every { requests.completeTesting(requestId, any()) } answers {
            val state = storage.current.data.execution as TaskValidationExecution.SubmissionsCreated
            storage.current = storage.current.withData {
                execution.completed {
                    diagnostics = state.diagnostics.toMutableList()
                    submissions = state.submissions.ids.toMutableList()
                    failures = secondArg<List<AuthorSubmissionFailure>>().toMutableList()
                    completedAt = Instant.EPOCH
                }
            }
            storage.current
        }
        return storage
    }

    private fun queuedRuns() {
        every { submissions.findById(SubmissionId(101)) } returns run(101, "v1")
        every { submissions.findById(SubmissionId(102)) } returns run(102, "v2")
        every { grader.sendToGrade(any()) } returns GradingAdmission.Accepted
    }

    private fun gradedRuns(firstScores: List<Int>, secondScores: List<Int>) {
        every { submissions.findById(SubmissionId(101)) } returns run(101, "v1") { status.graded { status.success { verdict(201) } } }
        every { submissions.findById(SubmissionId(102)) } returns run(102, "v2") { status.graded { status.success { verdict(202) } } }
        stubVerdicts(mapOf(VerdictId(201) to firstScores, VerdictId(202) to secondScores))
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

    private fun stubVerdicts(scoresByVerdict: Map<VerdictId, List<Int>>) {
        every { verdicts.load(any<LazyEntityList<VerdictId, Verdict>>()) } answers {
            firstArg<LazyEntityList<VerdictId, Verdict>>().ids.reversed().map { verdictId ->
                val scores = scoresByVerdict.getValue(verdictId)
                verdict {
                    id = verdictId.value
                    createdAt = Instant.EPOCH
                    data {
                        task(0)
                        submission(verdictId.value - 100)
                        testVerdict {
                            test(1)
                            logs(1)
                            score = scores[0]
                        }
                        testVerdict {
                            test(2)
                            logs(2)
                            score = scores[1]
                        }
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun failedRuns(): List<Submission> = listOf(
            run(101, "v1") { status.graded { status.error { description = "Grading failure" } } },
            run(101, "v1") { status.graded { status.timeout() } },
        )

        @JvmStatic
        fun terminalRequests(): List<TaskValidationRequest> = listOf(
            request {
                execution.stoppedByDiagnostics {
                    diagnostics = mutableListOf(
                        testDiagnosticResult {
                            testId(1)
                            reports += diagnosticReport {
                                severity = DiagnosticSeverity.Error
                                data.missingTimeLimit()
                            }
                        },
                        testDiagnosticResult { testId(2) },
                    )
                    completedAt = Instant.EPOCH
                }
            },
            request {
                execution.completed {
                    diagnostics = diagnosticResults()
                    submissions(listOf(101, 102))
                    completedAt = Instant.EPOCH
                }
            },
            request {
                execution.incompleteDiagnosticsFailure {
                    failure = taskValidationTechnicalFailure {
                        description = "Diagnostics unavailable"
                        occurredAt = Instant.EPOCH
                    }
                }
            },
            request {
                execution.completedDiagnosticsFailure {
                    diagnostics = diagnosticResults()
                    failure = taskValidationTechnicalFailure {
                        description = "Submission storage unavailable"
                        occurredAt = Instant.EPOCH
                    }
                }
            },
            request {
                execution.createdSubmissionsFailure {
                    diagnostics = diagnosticResults()
                    submissions(listOf(101, 102))
                    failure = taskValidationTechnicalFailure {
                        description = "Grading unavailable"
                        occurredAt = Instant.EPOCH
                    }
                }
            },
        )

        // One author solution expecting 5 points, checked in two versions on two polygons.
        private fun request(builder: TaskValidationRequestDataBuilder.() -> Unit = {}): TaskValidationRequest {
            return testTaskValidationRequest {
                snapshot = taskValidationSnapshot {
                    tests(listOf(1, 2))
                    developerSolutions = mutableListOf(input(authorId = 10, expected = 5))
                    supportedTrikStudioVersions = mutableListOf(TrikStudioVersion("v2"), TrikStudioVersion("v1"))
                }
                builder()
            }
        }

        private fun awaitingRequest(): TaskValidationRequest = request {
            execution.awaitingSubmissions { diagnostics = diagnosticResults() }
        }

        private fun createdRequest(): TaskValidationRequest = request {
            execution.submissionsCreated {
                diagnostics = diagnosticResults()
                submissions(listOf(101, 102))
            }
        }

        private fun input(authorId: Long, expected: Int): DeveloperSolutionValidationInput = developerSolutionValidationInput {
            developerSolution(authorId)
            solution(7)
            expectedScore = Score(expected)
        }

        private fun diagnosticResults(): MutableList<TestDiagnosticResult> =
            mutableListOf(testDiagnosticResult { testId(1) }, testDiagnosticResult { testId(2) })

        private fun run(submissionId: Long, version: String, builder: SubmissionDataBuilder.() -> Unit = {}): Submission = submission {
            id = submissionId
            createdAt = Instant.EPOCH
            data {
                author(0)
                task(0)
                solution(7)
                kind.developerSolutionTest { trikStudioVersion(version) }
                status.queued()
                builder()
            }
        }
    }
}
