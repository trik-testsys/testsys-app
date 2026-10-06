package tech.testsys.operation.user

import io.mockk.Called
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.judgeData
import tech.testsys.domain.builder.api.judgmentOrder
import tech.testsys.domain.builder.api.judgmentOrderData
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
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderData
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.operation.error.BlankJudgmentReasonError
import tech.testsys.operation.error.ChangeVerdictError
import tech.testsys.operation.error.MissedJudgeRoleError
import tech.testsys.operation.error.NegativeJudgmentScoreError
import tech.testsys.operation.error.SubmissionAccessDeniedError
import tech.testsys.operation.error.SubmissionNotExistsError
import tech.testsys.operation.error.SubmissionNotSuccessfullyGradedError
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
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

    @Nested
    inner class ViewResultsTests {

        @Test
        fun `should raise MissedJudgeRoleError before querying if user is not a judge`() {
            val user = testMultipleRoleUser {}

            assertRaises(MissedJudgeRoleError) { operations.viewResults(user = user, pagination = Pagination(page = 0, size = 10)) }

            verify { repository wasNot Called }
        }

        @Test
        fun `should return the page with scores and lazy references unchanged`() {
            val pagination = Pagination(page = 1, size = 2)
            val responsePagination = pagination.copy(sort = Sort(listOf(Sort.Order("storageOrder"))))
            val first = testVerdict(10)
            val second = testVerdict(20)
            val page = Page(content = listOf(first, second), pagination = responsePagination, totalElements = 5)
            every { repository.findAvailableToJudge(refEq(pagination), VerdictFilter()) } returns page

            val result = operations.viewResults(user = judge, pagination = pagination).getOrThrow()

            assertSame(page, result)
            assertSame(first, result.content[0])
            assertEquals(listOf(75, 0), result.content[0].data.testVerdicts.map { it.score.value })
            assertEquals(listOf(30L, 31L), result.content[0].data.testVerdicts.map { it.logs.id.value })
            assertEquals(40L, result.content[0].data.testVerdicts[0].recording?.id?.value)
            assertEquals(null, result.content[0].data.testVerdicts[1].recording)
            assertEquals(3, result.totalPages)
            assertEquals(true, result.hasNext)
            verify(exactly = 1) { repository.findAvailableToJudge(refEq(pagination), VerdictFilter()) }
        }

        @Test
        fun `should pass a student user id filter and retain the requested page`() {
            val authorId = MultipleRoleUserId(7)
            val pagination = Pagination(page = 3, size = 4)
            val page = Page<Verdict>(content = emptyList(), pagination = pagination, totalElements = 2)
            every { repository.findAvailableToJudge(refEq(pagination), VerdictFilter(authorId = authorId)) } returns page

            val result = operations.viewResults(
                user = judge,
                pagination = pagination,
                filter = VerdictFilter(authorId = authorId),
            ).getOrThrow()

            assertSame(page, result)
            assertFalse(result.hasNext)
            verify(exactly = 1) { repository.findAvailableToJudge(refEq(pagination), VerdictFilter(authorId = authorId)) }
        }

        @Test
        fun `should pass a participant user id and preserve the requested pagination`() {
            val authorId = SingleRoleUserId(9)
            val pagination = Pagination(page = 0, size = 1)
            val page = Page(content = listOf(testVerdict(1)), pagination = pagination, totalElements = 1)
            every { repository.findAvailableToJudge(pagination, VerdictFilter(authorId = authorId)) } returns page

            val result = operations.viewResults(
                user = judge,
                pagination = pagination,
                filter = VerdictFilter(authorId = authorId),
            ).getOrThrow()

            assertSame(page, result)
            verify(exactly = 1) { repository.findAvailableToJudge(pagination, VerdictFilter(authorId = authorId)) }
        }

        @Test
        fun `should return an empty page without a domain error`() {
            val pagination = Pagination(page = 0, size = 10)
            val page = Page<Verdict>(content = emptyList(), pagination = pagination, totalElements = 0)
            every { repository.findAvailableToJudge(page.pagination, VerdictFilter()) } returns page

            val result = operations.viewResults(user = judge, pagination = pagination).getOrThrow()

            assertSame(page, result)
            assertEquals(0, result.totalPages)
            assertFalse(result.hasNext)
        }

        @Test
        fun `should forward arbitrary sorting metadata unchanged to the repository`() {
            val pagination =
                Pagination(page = 0, size = 10, sort = Sort(listOf(Sort.Order("storageField", Sort.Direction.DESC))))
            val page = Page<Verdict>(content = emptyList(), pagination = pagination, totalElements = 0)
            every { repository.findAvailableToJudge(refEq(pagination), VerdictFilter()) } returns page

            val result = operations.viewResults(user = judge, pagination = pagination).getOrThrow()

            assertSame(page, result)
            verify(exactly = 1) { repository.findAvailableToJudge(refEq(pagination), VerdictFilter()) }
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

        @Test
        fun `should raise MissedJudgeRoleError if user is not a judge`() {
            val user = testMultipleRoleUser {}

            assertRaises(MissedJudgeRoleError) {
                operations.changeVerdict(user = user, submissionId = submissionId, score = score, reason = reason)
            }

            verify { submissionRepository wasNot Called }
            verify { judgmentOrderRepository wasNot Called }
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

        @ParameterizedTest
        @ValueSource(longs = [7, 8])
        fun `should deny access when fixed role author is not a current participant`(authorId: Long) {
            val author = SingleRoleUserId(authorId)
            every { submissionRepository.findById(submissionId) } returns testSubmission { this.author = author }
            every { participantRepository.findById(author) } returns null

            assertRefusal(SubmissionAccessDeniedError(submissionId))
        }

        @Test
        fun `should deny developer solution tests even when author has student and developer roles`() {
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

            assertRefusal(SubmissionAccessDeniedError(submissionId))
        }

        @Test
        fun `should reject a queued submission`() {
            arrangeStudentSubmission(testSubmission { status.queued() })

            assertRefusal(SubmissionNotSuccessfullyGradedError(submissionId))
        }

        @Test
        fun `should reject an in progress submission`() {
            arrangeStudentSubmission(testSubmission { status.inProgress() })

            assertRefusal(SubmissionNotSuccessfullyGradedError(submissionId))
        }

        @Test
        fun `should reject grading errors`() {
            arrangeStudentSubmission(testSubmission { status.graded { status.error { description = "Invalid solution" } } })

            assertRefusal(SubmissionNotSuccessfullyGradedError(submissionId))
        }

        @Test
        fun `should reject grading timeouts`() {
            arrangeStudentSubmission(testSubmission { status.graded { status.timeout() } })

            assertRefusal(SubmissionNotSuccessfullyGradedError(submissionId))
        }

        @ParameterizedTest
        @ValueSource(ints = [-1, Int.MIN_VALUE])
        fun `should reject negative judgment scores`(value: Int) {
            arrangeStudentSubmission()

            assertRefusal(NegativeJudgmentScoreError(Score(value)), requestedScore = Score(value))
        }

        @ParameterizedTest
        @ValueSource(strings = ["", " ", "\t\r\n", "\u00a0\u2003"])
        fun `should reject empty and whitespace only reasons`(value: String) {
            arrangeStudentSubmission()

            assertRefusal(BlankJudgmentReasonError, requestedReason = value)
        }

        @Test
        fun `should save exact judge submission score and reason and return persisted metadata for student`() {
            arrangeStudentSubmission()
            val savedData = slot<JudgmentOrderData>()
            val persisted = testJudgmentOrder()
            every { judgmentOrderRepository.save(capture(savedData)) } returns persisted

            val result = operations.changeVerdict(user = judge, submissionId = submissionId, score = score, reason = reason).getOrThrow()

            assertSame(persisted, result)
            assertEquals(10L, result.id.value)
            assertEquals(Instant.parse("2026-02-01T00:00:00Z"), result.createdAt)
            assertEquals(EntityVersion(7), result.version)
            assertEquals(judge.id, savedData.captured.judge.id)
            assertEquals(submissionId, savedData.captured.submission.id)
            assertEquals(Score(125), savedData.captured.score)
            assertEquals("  Corrected after manual review\n", savedData.captured.reason)
            verify(exactly = 1) { judgmentOrderRepository.save(any<JudgmentOrderData>()) }
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

            val result = operations.changeVerdict(user = judge, submissionId = submissionId, score = score, reason = reason).getOrThrow()

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

            val result = operations.changeVerdict(user = judge, submissionId = submissionId, score = score, reason = reason).getOrThrow()

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
        fun `should append identical judgments while preserving earlier history submission and automatic verdict`() {
            val originalSubmission = testSubmission { judgmentOrders(listOf(8)) }
            arrangeStudentSubmission(originalSubmission)
            val previous = testJudgmentOrder(
                orderId = 8,
                data = judgmentOrderData {
                    judge(19)
                    submission = submissionId
                    score = 50
                    reason = "Earlier ruling"
                },
            )
            val history = mutableListOf(previous)
            val savedData = mutableListOf<JudgmentOrderData>()
            every { judgmentOrderRepository.save(capture(savedData)) } answers {
                val saved = testJudgmentOrder(orderId = history.size.toLong() + 9, data = firstArg())
                history.add(saved)
                saved
            }

            val firstResult = operations.changeVerdict(
                user = judge,
                submissionId = submissionId,
                score = score,
                reason = reason,
            ).getOrThrow()
            val secondResult = operations.changeVerdict(
                user = judge,
                submissionId = submissionId,
                score = score,
                reason = reason,
            ).getOrThrow()

            assertEquals(10L, firstResult.id.value)
            assertEquals(11L, secondResult.id.value)
            assertEquals(listOf(8L, 10L, 11L), history.map { it.id.value })
            assertSame(previous, history[0])
            assertEquals(listOf(Score(125), Score(125)), savedData.map { it.score })
            assertEquals(listOf(submissionId, submissionId), savedData.map { it.submission.id })
            assertEquals(listOf(reason, reason), savedData.map { it.reason })
            assertEquals(Score(50), previous.data.score)
            assertEquals("Earlier ruling", previous.data.reason)
            assertEquals(listOf(8L), originalSubmission.data.judgmentOrders.ids.map { it.value })
            verify(exactly = 2) { judgmentOrderRepository.save(any<JudgmentOrderData>()) }
            verify(exactly = 0) { judgmentOrderRepository.update(any<JudgmentOrder>()) }
            verify(exactly = 0) { submissionRepository.update(any<Submission>()) }
            verify(exactly = 2) { submissionRepository.findById(submissionId) }
            verify { repository wasNot Called }
            confirmVerified(judgmentOrderRepository, submissionRepository)
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
            orderId: Long = 10,
            data: JudgmentOrderData = judgmentOrderData {
                judge = this@JudgeOperationsTests.judge.id
                submission = submissionId
                score = this@ChangeVerdictTests.score.value
                reason = this@ChangeVerdictTests.reason
            },
        ): JudgmentOrder = judgmentOrder {
            id = orderId
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
}
