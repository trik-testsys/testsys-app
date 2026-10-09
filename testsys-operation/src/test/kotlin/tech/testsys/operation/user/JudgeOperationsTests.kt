package tech.testsys.operation.user

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.judgeData
import tech.testsys.domain.builder.api.judgmentOrder
import tech.testsys.domain.builder.api.judgmentOrderData
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.builder.api.participant
import tech.testsys.domain.builder.api.participantData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.api.submission
import tech.testsys.domain.builder.api.submissionData
import tech.testsys.domain.builder.api.verdict
import tech.testsys.domain.builder.api.verdictData
import tech.testsys.domain.builder.task.SubmissionDataBuilder
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.contract.persistence.VerdictFilter
import tech.testsys.domain.contract.persistence.repository.JudgmentOrderRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderData
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.UserId
import tech.testsys.operation.error.BlankJudgmentReasonError
import tech.testsys.operation.error.ChangeVerdictError
import tech.testsys.operation.error.MissedJudgeRoleError
import tech.testsys.operation.error.NegativeJudgmentScoreError
import tech.testsys.operation.error.SubmissionAccessDeniedError
import tech.testsys.operation.error.SubmissionIsDeveloperSolutionTestError
import tech.testsys.operation.error.SubmissionNotExistsError
import tech.testsys.operation.error.SubmissionNotSuccessfullyGradedError
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.testDeveloper
import tech.testsys.operation.util.testJudge
import tech.testsys.operation.util.testMultipleRoleUser
import tech.testsys.operation.util.testStudent
import java.time.Instant

class JudgeOperationsTests {

    private val repository = mockk<VerdictRepository>()
    private val submissionRepository = mockk<SubmissionRepository>()
    private val judgmentOrderRepository = mockk<JudgmentOrderRepository>()
    private val userRepository = mockk<MultipleRoleUserRepository>()
    private val participantRepository = mockk<ParticipantRepository>()
    private val operations = JudgeOperations(
        verdictRepository = repository,
        submissionRepository = submissionRepository,
        judgmentOrderRepository = judgmentOrderRepository,
        multipleRoleUserRepository = userRepository,
        participantRepository = participantRepository,
    )
    private val judge = testJudge { data = judgeData {} }

    private fun userWithOtherRolesThanJudge(): MultipleRoleUser = testMultipleRoleUser {
        roles {
            student { data = studentData {} }
            developer { data = developerData {} }
            manager { data = managerData {} }
        }
    }

    private fun allRepositories() = listOf(repository, submissionRepository, judgmentOrderRepository, userRepository, participantRepository)

    @Nested
    inner class ViewResultsTests {

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should forward all supplied verdict filters unchanged`() {
                val pagination = Pagination(page = 1, size = 2)
                val filter = VerdictFilter(
                    authorId = MultipleRoleUserId(5),
                    submissionId = SubmissionId(6),
                    classId = ClassId(7),
                    competitionId = CompetitionId(8),
                )
                val page = Page<Verdict>(content = emptyList(), pagination = pagination, totalElements = 0)
                every { repository.findAvailableToJudge(refEq(pagination), refEq(filter)) } returns page

                val result = operations.viewResults(user = judge, pagination = pagination, filter = filter).getOrThrow()

                assertSame(page, result)
            }

            @Test
            fun `should query with an empty filter and return the found page if no filters are given`() {
                val pagination = Pagination(page = 1, size = 2)
                val responsePagination = pagination.copy(sort = Sort(listOf(Sort.Order("storageOrder"))))
                val page = Page(
                    content = listOf(testVerdict(10), testVerdict(20)),
                    pagination = responsePagination,
                    totalElements = 5,
                )
                every { repository.findAvailableToJudge(refEq(pagination), VerdictFilter()) } returns page

                val result = operations.viewResults(user = judge, pagination = pagination).getOrThrow()

                assertSame(page, result)
            }

            @ParameterizedTest
            @MethodSource("tech.testsys.operation.user.JudgeOperationsTests#authorIds")
            fun `should forward the author filter of a student or a participant unchanged`(authorId: UserId) {
                val pagination = Pagination(page = 3, size = 4)
                val page = Page<Verdict>(content = emptyList(), pagination = pagination, totalElements = 2)
                every { repository.findAvailableToJudge(refEq(pagination), VerdictFilter(authorId = authorId)) } returns page

                val result = operations.viewResults(
                    user = judge,
                    pagination = pagination,
                    filter = VerdictFilter(authorId = authorId),
                ).getOrThrow()

                assertSame(page, result)
            }

            @Test
            fun `should forward the requested sorting to the repository unchanged`() {
                val pagination =
                    Pagination(page = 0, size = 10, sort = Sort(listOf(Sort.Order("storageField", Sort.Direction.DESC))))
                val page = Page<Verdict>(content = emptyList(), pagination = pagination, totalElements = 0)
                every { repository.findAvailableToJudge(refEq(pagination), VerdictFilter()) } returns page

                val result = operations.viewResults(user = judge, pagination = pagination).getOrThrow()

                assertSame(page, result)
            }

            @Test
            fun `should return an empty page without a domain error if no verdicts match`() {
                val pagination = Pagination(page = 0, size = 10)
                val page = Page<Verdict>(content = emptyList(), pagination = pagination, totalElements = 0)
                every { repository.findAvailableToJudge(page.pagination, VerdictFilter()) } returns page

                val result = operations.viewResults(user = judge, pagination = pagination).getOrThrow()

                assertSame(page, result)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedJudgeRoleError before querying if user has no roles`() {
                val user = testMultipleRoleUser {}

                assertRaises(MissedJudgeRoleError) { operations.viewResults(user = user, pagination = Pagination(page = 0, size = 10)) }

                verify { repository wasNot Called }
            }

            @Test
            fun `should raise MissedJudgeRoleError before querying if user holds other roles but not the judge role`() {
                assertRaises(MissedJudgeRoleError) {
                    operations.viewResults(user = userWithOtherRolesThanJudge(), pagination = Pagination(page = 0, size = 10))
                }

                verify { allRepositories() wasNot Called }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should change no data when viewing results`() {
                val pagination = Pagination(page = 0, size = 10)
                every { repository.findAvailableToJudge(refEq(pagination), VerdictFilter()) } returns
                    Page(content = listOf(testVerdict(10)), pagination = pagination, totalElements = 1)

                operations.viewResults(user = judge, pagination = pagination)

                verify(exactly = 0) {
                    repository.save(any<VerdictData>())
                    repository.update(any<Verdict>())
                }
                verify { listOf(submissionRepository, judgmentOrderRepository, userRepository, participantRepository) wasNot Called }
            }
        }

        @Test
        fun `should propagate the original technical exception`() {
            val pagination = Pagination(page = 0, size = 10)
            val failure = IllegalStateException("Storage unavailable")
            every { repository.findAvailableToJudge(refEq(pagination), VerdictFilter()) } throws failure

            val thrown = assertThrows(IllegalStateException::class.java) {
                operations.viewResults(user = judge, pagination = pagination)
            }

            assertSame(failure, thrown)
        }
    }

    @Nested
    inner class ChangeVerdictTests {

        private val submissionId = SubmissionId(2)
        private val score = Score(125)
        private val reason = "  Corrected after manual review\n"
        private val student = testStudent { data = studentData {} }

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should save exact judge submission score and reason and return the persisted judgment order for student`() {
                arrangeStudentSubmission()
                val savedData = slot<JudgmentOrderData>()
                val persisted = testJudgmentOrder()
                every { judgmentOrderRepository.save(capture(savedData)) } returns persisted

                val result = operations.changeVerdict(user = judge, submissionId = submissionId, score = score, reason = reason)
                    .getOrThrow()

                assertSame(persisted, result)
                assertEquals(judge.id, savedData.captured.judge.id)
                assertEquals(submissionId, savedData.captured.submission.id)
                assertEquals(Score(125), savedData.captured.score)
                assertEquals("  Corrected after manual review\n", savedData.captured.reason)
            }

            @Test
            fun `should allow a regular submission by an author with student and developer roles`() {
                arrangeStudentSubmission()
                every { userRepository.findById(student.id) } returns testMultipleRoleUser {
                    roles {
                        student { data = studentData {} }
                        developer { data = developerData {} }
                    }
                }
                val persisted = testJudgmentOrder()
                every { judgmentOrderRepository.save(any<JudgmentOrderData>()) } returns persisted

                val result = operations.changeVerdict(user = judge, submissionId = submissionId, score = score, reason = reason)
                    .getOrThrow()

                assertSame(persisted, result)
            }

            @Test
            fun `should save a judgment for a current participant submission`() {
                val author = participant {
                    id = 9
                    createdAt = Instant.MIN
                    data = participantData {
                        name = "Participant"
                        accessToken("token", algorithm = HashAlgorithm.Identity)
                        competition(1)
                    }
                }
                every { submissionRepository.findById(submissionId) } returns testSubmission { this.author = author.id }
                every { participantRepository.findById(author.id) } returns author
                val persisted = testJudgmentOrder()
                every { judgmentOrderRepository.save(any<JudgmentOrderData>()) } returns persisted

                val result = operations.changeVerdict(user = judge, submissionId = submissionId, score = score, reason = reason)
                    .getOrThrow()

                assertSame(persisted, result)
                verify(exactly = 1) {
                    judgmentOrderRepository.save(match<JudgmentOrderData> { it.submission.id == submissionId && it.judge.id == judge.id })
                }
            }

            @ParameterizedTest
            @ValueSource(ints = [0, Int.MAX_VALUE])
            fun `should accept scores at nonnegative integer boundaries`(value: Int) {
                arrangeStudentSubmission()
                every { judgmentOrderRepository.save(any<JudgmentOrderData>()) } answers { testJudgmentOrder(data = firstArg()) }

                val result = operations.changeVerdict(
                    user = judge,
                    submissionId = submissionId,
                    score = Score(value),
                    reason = "x",
                ).getOrThrow()

                assertEquals(Score(value), result.data.score)
                assertEquals("x", result.data.reason)
            }

            @Test
            fun `should save a new judgment order if the submission already has one with the same score`() {
                arrangeStudentSubmission(testSubmission { judgmentOrders(listOf(8)) })
                every { judgmentOrderRepository.save(any<JudgmentOrderData>()) } answers { testJudgmentOrder(data = firstArg()) }

                operations.changeVerdict(user = judge, submissionId = submissionId, score = score, reason = reason)

                verify(exactly = 1) {
                    judgmentOrderRepository.save(match<JudgmentOrderData> { it.submission.id == submissionId && it.score == score })
                }
                verify(exactly = 0) { judgmentOrderRepository.update(any<JudgmentOrder>()) }
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedJudgeRoleError if user has no roles`() {
                val user = testMultipleRoleUser {}

                assertRaises(MissedJudgeRoleError) {
                    operations.changeVerdict(user = user, submissionId = submissionId, score = score, reason = reason)
                }

                verify { submissionRepository wasNot Called }
                verify { judgmentOrderRepository wasNot Called }
            }

            @Test
            fun `should raise MissedJudgeRoleError without reading anything if user holds other roles but not the judge role`() {
                assertRaises(MissedJudgeRoleError) {
                    operations.changeVerdict(
                        user = userWithOtherRolesThanJudge(),
                        submissionId = submissionId,
                        score = score,
                        reason = reason,
                    )
                }

                verify { allRepositories() wasNot Called }
            }

            @Test
            fun `should raise SubmissionNotExistsError if submission is missing`() {
                every { submissionRepository.findById(submissionId) } returns null

                assertRefusal(SubmissionNotExistsError(submissionId))
            }

            @Test
            fun `should deny access when author no longer has the student role`() {
                arrangeStudentSubmission()
                every { userRepository.findById(student.id) } returns testMultipleRoleUser {}

                assertRefusal(SubmissionAccessDeniedError(submissionId))
            }

            @Test
            fun `should deny access when multiple role author is missing`() {
                arrangeStudentSubmission()
                every { userRepository.findById(student.id) } returns null

                assertRefusal(SubmissionAccessDeniedError(submissionId))
            }

            @Test
            fun `should deny access when fixed role author is not a current participant`() {
                val author = SingleRoleUserId(7)
                every { submissionRepository.findById(submissionId) } returns testSubmission { this.author = author }
                every { participantRepository.findById(author) } returns null

                assertRefusal(SubmissionAccessDeniedError(submissionId))
            }

            @Test
            fun `should raise SubmissionIsDeveloperSolutionTestError if a student author's submission tests a developer solution`() {
                val author = testMultipleRoleUser {
                    roles {
                        student { data = studentData {} }
                        developer { data = developerData {} }
                    }
                }
                every { userRepository.findById(author.id) } returns author
                every { submissionRepository.findById(submissionId) } returns testSubmission {
                    this.author = author.id
                    kind.developerSolutionTest { trikStudioVersion("3.0.0") }
                }

                assertRefusal(SubmissionIsDeveloperSolutionTestError(submissionId))
            }

            @Test
            fun `should raise SubmissionAccessDeniedError before the developer solution check if the author is not a student`() {
                val author = testDeveloper { data = developerData {} }
                every { userRepository.findById(author.id) } returns author
                every { submissionRepository.findById(submissionId) } returns testSubmission {
                    this.author = author.id
                    kind.developerSolutionTest { trikStudioVersion("3.0.0") }
                }

                assertRefusal(SubmissionAccessDeniedError(submissionId))
            }

            @Test
            fun `should raise SubmissionNotSuccessfullyGradedError if the submission is queued`() {
                arrangeStudentSubmission(testSubmission { status.queued() })

                assertRefusal(SubmissionNotSuccessfullyGradedError(submissionId))
            }

            @Test
            fun `should raise SubmissionNotSuccessfullyGradedError if the submission is in progress`() {
                arrangeStudentSubmission(testSubmission { status.inProgress() })

                assertRefusal(SubmissionNotSuccessfullyGradedError(submissionId))
            }

            @Test
            fun `should raise SubmissionNotSuccessfullyGradedError if grading ended with an error`() {
                arrangeStudentSubmission(testSubmission { status.graded { status.error { description = "Invalid solution" } } })

                assertRefusal(SubmissionNotSuccessfullyGradedError(submissionId))
            }

            @Test
            fun `should raise SubmissionNotSuccessfullyGradedError if grading timed out`() {
                arrangeStudentSubmission(testSubmission { status.graded { status.timeout() } })

                assertRefusal(SubmissionNotSuccessfullyGradedError(submissionId))
            }

            @ParameterizedTest
            @ValueSource(ints = [-1, Int.MIN_VALUE])
            fun `should raise NegativeJudgmentScoreError if the score is negative`(value: Int) {
                arrangeStudentSubmission()

                assertRefusal(NegativeJudgmentScoreError(Score(value)), requestedScore = Score(value))
            }

            @ParameterizedTest
            @ValueSource(strings = ["", " ", "\t\r\n", "  "])
            fun `should raise BlankJudgmentReasonError if the reason is empty or whitespace only`(value: String) {
                arrangeStudentSubmission()

                assertRefusal(BlankJudgmentReasonError, requestedReason = value)
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should keep earlier judgment orders, the submission and the automatic verdict unchanged`() {
                arrangeStudentSubmission(testSubmission { judgmentOrders(listOf(8)) })
                every { judgmentOrderRepository.save(any<JudgmentOrderData>()) } answers { testJudgmentOrder(data = firstArg()) }

                operations.changeVerdict(user = judge, submissionId = submissionId, score = score, reason = reason)

                verify(exactly = 0) {
                    judgmentOrderRepository.update(any<JudgmentOrder>())
                    submissionRepository.update(any<Submission>())
                }
                verify { repository wasNot Called }
            }
        }

        @Test
        fun `should propagate the original submission lookup exception`() {
            val failure = IllegalStateException("Submission storage unavailable")
            every { submissionRepository.findById(submissionId) } throws failure

            val thrown = assertThrows(IllegalStateException::class.java) {
                operations.changeVerdict(user = judge, submissionId = submissionId, score = score, reason = reason)
            }

            assertSame(failure, thrown)
            verify { judgmentOrderRepository wasNot Called }
        }

        @Test
        fun `should propagate the original author lookup exception`() {
            arrangeStudentSubmission()
            val failure = IllegalStateException("User storage unavailable")
            every { userRepository.findById(student.id) } throws failure

            val thrown = assertThrows(IllegalStateException::class.java) {
                operations.changeVerdict(user = judge, submissionId = submissionId, score = score, reason = reason)
            }

            assertSame(failure, thrown)
            verify { judgmentOrderRepository wasNot Called }
        }

        @Test
        fun `should propagate the original judgment save exception`() {
            arrangeStudentSubmission()
            val failure = IllegalStateException("Judgment storage unavailable")
            every { judgmentOrderRepository.save(any<JudgmentOrderData>()) } throws failure

            val thrown = assertThrows(IllegalStateException::class.java) {
                operations.changeVerdict(user = judge, submissionId = submissionId, score = score, reason = reason)
            }

            assertSame(failure, thrown)
        }

        @Test
        fun `should propagate the original participant lookup exception`() {
            val authorId = SingleRoleUserId(9)
            every { submissionRepository.findById(submissionId) } returns testSubmission { author = authorId }
            val failure = IllegalStateException("Participant storage unavailable")
            every { participantRepository.findById(authorId) } throws failure

            val thrown = assertThrows(IllegalStateException::class.java) {
                operations.changeVerdict(user = judge, submissionId = submissionId, score = score, reason = reason)
            }

            assertSame(failure, thrown)
            verify { judgmentOrderRepository wasNot Called }
        }

        private fun arrangeStudentSubmission(value: Submission = testSubmission()) {
            every { submissionRepository.findById(submissionId) } returns value
            every { userRepository.findById(student.id) } returns student
        }

        private fun assertRefusal(expected: ChangeVerdictError, requestedScore: Score = score, requestedReason: String = reason) {
            assertRaises(expected) {
                operations.changeVerdict(user = judge, submissionId = submissionId, score = requestedScore, reason = requestedReason)
            }
            verify { judgmentOrderRepository wasNot Called }
        }

        private fun testSubmission(builder: SubmissionDataBuilder.() -> Unit = {}): Submission = submission {
            id = 2
            createdAt = Instant.MIN
            data = submissionData {
                author = student.id
                solution(3)
                task(1)
                status.graded { status.success { verdict(4) } }
                kind.grading { contest(1) }
                builder()
            }
        }

        private fun testJudgmentOrder(
            data: JudgmentOrderData = judgmentOrderData {
                judge = this@JudgeOperationsTests.judge.id
                submission = submissionId
                score = this@ChangeVerdictTests.score.value
                reason = this@ChangeVerdictTests.reason
            },
        ): JudgmentOrder = judgmentOrder {
            id = 10
            createdAt = Instant.parse("2026-02-01T00:00:00Z")
            version = EntityVersion(7)
            this.data = data
        }
    }

    private fun testVerdict(verdictId: Long): Verdict = verdict {
        id = verdictId
        createdAt = Instant.parse("2026-01-01T00:00:00Z")
        data = verdictData {
            task(1)
            submission(2)
            testVerdict {
                score = 75
                test(10)
                logs(30)
                recording(40)
            }
            testVerdict {
                score = 0
                test(11)
                logs(31)
            }
        }
    }

    companion object {
        @JvmStatic
        fun authorIds(): List<UserId> = listOf(MultipleRoleUserId(7), SingleRoleUserId(9))
    }
}
