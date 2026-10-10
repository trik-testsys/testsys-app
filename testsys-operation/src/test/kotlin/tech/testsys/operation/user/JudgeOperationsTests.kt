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
import tech.testsys.domain.builder.api.logs
import tech.testsys.domain.builder.api.logsData
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.builder.api.multipleRoleUser
import tech.testsys.domain.builder.api.multipleRoleUserData
import tech.testsys.domain.builder.api.participant
import tech.testsys.domain.builder.api.participantData
import tech.testsys.domain.builder.api.recording
import tech.testsys.domain.builder.api.recordingData
import tech.testsys.domain.builder.api.solution
import tech.testsys.domain.builder.api.solutionData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.api.submission
import tech.testsys.domain.builder.api.submissionData
import tech.testsys.domain.builder.api.testData
import tech.testsys.domain.builder.api.verdict
import tech.testsys.domain.builder.api.verdictData
import tech.testsys.domain.builder.task.SubmissionDataBuilder
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.contract.persistence.VerdictFilter
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.JudgmentOrderRepository
import tech.testsys.domain.contract.persistence.repository.LogsRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.RecordingRepository
import tech.testsys.domain.contract.persistence.repository.SolutionRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderData
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.Logs
import tech.testsys.domain.model.task.LogsId
import tech.testsys.domain.model.task.Recording
import tech.testsys.domain.model.task.RecordingId
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId
import tech.testsys.operation.error.BlankJudgmentReasonError
import tech.testsys.operation.error.ChangeVerdictError
import tech.testsys.operation.error.MissedJudgeRoleError
import tech.testsys.operation.error.NegativeJudgmentScoreError
import tech.testsys.operation.error.RecordingNotExistsError
import tech.testsys.operation.error.SubmissionAccessDeniedError
import tech.testsys.operation.error.SubmissionIsDeveloperSolutionTestError
import tech.testsys.operation.error.SubmissionNotExistsError
import tech.testsys.operation.error.SubmissionNotSuccessfullyGradedError
import tech.testsys.operation.error.TestNotInVerdictError
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.testContest
import tech.testsys.operation.util.testDeveloper
import tech.testsys.operation.util.testJudge
import tech.testsys.operation.util.testMultipleRoleUser
import tech.testsys.operation.util.testNewTask
import tech.testsys.operation.util.testParticipant
import tech.testsys.operation.util.testStudent
import java.time.Instant
import java.util.UUID
import tech.testsys.domain.builder.api.test as polygon
import tech.testsys.domain.model.task.Test as Polygon

class JudgeOperationsTests {

    private val repository = mockk<VerdictRepository>()
    private val submissionRepository = mockk<SubmissionRepository>()
    private val judgmentOrderRepository = mockk<JudgmentOrderRepository>()
    private val userRepository = mockk<MultipleRoleUserRepository>()
    private val participantRepository = mockk<ParticipantRepository>()
    private val taskRepository = mockk<TaskRepository>()
    private val contestRepository = mockk<ContestRepository>()
    private val solutionRepository = mockk<SolutionRepository>()
    private val testRepository = mockk<TestRepository>()
    private val logsRepository = mockk<LogsRepository>()
    private val recordingRepository = mockk<RecordingRepository>()
    private val operations = JudgeOperations(
        verdictRepository = repository,
        submissionRepository = submissionRepository,
        judgmentOrderRepository = judgmentOrderRepository,
        multipleRoleUserRepository = userRepository,
        participantRepository = participantRepository,
        taskRepository = taskRepository,
        contestRepository = contestRepository,
        solutionRepository = solutionRepository,
        testRepository = testRepository,
        logsRepository = logsRepository,
        recordingRepository = recordingRepository,
    )
    private val judge = testJudge { data = judgeData {} }
    private val studentAuthor = testMultipleRoleUser {
        name = "Student"
        roles { student { data = studentData {} } }
    }
    private val participantAuthor = testParticipant { name = "Participant" }

    /** Verdict 4 of submission 2: test 11 scored 75 with logs 31 and recording 41, then test 10 scored 25 with logs 30. */
    private val gradedVerdict = verdict {
        id = 4
        createdAt = Instant.parse("2026-01-01T00:00:00Z")
        data = verdictData {
            task(1)
            submission(SUBMISSION_ID.value)
            testVerdict {
                score = 75
                test(11)
                logs(31)
                recording(41)
            }
            testVerdict {
                score = 25
                test(10)
                logs(30)
            }
        }
    }

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

                assertEquals(
                    Page<Pair<Verdict, User<*>>>(
                        content = page.content.map { it to studentAuthor },
                        pagination = page.pagination,
                        totalElements = page.totalElements,
                    ),
                    result,
                )
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
                arrangeAuthor(studentAuthor)
                every { repository.findAvailableToJudge(refEq(pagination), VerdictFilter()) } returns page

                val result = operations.viewResults(user = judge, pagination = pagination).getOrThrow()

                assertEquals(
                    Page<Pair<Verdict, User<*>>>(
                        content = page.content.map { it to studentAuthor },
                        pagination = page.pagination,
                        totalElements = page.totalElements,
                    ),
                    result,
                )
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

                assertEquals(
                    Page<Pair<Verdict, User<*>>>(
                        content = page.content.map { it to studentAuthor },
                        pagination = page.pagination,
                        totalElements = page.totalElements,
                    ),
                    result,
                )
            }

            @Test
            fun `should forward the requested sorting to the repository unchanged`() {
                val pagination =
                    Pagination(page = 0, size = 10, sort = Sort(listOf(Sort.Order("storageField", Sort.Direction.DESC))))
                val page = Page<Verdict>(content = emptyList(), pagination = pagination, totalElements = 0)
                every { repository.findAvailableToJudge(refEq(pagination), VerdictFilter()) } returns page

                val result = operations.viewResults(user = judge, pagination = pagination).getOrThrow()

                assertEquals(
                    Page<Pair<Verdict, User<*>>>(
                        content = page.content.map { it to studentAuthor },
                        pagination = page.pagination,
                        totalElements = page.totalElements,
                    ),
                    result,
                )
            }

            @Test
            fun `should return an empty page without a domain error if no verdicts match`() {
                val pagination = Pagination(page = 0, size = 10)
                val page = Page<Verdict>(content = emptyList(), pagination = pagination, totalElements = 0)
                every { repository.findAvailableToJudge(page.pagination, VerdictFilter()) } returns page

                val result = operations.viewResults(user = judge, pagination = pagination).getOrThrow()

                assertEquals(
                    Page<Pair<Verdict, User<*>>>(
                        content = page.content.map { it to studentAuthor },
                        pagination = page.pagination,
                        totalElements = page.totalElements,
                    ),
                    result,
                )
            }

            @Test
            fun `should return the student author of the submission of each verdict`() {
                val pagination = Pagination(page = 0, size = 10)
                every { repository.findAvailableToJudge(refEq(pagination), VerdictFilter()) } returns
                    Page(content = listOf(testVerdict(10)), pagination = pagination, totalElements = 1)
                arrangeAuthor(studentAuthor)

                val result = operations.viewResults(user = judge, pagination = pagination).getOrThrow()

                assertSame(studentAuthor, result.content.single().second)
            }

            @Test
            fun `should return the participant author of the submission of each verdict`() {
                val pagination = Pagination(page = 0, size = 10)
                every { repository.findAvailableToJudge(refEq(pagination), VerdictFilter()) } returns
                    Page(content = listOf(testVerdict(10)), pagination = pagination, totalElements = 1)
                arrangeAuthor(participantAuthor)

                val result = operations.viewResults(user = judge, pagination = pagination).getOrThrow()

                assertSame(participantAuthor, result.content.single().second)
            }

            @Test
            fun `should return the page with scores and lazy references unchanged`() {
                val pagination = Pagination(page = 1, size = 2)
                val responsePagination = pagination.copy(sort = Sort(listOf(Sort.Order("storageOrder"))))
                val first = testVerdict(10)
                val second = testVerdict(20)
                val page = Page(content = listOf(first, second), pagination = responsePagination, totalElements = 5)
                every { repository.findAvailableToJudge(refEq(pagination), VerdictFilter()) } returns page
                arrangeAuthor(studentAuthor)

                val result = operations.viewResults(user = judge, pagination = pagination).getOrThrow()

                assertEquals(listOf(first, second), result.content.map { (verdict, _) -> verdict })
                assertSame(first, result.content[0].first)
                assertEquals(listOf(75, 0), result.content[0].first.data.testVerdicts.map { it.score.value })
                assertEquals(listOf(30L, 31L), result.content[0].first.data.testVerdicts.map { it.logs.id.value })
                assertEquals(40L, result.content[0].first.data.testVerdicts[0].recording?.id?.value)
                assertEquals(null, result.content[0].first.data.testVerdicts[1].recording)
                assertEquals(responsePagination, result.pagination)
                assertEquals(3, result.totalPages)
                assertEquals(true, result.hasNext)
                verify(exactly = 1) { repository.findAvailableToJudge(refEq(pagination), VerdictFilter()) }
            }

            @Test
            fun `should pass a participant user id and preserve the requested pagination`() {
                val authorId = SingleRoleUserId(9)
                val pagination = Pagination(page = 0, size = 1)
                val verdict = testVerdict(1)
                val page = Page(content = listOf(verdict), pagination = pagination, totalElements = 1)
                every { repository.findAvailableToJudge(pagination, VerdictFilter(authorId = authorId)) } returns page
                arrangeAuthor(participantAuthor)

                val result = operations.viewResults(
                    user = judge,
                    pagination = pagination,
                    filter = VerdictFilter(authorId = authorId),
                ).getOrThrow()

                assertEquals(Page(content = listOf(verdict to participantAuthor), pagination = pagination, totalElements = 1), result)
                verify(exactly = 1) { repository.findAvailableToJudge(pagination, VerdictFilter(authorId = authorId)) }
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

                arrangeAuthor(studentAuthor)

                operations.viewResults(user = judge, pagination = pagination)

                verify(exactly = 0) {
                    repository.save(any<VerdictData>())
                    repository.update(any<Verdict>())
                }
                verify { judgmentOrderRepository wasNot Called }
                verify(exactly = 0) {
                    submissionRepository.update(any<Submission>())
                    userRepository.update(any<MultipleRoleUser>())
                }
            }
        }

        @Nested
        inner class ModuleRuleTests {

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

        private fun arrangeAuthor(author: User<*>) {
            every { submissionRepository.load(any<LazyEntityList<SubmissionId, Submission>>()) } returns listOf(gradedSubmission(author.id))
            every { userRepository.findByIds(any()) } returns listOfNotNull(author as? MultipleRoleUser)
            every { participantRepository.findByIds(any()) } returns listOfNotNull(author as? Participant)
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

        @Nested
        inner class ModuleRuleTests {

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

    @Nested
    inner class ViewSolutionTests {

        private val task = testNewTask()
        private val contest = testContest()
        private val solution = testSolution()
        private val firstPolygon = testPolygon(10)
        private val secondPolygon = testPolygon(11)

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should return a queued submission without a verdict, tests and final score`() {
                val queued = gradedSubmission(studentAuthor.id) { status.queued() }
                arrangeDetails(queued)

                val result = operations.viewSolution(user = judge, submissionId = SUBMISSION_ID).getOrThrow()

                assertEquals(
                    SubmissionDetails(
                        submission = queued,
                        author = studentAuthor,
                        task = task,
                        contest = contest,
                        solution = solution,
                        verdict = null,
                        tests = emptyList(),
                        finalScore = null,
                        judgmentOrders = emptyList(),
                    ),
                    result,
                )
            }

            @Test
            fun `should return no verdict and no final score after a grading error`() {
                arrangeDetails(gradedSubmission(studentAuthor.id) { status.graded { status.error { description = "Invalid solution" } } })

                val result = operations.viewSolution(user = judge, submissionId = SUBMISSION_ID).getOrThrow()

                assertEquals(null, result.verdict)
                assertEquals(null, result.finalScore)
            }

            @Test
            fun `should return the verdict with its tests in outcome order and the total score as the final score`() {
                arrangeDetails(gradedSubmission(studentAuthor.id))

                val result = operations.viewSolution(user = judge, submissionId = SUBMISSION_ID).getOrThrow()

                assertSame(gradedVerdict, result.verdict)
                assertEquals(listOf(secondPolygon, firstPolygon), result.tests)
                assertEquals(100L, result.finalScore)
            }

            @Test
            fun `should take the final score from the last judgment order deciding equal moments by id`() {
                arrangeJudgmentOrders()

                val result = operations.viewSolution(user = judge, submissionId = SUBMISSION_ID).getOrThrow()

                assertEquals(90L, result.finalScore)
            }

            @Test
            fun `should return judgment orders in the order of issue with their judges`() {
                val (firstJudge, secondJudge) = arrangeJudgmentOrders()

                val result = operations.viewSolution(user = judge, submissionId = SUBMISSION_ID).getOrThrow()

                assertEquals(listOf(8L, 6L, 7L), result.judgmentOrders.map { (order, _) -> order.id.value })
                assertEquals(listOf(firstJudge, secondJudge, firstJudge), result.judgmentOrders.map { (_, issuer) -> issuer })
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedJudgeRoleError before reading the submission if user is not a judge`() {
                val user = testMultipleRoleUser {}

                assertRaises(MissedJudgeRoleError) { operations.viewSolution(user = user, submissionId = SUBMISSION_ID) }

                verify { submissionRepository wasNot Called }
            }

            @Test
            fun `should raise SubmissionNotExistsError if submission is missing`() {
                every { submissionRepository.findById(SUBMISSION_ID) } returns null

                assertRaises(SubmissionNotExistsError(SUBMISSION_ID)) {
                    operations.viewSolution(user = judge, submissionId = SUBMISSION_ID)
                }
            }

            @Test
            fun `should deny access to a developer solution test`() {
                arrangeSubmission(gradedSubmission(studentAuthor.id) { kind.developerSolutionTest { trikStudioVersion("3.0.0") } })

                assertRaises(SubmissionAccessDeniedError(SUBMISSION_ID)) {
                    operations.viewSolution(user = judge, submissionId = SUBMISSION_ID)
                }
            }

            @Test
            fun `should deny access when author no longer has the student role`() {
                arrangeSubmission()
                every { userRepository.findById(studentAuthor.id) } returns testMultipleRoleUser {}

                assertRaises(SubmissionAccessDeniedError(SUBMISSION_ID)) {
                    operations.viewSolution(user = judge, submissionId = SUBMISSION_ID)
                }
            }
        }

        /** Arranges three orders: 8 issued first, then 6 and 7 at the same moment; returns their two judges. */
        private fun arrangeJudgmentOrders(): Pair<MultipleRoleUser, MultipleRoleUser> {
            arrangeDetails(gradedSubmission(studentAuthor.id) { judgmentOrders(listOf(7, 6, 8)) })
            val firstJudge = testJudgeWithId(21)
            val secondJudge = testJudgeWithId(22)
            val later = Instant.parse("2026-03-02T00:00:00Z")
            every { judgmentOrderRepository.findByIds(listOf(JudgmentOrderId(7), JudgmentOrderId(6), JudgmentOrderId(8))) } returns listOf(
                testOrder(orderId = 7, judgeId = 21, issuedAt = later, score = 90),
                testOrder(orderId = 6, judgeId = 22, issuedAt = later, score = 30),
                testOrder(orderId = 8, judgeId = 21, issuedAt = Instant.parse("2026-03-01T00:00:00Z"), score = 50),
            )
            every {
                userRepository.findByIds(match<List<MultipleRoleUserId>> { ids -> ids.toSet() == setOf(firstJudge.id, secondJudge.id) })
            } returns listOf(firstJudge, secondJudge)
            return firstJudge to secondJudge
        }

        private fun arrangeDetails(submission: Submission) {
            arrangeSubmission(submission)
            every { taskRepository.load(any<LazyEntity<TaskId, Task>>()) } returns task
            every { contestRepository.load(any<LazyEntity<ContestId, Contest>>()) } returns contest
            every { solutionRepository.load(any<LazyEntity<SolutionId, Solution>>()) } returns solution
            every { testRepository.load(any<LazyEntityList<TestId, Polygon>>()) } returns listOf(firstPolygon, secondPolygon)
            every { judgmentOrderRepository.findByIds(emptyList()) } returns emptyList()
            every { userRepository.findByIds(emptyList()) } returns emptyList()
        }
    }

    @Nested
    inner class DownloadSolutionTests {

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should return the file of the solution of the submission`() {
                arrangeSubmission()
                val solution = testSolution()
                every {
                    solutionRepository.load(match<LazyEntity<SolutionId, Solution>> { reference -> reference.id == SolutionId(3) })
                } returns solution

                val result = operations.downloadSolution(user = judge, submissionId = SUBMISSION_ID).getOrThrow()

                assertSame(solution.data.file, result)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedJudgeRoleError if user is not a judge`() {
                assertRaises(MissedJudgeRoleError) {
                    operations.downloadSolution(user = testMultipleRoleUser {}, submissionId = SUBMISSION_ID)
                }

                verify { submissionRepository wasNot Called }
            }

            @Test
            fun `should raise SubmissionNotExistsError if submission is missing`() {
                every { submissionRepository.findById(SUBMISSION_ID) } returns null

                assertRaises(SubmissionNotExistsError(SUBMISSION_ID)) {
                    operations.downloadSolution(user = judge, submissionId = SUBMISSION_ID)
                }
            }

            @Test
            fun `should deny access to a developer solution test`() {
                arrangeSubmission(gradedSubmission(studentAuthor.id) { kind.developerSolutionTest { trikStudioVersion("3.0.0") } })

                assertRaises(SubmissionAccessDeniedError(SUBMISSION_ID)) {
                    operations.downloadSolution(user = judge, submissionId = SUBMISSION_ID)
                }
                verify { solutionRepository wasNot Called }
            }

            @Test
            fun `should deny access when the participant author no longer exists`() {
                arrangeSubmission(gradedSubmission(participantAuthor.id))
                every { participantRepository.findById(participantAuthor.id) } returns null

                assertRaises(SubmissionAccessDeniedError(SUBMISSION_ID)) {
                    operations.downloadSolution(user = judge, submissionId = SUBMISSION_ID)
                }
            }
        }
    }

    @Nested
    inner class DownloadLogsTests {

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should return the logs of the run on the test`() {
                arrangeSubmission()
                val logs = testLogs(30)
                every { logsRepository.load(match<LazyEntity<LogsId, Logs>> { reference -> reference.id == LogsId(30) }) } returns logs

                val result = operations.downloadLogs(user = judge, submissionId = SUBMISSION_ID, testId = TestId(10)).getOrThrow()

                assertSame(logs.data.file, result)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedJudgeRoleError if user is not a judge`() {
                assertRaises(MissedJudgeRoleError) {
                    operations.downloadLogs(user = testMultipleRoleUser {}, submissionId = SUBMISSION_ID, testId = TestId(10))
                }

                verify { submissionRepository wasNot Called }
            }

            @Test
            fun `should raise SubmissionNotExistsError if submission is missing`() {
                every { submissionRepository.findById(SUBMISSION_ID) } returns null

                assertRaises(SubmissionNotExistsError(SUBMISSION_ID)) {
                    operations.downloadLogs(user = judge, submissionId = SUBMISSION_ID, testId = TestId(10))
                }
            }

            @Test
            fun `should deny access when author no longer has the student role`() {
                arrangeSubmission()
                every { userRepository.findById(studentAuthor.id) } returns testMultipleRoleUser {}

                assertRaises(SubmissionAccessDeniedError(SUBMISSION_ID)) {
                    operations.downloadLogs(user = judge, submissionId = SUBMISSION_ID, testId = TestId(10))
                }
            }

            @Test
            fun `should reject a submission without a successful verdict`() {
                arrangeSubmission(gradedSubmission(studentAuthor.id) { status.inProgress() })

                assertRaises(SubmissionNotSuccessfullyGradedError(SUBMISSION_ID)) {
                    operations.downloadLogs(user = judge, submissionId = SUBMISSION_ID, testId = TestId(10))
                }
            }

            @Test
            fun `should reject a test outside the verdict`() {
                arrangeSubmission()

                assertRaises(TestNotInVerdictError(SUBMISSION_ID, TestId(12))) {
                    operations.downloadLogs(user = judge, submissionId = SUBMISSION_ID, testId = TestId(12))
                }
                verify { logsRepository wasNot Called }
            }
        }
    }

    @Nested
    inner class DownloadRecordingTests {

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should return the recording of the run on the test`() {
                arrangeSubmission()
                val recording = testRecording(41)
                every {
                    recordingRepository.load(match<LazyEntity<RecordingId, Recording>> { reference -> reference.id == RecordingId(41) })
                } returns recording

                val result = operations.downloadRecording(user = judge, submissionId = SUBMISSION_ID, testId = TestId(11)).getOrThrow()

                assertSame(recording.data.file, result)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedJudgeRoleError if user is not a judge`() {
                assertRaises(MissedJudgeRoleError) {
                    operations.downloadRecording(user = testMultipleRoleUser {}, submissionId = SUBMISSION_ID, testId = TestId(11))
                }

                verify { submissionRepository wasNot Called }
            }

            @Test
            fun `should raise SubmissionNotExistsError if submission is missing`() {
                every { submissionRepository.findById(SUBMISSION_ID) } returns null

                assertRaises(SubmissionNotExistsError(SUBMISSION_ID)) {
                    operations.downloadRecording(user = judge, submissionId = SUBMISSION_ID, testId = TestId(11))
                }
            }

            @Test
            fun `should deny access to a developer solution test`() {
                arrangeSubmission(gradedSubmission(studentAuthor.id) { kind.developerSolutionTest { trikStudioVersion("3.0.0") } })

                assertRaises(SubmissionAccessDeniedError(SUBMISSION_ID)) {
                    operations.downloadRecording(user = judge, submissionId = SUBMISSION_ID, testId = TestId(11))
                }
            }

            @Test
            fun `should reject a submission whose grading timed out`() {
                arrangeSubmission(gradedSubmission(studentAuthor.id) { status.graded { status.timeout() } })

                assertRaises(SubmissionNotSuccessfullyGradedError(SUBMISSION_ID)) {
                    operations.downloadRecording(user = judge, submissionId = SUBMISSION_ID, testId = TestId(11))
                }
            }

            @Test
            fun `should reject a test outside the verdict`() {
                arrangeSubmission()

                assertRaises(TestNotInVerdictError(SUBMISSION_ID, TestId(12))) {
                    operations.downloadRecording(user = judge, submissionId = SUBMISSION_ID, testId = TestId(12))
                }
            }

            @Test
            fun `should raise RecordingNotExistsError if the run on the test has no recording`() {
                arrangeSubmission()

                assertRaises(RecordingNotExistsError(SUBMISSION_ID, TestId(10))) {
                    operations.downloadRecording(user = judge, submissionId = SUBMISSION_ID, testId = TestId(10))
                }
                verify { recordingRepository wasNot Called }
            }
        }
    }

    /** Makes [submission] by [studentAuthor] stored with [gradedVerdict] as its verdict. */
    private fun arrangeSubmission(submission: Submission = gradedSubmission(studentAuthor.id)) {
        every { submissionRepository.findById(SUBMISSION_ID) } returns submission
        every { userRepository.findById(studentAuthor.id) } returns studentAuthor
        every { participantRepository.findById(participantAuthor.id) } returns participantAuthor
        every { repository.load(any<LazyEntity<VerdictId, Verdict>>()) } returns gradedVerdict
    }

    /** Returns submission 2 of [authorId] in a contest, successfully graded with verdict 4 unless [builder] changes it. */
    private fun gradedSubmission(authorId: UserId, builder: SubmissionDataBuilder.() -> Unit = {}): Submission = submission {
        id = SUBMISSION_ID.value
        createdAt = Instant.MIN
        data = submissionData {
            author = authorId
            solution(3)
            task(1)
            status.graded { status.success { verdict(4) } }
            kind.grading { contest(19) }
            builder()
        }
    }

    private fun testSolution(): Solution = solution {
        id = 3
        createdAt = Instant.MIN
        data = solutionData {
            file("solution.py", "print(1)".toByteArray())
            language.python()
        }
    }

    private fun testPolygon(polygonId: Long): Polygon = polygon {
        id = polygonId
        createdAt = Instant.MIN
        data = testData {
            name = "Polygon $polygonId"
            description = "Polygon"
            file("polygon-$polygonId.xml", "<world/>".toByteArray())
            versionBucket = VersionBucket(UUID(0, polygonId))
        }
    }

    private fun testLogs(logsId: Long): Logs = logs {
        id = logsId
        createdAt = Instant.MIN
        data = logsData { file("logs-$logsId.txt", "logs".toByteArray()) }
    }

    private fun testRecording(recordingId: Long): Recording = recording {
        id = recordingId
        createdAt = Instant.MIN
        data = recordingData { file("recording-$recordingId.mp4", "video".toByteArray()) }
    }

    private fun testJudgeWithId(judgeId: Long): MultipleRoleUser = multipleRoleUser {
        id = judgeId
        createdAt = Instant.MIN
        data = multipleRoleUserData {
            name = "Judge $judgeId"
            email = "judge-$judgeId"
            accessToken("judge-$judgeId", algorithm = HashAlgorithm.Identity)
            roles { judge { data = judgeData {} } }
        }
    }

    private fun testOrder(orderId: Long, judgeId: Long, issuedAt: Instant, score: Int): JudgmentOrder = judgmentOrder {
        id = orderId
        createdAt = issuedAt
        data = judgmentOrderData {
            judge(judgeId)
            submission(SUBMISSION_ID.value)
            this.score = score
            reason = "Order $orderId"
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
        private val SUBMISSION_ID = SubmissionId(2)

        @JvmStatic
        fun authorIds(): List<UserId> = listOf(MultipleRoleUserId(7), SingleRoleUserId(9))
    }
}
