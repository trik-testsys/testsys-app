package tech.testsys.operation.user

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.builder.api.*
import tech.testsys.domain.builder.util.chooser.LanguageChooser
import tech.testsys.domain.builder.util.chooser.SubmissionStatusChooser
import tech.testsys.domain.contract.FileContentReader
import tech.testsys.domain.contract.StoredBlobRef
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperSolutionRepository
import tech.testsys.domain.contract.persistence.repository.EntityLoader
import tech.testsys.domain.contract.persistence.repository.ExerciseRepository
import tech.testsys.domain.contract.persistence.repository.JudgmentOrderRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantContestEntryRepository
import tech.testsys.domain.contract.persistence.repository.SolutionRepository
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.contract.persistence.repository.StudentContestEntryRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.FileContent
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.FileStorageKind
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderData
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionData
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.Statement
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionData
import tech.testsys.domain.model.task.SubmissionKind
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.SingleRoleUser
import tech.testsys.operation.error.*
import tech.testsys.operation.util.*
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class StudyOperationsTests {

    private val competitions = mockk<CompetitionRepository>()
    private val classes = mockk<ClassRepository>()
    private val contests = mockk<ContestRepository>()
    private val participantEntries = mockk<ParticipantContestEntryRepository>()
    private val studentEntries = mockk<StudentContestEntryRepository>()
    private val taskRepository = mockk<TaskRepository>()
    private val submissionRepository = mockk<SubmissionRepository>()
    private val verdictRepository = mockk<VerdictRepository>()
    private val judgmentOrderRepository = mockk<JudgmentOrderRepository>()
    private val statementRepository = mockk<StatementRepository>()
    private val exerciseRepository = mockk<ExerciseRepository>()
    private val solutionRepository = mockk<SolutionRepository>()
    private val developerSolutionRepository = mockk<DeveloperSolutionRepository>()
    private val storedVerdicts = mutableMapOf<VerdictId, Verdict>()
    private val storedJudgmentOrders = mutableMapOf<JudgmentOrderId, JudgmentOrder>()
    private val storedSolutions = mutableMapOf<SolutionId, Solution>()
    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val fileContentReader = mockk<FileContentReader> {
        every { read(any()) } answers { assertInstanceOf(FileContent.Inline::class.java, firstArg<FileData>().content).bytes }
    }
    private val operations = StudyOperations(
        fileContentReader = fileContentReader,
        competitionRepository = competitions,
        classRepository = classes,
        contestRepository = contests,
        participantContestEntryRepository = participantEntries,
        studentContestEntryRepository = studentEntries,
        taskRepository = taskRepository,
        submissionRepository = submissionRepository,
        verdictRepository = verdictRepository,
        judgmentOrderRepository = judgmentOrderRepository,
        statementRepository = statementRepository,
        exerciseRepository = exerciseRepository,
        solutionRepository = solutionRepository,
        developerSolutionRepository = developerSolutionRepository,
        clock = Clock.fixed(now, ZoneOffset.UTC),
    )
    private val participant = testParticipant()
    private val student = testStudent { data = studentData {} }
    private val competition = testCompetition { contests(listOf(19)) }
    private val studyClass = testStudyClass {
        students(listOf(0))
        contests(listOf(19))
    }
    private val contest = testContest()
    private val task = testCommitedTask()
    private val taskContest = testContest { tasks(listOf(0)) }
    private val firstEntry = Instant.parse("2020-01-01T00:00:00Z")
    private val otherClass = `class` {
        id = 24
        createdAt = Instant.EPOCH
        data = studyClass.data
    }

    @Nested
    inner class ViewContestTests {

        @Nested
        inner class ParticipantTests {

            @Nested
            inner class HappyPathTests {

                @Test
                fun `should return the original contest without an entry time if participant has not entered it`() {
                    prepareParticipantView()

                    val result = operations.viewContest(user = participant, contestId = contest.id).getOrThrow()

                    assertSame(contest, result.second)
                    assertNull(result.first)
                }

                @Test
                fun `should return the saved first entry time of the participant with nanosecond precision`() {
                    val enteredAt = Instant.parse("2020-01-01T00:00:00.123456789Z")
                    prepareParticipantView(enteredAt = enteredAt)

                    val result = operations.viewContest(user = participant, contestId = contest.id).getOrThrow()

                    assertSame(contest, result.second)
                    assertEquals(enteredAt, result.first)
                }

                @Test
                fun `should keep lazy task references of the returned contest unresolved`() {
                    val task = testNewTask()
                    val current = contest.withData { tasks(listOf(0)) }
                    val loader = mockk<EntityLoader<TaskId, Task>>()
                    every { loader.load(current.data.tasks) } returns listOf(task)
                    prepareParticipantView(current = current)

                    val result = operations.viewContest(user = participant, contestId = current.id).getOrThrow()

                    assertEquals(listOf(task), result.second.data.tasks.load(loader))
                    verify(exactly = 1) { loader.load(current.data.tasks) }
                }

                @ParameterizedTest
                @ValueSource(longs = [-100, 4_102_444_800])
                fun `should allow viewing completed and future contests`(start: Long) {
                    val current = contest.withData {
                        startsAt = Instant.ofEpochSecond(start)
                        contestDuration = Duration.ofSeconds(10)
                    }
                    prepareParticipantView(current = current)

                    val result = operations.viewContest(user = participant, contestId = current.id).getOrThrow()

                    assertSame(current, result.second)
                    assertNull(result.first)
                }
            }

            @Nested
            inner class RefusalTests {

                @Test
                fun `should raise MissedParticipantRoleError if user is a Supervisor`() {
                    val user = testSupervisor()

                    assertRaises(MissedParticipantRoleError) {
                        operations.viewContest(user = user, contestId = contest.id)
                    }

                    verify { competitions wasNot Called }
                }

                @Test
                fun `should raise CompetitionNotExistsError if participant competition is missing`() {
                    every { competitions.findById(competition.id) } returns null

                    assertRaises(CompetitionNotExistsError(competition.id)) {
                        operations.viewContest(user = participant, contestId = contest.id)
                    }
                }

                @Test
                fun `should raise ContestNotExistsError if contest is missing`() {
                    every { competitions.findById(competition.id) } returns competition
                    every { contests.findById(contest.id) } returns null

                    assertRaises(ContestNotExistsError(contest.id)) {
                        operations.viewContest(user = participant, contestId = contest.id)
                    }
                }

                @Test
                fun `should raise ContestAccessDeniedError if contest belongs to another competition`() {
                    every { competitions.findById(competition.id) } returns competition.withData { contests = mutableListOf() }
                    every { contests.findById(contest.id) } returns contest

                    assertRaises(ContestAccessDeniedError(contest.id)) {
                        operations.viewContest(user = participant, contestId = contest.id)
                    }

                    verify { participantEntries wasNot Called }
                }
            }

            @Nested
            inner class InvariantTests {

                @ParameterizedTest
                @ValueSource(longs = [-100, 4_102_444_800])
                fun `should not save an entry or change the contest, its tasks or the competition when viewing a contest`(start: Long) {
                    val current = contest.withData {
                        startsAt = Instant.ofEpochSecond(start)
                        contestDuration = Duration.ofSeconds(10)
                    }
                    prepareParticipantView(current = current)

                    operations.viewContest(user = participant, contestId = current.id).getOrThrow()

                    verify(exactly = 0) {
                        participantEntries.findOrCreate(any())
                        contests.update(any<Contest>())
                        taskRepository.update(any<Task>())
                        competitions.update(any<Competition>())
                    }
                }
            }

            @Nested
            inner class ModuleRuleTests {

                @Test
                fun `should propagate a technical exception while reading participant entry`() {
                    val failure = IllegalStateException("Participant entry storage failed")
                    prepareParticipantView()
                    every {
                        participantEntries.findByContext(
                            participantId = participant.id,
                            competitionId = competition.id,
                            contestId = contest.id,
                        )
                    } throws failure

                    val thrown = assertThrows(IllegalStateException::class.java) {
                        operations.viewContest(user = participant, contestId = contest.id)
                    }

                    assertSame(failure, thrown)
                }
            }
        }

        @Nested
        inner class StudentTests {

            @Nested
            inner class HappyPathTests {

                @Test
                fun `should return the original contest without an entry time if student has not entered it in selected class`() {
                    prepareStudentView()

                    val result = operations.viewContest(user = student, classId = studyClass.id, contestId = contest.id).getOrThrow()

                    assertSame(contest, result.second)
                    assertNull(result.first)
                }

                @Test
                fun `should return the exact first entry time of the student in selected class`() {
                    val enteredAt = Instant.parse("2020-01-01T00:00:00.123456789Z")
                    prepareStudentView(enteredAt = enteredAt)

                    val result = operations.viewContest(user = student, classId = studyClass.id, contestId = contest.id).getOrThrow()

                    assertSame(contest, result.second)
                    assertEquals(enteredAt, result.first)
                }

                @ParameterizedTest
                @ValueSource(longs = [-100, 4_102_444_800])
                fun `should allow viewing completed and future class contests`(start: Long) {
                    val current = contest.withData {
                        startsAt = Instant.ofEpochSecond(start)
                        contestDuration = Duration.ofSeconds(10)
                    }
                    prepareStudentView(current = current)

                    val result = operations.viewContest(user = student, classId = studyClass.id, contestId = current.id).getOrThrow()

                    assertSame(current, result.second)
                    assertNull(result.first)
                }
            }

            @Nested
            inner class RefusalTests {

                @Test
                fun `should raise MissedStudentRoleError if user has other roles without Student`() {
                    val user = testMultipleRoleUser {
                        roles {
                            developer { data = developerData {} }
                            administrator {}
                        }
                    }

                    assertRaises(MissedStudentRoleError) {
                        operations.viewContest(user = user, classId = studyClass.id, contestId = contest.id)
                    }

                    verify { classes wasNot Called }
                }

                @Test
                fun `should raise ClassNotExistsError if selected class is missing`() {
                    every { classes.findById(studyClass.id) } returns null

                    assertRaises(ClassNotExistsError(studyClass.id)) {
                        operations.viewContest(user = student, classId = studyClass.id, contestId = contest.id)
                    }
                }

                @Test
                fun `should raise ContestNotExistsError if contest is missing`() {
                    every { classes.findById(studyClass.id) } returns studyClass
                    every { contests.findById(contest.id) } returns null

                    assertRaises(ContestNotExistsError(contest.id)) {
                        operations.viewContest(user = student, classId = studyClass.id, contestId = contest.id)
                    }
                }

                @Test
                fun `should raise ClassAccessDeniedError if student is not enrolled in selected class`() {
                    every { classes.findById(studyClass.id) } returns studyClass.withData { students = mutableListOf() }
                    every { contests.findById(contest.id) } returns contest

                    assertRaises(ClassAccessDeniedError(studyClass.id)) {
                        operations.viewContest(user = student, classId = studyClass.id, contestId = contest.id)
                    }
                }

                @Test
                fun `should raise ContestAccessDeniedError if contest is outside selected class`() {
                    every { classes.findById(studyClass.id) } returns studyClass.withData { contests = mutableListOf() }
                    every { contests.findById(contest.id) } returns contest

                    assertRaises(ContestAccessDeniedError(contest.id)) {
                        operations.viewContest(user = student, classId = studyClass.id, contestId = contest.id)
                    }

                    verify { studentEntries wasNot Called }
                }
            }

            @Nested
            inner class InvariantTests {

                @ParameterizedTest
                @ValueSource(longs = [-100, 4_102_444_800])
                fun `should not save an entry or change the contest, its tasks or the class when viewing a class contest`(start: Long) {
                    val current = contest.withData {
                        startsAt = Instant.ofEpochSecond(start)
                        contestDuration = Duration.ofSeconds(10)
                    }
                    prepareStudentView(current = current)

                    operations.viewContest(user = student, classId = studyClass.id, contestId = current.id).getOrThrow()

                    verify(exactly = 0) {
                        studentEntries.findOrCreate(any())
                        contests.update(any<Contest>())
                        taskRepository.update(any<Task>())
                        classes.update(any<Class>())
                    }
                }

                @Test
                fun `should return no entry time if the same contest was entered only in another class`() {
                    stubEntryInOtherClass(contestId = contest.id)
                    prepareStudentView()

                    val result = operations.viewContest(user = student, classId = studyClass.id, contestId = contest.id).getOrThrow()

                    assertNull(result.first)
                }

                @Test
                fun `should deny class access despite ownership and communities of other roles`() {
                    val user = studentWithOtherRoles()
                    prepareContextOwnedWithoutEnrollment(user = user, current = contest)

                    assertRaises(ClassAccessDeniedError(studyClass.id)) {
                        operations.viewContest(user = user, classId = studyClass.id, contestId = contest.id)
                    }
                }
            }

            @Nested
            inner class ModuleRuleTests {

                @Test
                fun `should propagate a technical exception while reading student entry`() {
                    val failure = IllegalStateException("Student entry storage failed")
                    prepareStudentView()
                    every {
                        studentEntries.findByContext(userId = student.id, studyClassId = studyClass.id, contestId = contest.id)
                    } throws failure

                    val thrown = assertThrows(IllegalStateException::class.java) {
                        operations.viewContest(user = student, classId = studyClass.id, contestId = contest.id)
                    }

                    assertSame(failure, thrown)
                }
            }
        }
    }

    @Nested
    inner class ViewTaskTests {

        @Nested
        inner class ParticipantTests {

            @Nested
            inner class HappyPathTests {

                @Test
                fun `should return the original task, submissions of the participant and the best submission`() {
                    val submission = testSubmission(submissionId = 51)
                    prepareParticipantTask()
                    stubSubmissions(submission)
                    stubVerdict(verdictId = 51, scores = listOf(40, 60))

                    val (resultTask, submissions, best) = view().getOrThrow()

                    assertSame(task, resultTask)
                    assertEquals(listOf(submission), submissions)
                    assertSame(submission, best)
                }

                @Test
                fun `should return several submissions of the participant in the order of the port`() {
                    val nextDay = Instant.parse("2026-01-02T00:00:00Z")
                    val first = testSubmission(submissionId = 51, at = Instant.parse("2026-01-01T00:00:00Z"))
                    val second = testSubmission(submissionId = 52, at = nextDay) { queued() }
                    val third = testSubmission(submissionId = 53, at = nextDay) { queued() }
                    prepareParticipantTask()
                    stubSubmissions(first, second, third)
                    stubVerdict(verdictId = 51, scores = listOf(10))

                    val (_, submissions, _) = view().getOrThrow()

                    assertEquals(listOf(first, second, third), submissions)
                }

                @ParameterizedTest
                @ValueSource(longs = [-100, 4_102_444_800])
                fun `should allow viewing a task of completed and future contests`(start: Long) {
                    val current = taskContest.withData {
                        startsAt = Instant.ofEpochSecond(start)
                        contestDuration = Duration.ofSeconds(10)
                    }
                    prepareParticipantTask(current = current)
                    stubSubmissions()

                    val (resultTask, submissions, best) = view(contestId = current.id).getOrThrow()

                    assertSame(task, resultTask)
                    assertEquals(emptyList<Submission>(), submissions)
                    assertNull(best)
                }

                @Test
                fun `should prefer the judgment order score over the verdict score`() {
                    val judged = testSubmission(submissionId = 51, orders = listOf(61))
                    val graded = testSubmission(submissionId = 52)
                    prepareParticipantTask()
                    stubSubmissions(judged, graded)
                    stubVerdict(verdictId = 51, scores = listOf(100))
                    stubVerdict(verdictId = 52, scores = listOf(50))
                    stubJudgmentOrders(testJudgmentOrder(orderId = 61, score = 20))

                    val best = view().getOrThrow().third

                    assertSame(graded, best)
                }

                @Test
                fun `should use the score of the judgment order created last`() {
                    val judged = testSubmission(submissionId = 51, orders = listOf(61, 62))
                    val graded = testSubmission(submissionId = 52)
                    prepareParticipantTask()
                    stubSubmissions(judged, graded)
                    stubVerdict(verdictId = 52, scores = listOf(50))
                    stubJudgmentOrders(
                        testJudgmentOrder(orderId = 61, score = 80, at = Instant.parse("2026-01-02T00:00:00Z")),
                        testJudgmentOrder(orderId = 62, score = 10, at = Instant.parse("2026-01-01T00:00:00Z")),
                    )

                    val best = view().getOrThrow().third

                    assertSame(judged, best)
                }

                @Test
                fun `should use the judgment order with the greater id if orders were created at the same time`() {
                    val judged = testSubmission(submissionId = 51, orders = listOf(62, 61))
                    val graded = testSubmission(submissionId = 52)
                    prepareParticipantTask()
                    stubSubmissions(judged, graded)
                    stubVerdict(verdictId = 52, scores = listOf(50))
                    stubJudgmentOrders(
                        testJudgmentOrder(orderId = 62, score = 80),
                        testJudgmentOrder(orderId = 61, score = 10),
                    )

                    val best = view().getOrThrow().third

                    assertSame(judged, best)
                }

                @Test
                fun `should choose the earlier submission if final scores are equal`() {
                    val later = testSubmission(submissionId = 51, at = Instant.parse("2026-01-02T00:00:00Z"))
                    val earlier = testSubmission(submissionId = 52, at = Instant.parse("2026-01-01T00:00:00Z"))
                    prepareParticipantTask()
                    stubSubmissions(later, earlier)
                    stubVerdict(verdictId = 51, scores = listOf(30, 20))
                    stubVerdict(verdictId = 52, scores = listOf(50))

                    val best = view().getOrThrow().third

                    assertSame(earlier, best)
                }

                @Test
                fun `should choose the submission with the smaller id if final scores and creation times are equal`() {
                    val greater = testSubmission(submissionId = 52)
                    val smaller = testSubmission(submissionId = 51)
                    prepareParticipantTask()
                    stubSubmissions(greater, smaller)
                    stubVerdict(verdictId = 51, scores = listOf(50))
                    stubVerdict(verdictId = 52, scores = listOf(50))

                    val best = view().getOrThrow().third

                    assertSame(smaller, best)
                }

                @Test
                fun `should ignore submissions without a successful verdict even if they have judgment orders`() {
                    val graded = testSubmission(submissionId = 51)
                    val queued = testSubmission(submissionId = 52) { queued() }
                    val inProgress = testSubmission(submissionId = 53) { inProgress() }
                    val failed = testSubmission(submissionId = 54, orders = listOf(64)) {
                        graded { status.error { description = "Crash" } }
                    }
                    val timedOut = testSubmission(submissionId = 55, orders = listOf(65)) {
                        graded { status.timeout() }
                    }
                    prepareParticipantTask()
                    stubSubmissions(queued, inProgress, failed, timedOut, graded)
                    stubVerdict(verdictId = 51, scores = listOf(1))
                    stubJudgmentOrders(testJudgmentOrder(orderId = 64, score = 90))
                    stubJudgmentOrders(testJudgmentOrder(orderId = 65, score = 90))

                    val best = view().getOrThrow().third

                    assertSame(graded, best)
                }

                @Test
                fun `should return no best submission if none has a successful verdict`() {
                    val queued = testSubmission(submissionId = 52) { queued() }
                    val failed = testSubmission(submissionId = 54) { graded { status.error { description = "Crash" } } }
                    prepareParticipantTask()
                    stubSubmissions(queued, failed)

                    val (_, submissions, best) = view().getOrThrow()

                    assertEquals(listOf(queued, failed), submissions)
                    assertNull(best)
                }

                @Test
                fun `should keep verdict references of returned submissions unresolved`() {
                    val submission = testSubmission(submissionId = 51)
                    val verdict = testVerdict(verdictId = 51, scores = listOf(10))
                    val loader = mockk<EntityLoader<VerdictId, Verdict>>()
                    every { loader.load(submission.verdictReference()) } returns verdict
                    prepareParticipantTask()
                    stubSubmissions(submission)
                    stubVerdict(verdictId = 51, scores = listOf(10))

                    val (_, submissions, _) = view().getOrThrow()

                    assertSame(verdict, submissions.single().verdictReference().load(loader))
                    verify(exactly = 1) { loader.load(submission.verdictReference()) }
                }
            }

            @Nested
            inner class RefusalTests {

                @Test
                fun `should raise MissedParticipantRoleError if user is a Supervisor`() {
                    val user = testSupervisor()

                    assertRaises(MissedParticipantRoleError) {
                        operations.viewTask(user = user, contestId = taskContest.id, taskId = task.id)
                    }

                    verify { competitions wasNot Called }
                }

                @Test
                fun `should raise CompetitionNotExistsError if participant competition is missing`() {
                    every { competitions.findById(competition.id) } returns null

                    assertRaises(CompetitionNotExistsError(competition.id)) { view() }
                }

                @Test
                fun `should raise ContestNotExistsError if contest is missing`() {
                    every { competitions.findById(competition.id) } returns competition
                    every { contests.findById(taskContest.id) } returns null

                    assertRaises(ContestNotExistsError(taskContest.id)) { view() }
                }

                @Test
                fun `should raise TaskNotExistsError if task is missing`() {
                    every { competitions.findById(competition.id) } returns competition
                    every { contests.findById(taskContest.id) } returns taskContest
                    every { taskRepository.findById(task.id) } returns null

                    assertRaises(TaskNotExistsError(task.id)) { view() }
                }

                @Test
                fun `should raise ContestAccessDeniedError if contest belongs to another competition`() {
                    every { competitions.findById(competition.id) } returns competition.withData { contests = mutableListOf() }
                    every { contests.findById(taskContest.id) } returns taskContest
                    every { taskRepository.findById(task.id) } returns task

                    assertRaises(ContestAccessDeniedError(taskContest.id)) { view() }
                }

                @Test
                fun `should raise TaskAccessDeniedError if task is outside the contest`() {
                    prepareParticipantTask(current = contest)

                    assertRaises(TaskAccessDeniedError(task.id)) { view(contestId = contest.id) }

                    verify { participantEntries wasNot Called }
                }

                @Test
                fun `should raise ContestNotEnteredError if participant has not entered the contest`() {
                    prepareParticipantTask(enteredAt = null)

                    assertRaises(ContestNotEnteredError(taskContest.id)) { view() }

                    verify { submissionRepository wasNot Called }
                }
            }

            @Nested
            inner class InvariantTests {

                @ParameterizedTest
                @ValueSource(longs = [-100, 4_102_444_800])
                fun `should not save an entry or change the task, contest, submissions, verdicts or judgment orders when viewing a task`(
                    start: Long,
                ) {
                    val current = taskContest.withData {
                        startsAt = Instant.ofEpochSecond(start)
                        contestDuration = Duration.ofSeconds(10)
                    }
                    prepareParticipantTask(current = current)
                    stubSubmissions(
                        testSubmission(submissionId = 51, orders = listOf(61)),
                        testSubmission(submissionId = 52),
                        contestId = current.id,
                    )
                    stubVerdict(verdictId = 52, scores = listOf(50))
                    stubJudgmentOrders(testJudgmentOrder(orderId = 61, score = 20))

                    view(contestId = current.id).getOrThrow()

                    verifyNothingChangedByView()
                    verify(exactly = 0) { participantEntries.findOrCreate(any()) }
                }
            }

            @Nested
            inner class ModuleRuleTests {

                @Test
                fun `should propagate a technical exception while reading submissions`() {
                    val failure = IllegalStateException("Submission storage failed")
                    prepareParticipantTask()
                    every {
                        submissionRepository.findGradingByContext(authorId = participant.id, taskId = task.id, contestId = taskContest.id)
                    } throws failure

                    val thrown = assertThrows(IllegalStateException::class.java) { view() }

                    assertSame(failure, thrown)
                }

                @Test
                fun `should load judgment orders and verdicts with one call each and match answers given out of order`() {
                    val lowJudged = testSubmission(submissionId = 51, orders = listOf(61))
                    val highJudged = testSubmission(submissionId = 52, orders = listOf(62))
                    val lowGraded = testSubmission(submissionId = 53)
                    val highGraded = testSubmission(submissionId = 54)
                    stubVerdict(verdictId = 53, scores = listOf(30))
                    stubVerdict(verdictId = 54, scores = listOf(70))
                    stubJudgmentOrders(testJudgmentOrder(orderId = 61, score = 10), testJudgmentOrder(orderId = 62, score = 50))

                    prepareParticipantTask()
                    stubSubmissions(lowJudged, highGraded, highJudged, lowGraded)

                    val best = view().getOrThrow().third

                    assertSame(highGraded, best)
                    verify(exactly = 1) {
                        judgmentOrderRepository.load(
                            match<LazyEntityList<JudgmentOrderId, JudgmentOrder>> { list ->
                                list.ids.toSet() == setOf(JudgmentOrderId(61), JudgmentOrderId(62))
                            },
                        )
                        verdictRepository.load(
                            match<LazyEntityList<VerdictId, Verdict>> { list -> list.ids.toSet() == setOf(VerdictId(53), VerdictId(54)) },
                        )
                    }
                    verify(exactly = 1) { verdictRepository.load(any<LazyEntityList<VerdictId, Verdict>>()) }
                    verify(exactly = 1) { judgmentOrderRepository.load(any<LazyEntityList<JudgmentOrderId, JudgmentOrder>>()) }
                }
            }

            private fun stubSubmissions(vararg submissions: Submission, contestId: ContestId = taskContest.id) {
                every {
                    submissionRepository.findGradingByContext(authorId = participant.id, taskId = task.id, contestId = contestId)
                } returns submissions.toList()
            }

            private fun view(contestId: ContestId = taskContest.id) =
                operations.viewTask(user = participant, contestId = contestId, taskId = task.id)
        }

        @Nested
        inner class StudentTests {

            @Nested
            inner class HappyPathTests {

                @Test
                fun `should return the original task, submissions of the student and the best submission`() {
                    val submission = testSubmission(submissionId = 51, author = student.id.value)
                    prepareStudentTask()
                    stubSubmissions(submission)
                    stubVerdict(verdictId = 51, scores = listOf(100))

                    val (resultTask, submissions, best) = view().getOrThrow()

                    assertSame(task, resultTask)
                    assertEquals(listOf(submission), submissions)
                    assertSame(submission, best)
                }

                @Test
                fun `should return several submissions of the student in the order of the port`() {
                    val author = student.id.value
                    val firstDay = Instant.parse("2026-01-01T00:00:00Z")
                    val nextDay = Instant.parse("2026-01-02T00:00:00Z")
                    val first = testSubmission(submissionId = 51, author = author, at = firstDay)
                    val second = testSubmission(submissionId = 52, author = author, at = nextDay) { queued() }
                    val third = testSubmission(submissionId = 53, author = author, at = nextDay) { queued() }
                    prepareStudentTask()
                    stubSubmissions(first, second, third)
                    stubVerdict(verdictId = 51, scores = listOf(10))

                    val (_, submissions, _) = view().getOrThrow()

                    assertEquals(listOf(first, second, third), submissions)
                }

                @ParameterizedTest
                @ValueSource(longs = [-100, 4_102_444_800])
                fun `should allow viewing a task of completed and future class contests`(start: Long) {
                    val current = taskContest.withData {
                        startsAt = Instant.ofEpochSecond(start)
                        contestDuration = Duration.ofSeconds(10)
                    }
                    prepareStudentTask(current = current)
                    stubSubmissions(contestId = current.id)

                    val (resultTask, _, best) = view(contestId = current.id).getOrThrow()

                    assertSame(task, resultTask)
                    assertNull(best)
                }
            }

            @Nested
            inner class RefusalTests {

                @Test
                fun `should raise MissedStudentRoleError if user has other roles without Student`() {
                    val user = testMultipleRoleUser { roles { developer { data = developerData {} } } }

                    assertRaises(MissedStudentRoleError) { view(user = user) }

                    verify { classes wasNot Called }
                }

                @Test
                fun `should raise ClassNotExistsError if selected class is missing`() {
                    every { classes.findById(studyClass.id) } returns null

                    assertRaises(ClassNotExistsError(studyClass.id)) { view() }
                }

                @Test
                fun `should raise ContestNotExistsError if contest is missing`() {
                    every { classes.findById(studyClass.id) } returns studyClass
                    every { contests.findById(taskContest.id) } returns null

                    assertRaises(ContestNotExistsError(taskContest.id)) { view() }
                }

                @Test
                fun `should raise TaskNotExistsError if task is missing`() {
                    every { classes.findById(studyClass.id) } returns studyClass
                    every { contests.findById(taskContest.id) } returns taskContest
                    every { taskRepository.findById(task.id) } returns null

                    assertRaises(TaskNotExistsError(task.id)) { view() }
                }

                @Test
                fun `should raise ClassAccessDeniedError if student is not enrolled in selected class`() {
                    every { classes.findById(studyClass.id) } returns studyClass.withData { students = mutableListOf() }
                    every { contests.findById(taskContest.id) } returns taskContest
                    every { taskRepository.findById(task.id) } returns task

                    assertRaises(ClassAccessDeniedError(studyClass.id)) { view() }
                }

                @Test
                fun `should raise ContestAccessDeniedError if contest is outside selected class`() {
                    every { classes.findById(studyClass.id) } returns studyClass.withData { contests = mutableListOf() }
                    every { contests.findById(taskContest.id) } returns taskContest
                    every { taskRepository.findById(task.id) } returns task

                    assertRaises(ContestAccessDeniedError(taskContest.id)) { view() }
                }

                @Test
                fun `should raise TaskAccessDeniedError if task is outside the contest`() {
                    prepareStudentTask(current = contest)

                    assertRaises(TaskAccessDeniedError(task.id)) { view(contestId = contest.id) }

                    verify { studentEntries wasNot Called }
                }

                @Test
                fun `should raise ContestNotEnteredError if the contest was entered only in another class`() {
                    stubEntryInOtherClass(contestId = taskContest.id)
                    prepareStudentTask(enteredAt = null)

                    assertRaises(ContestNotEnteredError(taskContest.id)) { view() }

                    verify { submissionRepository wasNot Called }
                }
            }

            @Nested
            inner class InvariantTests {

                @ParameterizedTest
                @ValueSource(longs = [-100, 4_102_444_800])
                fun `should not save an entry or change the task, contest, submissions, verdicts or judgment orders when viewing a task`(
                    start: Long,
                ) {
                    val current = taskContest.withData {
                        startsAt = Instant.ofEpochSecond(start)
                        contestDuration = Duration.ofSeconds(10)
                    }
                    prepareStudentTask(current = current)
                    stubSubmissions(
                        testSubmission(submissionId = 51, author = student.id.value, orders = listOf(61)),
                        testSubmission(submissionId = 52, author = student.id.value),
                        contestId = current.id,
                    )
                    stubVerdict(verdictId = 52, scores = listOf(50))
                    stubJudgmentOrders(testJudgmentOrder(orderId = 61, score = 20))

                    view(contestId = current.id).getOrThrow()

                    verifyNothingChangedByView()
                    verify(exactly = 0) { studentEntries.findOrCreate(any()) }
                }

                @Test
                fun `should deny class access despite ownership and communities of other roles`() {
                    val user = studentWithOtherRoles()
                    prepareContextOwnedWithoutEnrollment(user = user, current = taskContest)

                    assertRaises(ClassAccessDeniedError(studyClass.id)) { view(user = user) }

                    verify { submissionRepository wasNot Called }
                }
            }

            @Nested
            inner class ModuleRuleTests {

                @Test
                fun `should propagate a technical exception while reading submissions`() {
                    val failure = IllegalStateException("Submission storage failed")
                    prepareStudentTask()
                    every {
                        submissionRepository.findGradingByContext(authorId = student.id, taskId = task.id, contestId = taskContest.id)
                    } throws failure

                    val thrown = assertThrows(IllegalStateException::class.java) { view() }

                    assertSame(failure, thrown)
                }
            }

            private fun stubSubmissions(vararg submissions: Submission, contestId: ContestId = taskContest.id) {
                every {
                    submissionRepository.findGradingByContext(authorId = student.id, taskId = task.id, contestId = contestId)
                } returns submissions.toList()
            }

            private fun view(user: MultipleRoleUser = student, contestId: ContestId = taskContest.id) =
                operations.viewTask(user = user, classId = studyClass.id, contestId = contestId, taskId = task.id)
        }

        private fun verifyNothingChangedByView() {
            verify(exactly = 0) {
                taskRepository.update(any<Task>())
                contests.update(any<Contest>())
                submissionRepository.save(any<SubmissionData>())
                submissionRepository.update(any<Submission>())
                verdictRepository.save(any<VerdictData>())
                verdictRepository.update(any<Verdict>())
                judgmentOrderRepository.save(any<JudgmentOrderData>())
                judgmentOrderRepository.update(any<JudgmentOrder>())
            }
        }
    }

    @Nested
    inner class DownloadTaskResourceTests {

        private val statement = testStatement(statementId = 1)
        private val exercise = testExercise(exerciseId = 1)

        @Nested
        inner class ParticipantTests {

            @Nested
            inner class HappyPathTests {

                @Test
                fun `should return the file of the committed statement`() {
                    prepareParticipantTask()
                    val storedFile =
                        FileData("statement.pdf", FileContent.Stored(StoredBlobRef("statement"), FileStorageKind.Statement))
                    every { statementRepository.findById(statement.id) } returns statement.withData { file(storedFile) }
                    every { fileContentReader.read(storedFile) } returns byteArrayOf(1, 2)

                    val file = download(resourceId = statement.id).getOrThrow()

                    assertEquals("statement.pdf", file.uploadedFilename)
                    assertArrayEquals(byteArrayOf(1, 2), assertInstanceOf(FileContent.Inline::class.java, file.content).bytes)
                    verify(exactly = 1) { fileContentReader.read(storedFile) }
                }

                @Test
                fun `should return the file of a committed exercise`() {
                    prepareParticipantTask()
                    every { exerciseRepository.findById(exercise.id) } returns exercise

                    val file = download(resourceId = exercise.id).getOrThrow()

                    assertEquals(exercise.data.file.uploadedFilename, file.uploadedFilename)
                    assertArrayEquals(fileContentReader.read(exercise.data.file), fileContentReader.read(file))
                }

                @Test
                fun `should allow downloading after the contest end`() {
                    prepareParticipantTask(current = endedTaskContest())
                    every { statementRepository.findById(statement.id) } returns statement

                    val file = download(resourceId = statement.id).getOrThrow()

                    assertEquals(statement.data.file.uploadedFilename, file.uploadedFilename)
                    assertArrayEquals(fileContentReader.read(statement.data.file), fileContentReader.read(file))
                }
            }

            @Nested
            inner class RefusalTests {

                @Test
                fun `should raise MissedParticipantRoleError if user is a Supervisor`() {
                    val user = testSupervisor()

                    assertRaises(MissedParticipantRoleError) {
                        download(resourceId = statement.id, user = user)
                    }

                    verify { competitions wasNot Called }
                }

                @Test
                fun `should raise CompetitionNotExistsError if participant competition is missing`() {
                    every { competitions.findById(competition.id) } returns null

                    assertRaises(CompetitionNotExistsError(competition.id)) {
                        download(resourceId = statement.id)
                    }
                }

                @Test
                fun `should raise ContestNotExistsError if contest is missing`() {
                    every { competitions.findById(competition.id) } returns competition
                    every { contests.findById(taskContest.id) } returns null

                    assertRaises(ContestNotExistsError(taskContest.id)) {
                        download(resourceId = statement.id)
                    }
                }

                @Test
                fun `should raise TaskNotExistsError if task is missing`() {
                    every { competitions.findById(competition.id) } returns competition
                    every { contests.findById(taskContest.id) } returns taskContest
                    every { taskRepository.findById(task.id) } returns null

                    assertRaises(TaskNotExistsError(task.id)) {
                        download(resourceId = statement.id)
                    }
                }

                @Test
                fun `should raise ContestAccessDeniedError if contest belongs to another competition`() {
                    every { competitions.findById(competition.id) } returns competition.withData { contests = mutableListOf() }
                    every { contests.findById(taskContest.id) } returns taskContest
                    every { taskRepository.findById(task.id) } returns task

                    assertRaises(ContestAccessDeniedError(taskContest.id)) {
                        download(resourceId = statement.id)
                    }
                }

                @Test
                fun `should raise TaskAccessDeniedError if task is outside the contest`() {
                    prepareParticipantTask(current = contest)

                    assertRaises(TaskAccessDeniedError(task.id)) {
                        download(resourceId = statement.id, contestId = contest.id)
                    }
                }

                @Test
                fun `should raise ContestNotEnteredError if participant has not entered the contest`() {
                    prepareParticipantTask(enteredAt = null)

                    assertRaises(ContestNotEnteredError(taskContest.id)) {
                        download(resourceId = statement.id)
                    }

                    verify { statementRepository wasNot Called }
                    verify { fileContentReader wasNot Called }
                }

                @Test
                fun `should raise ResourceNotInCommittedTaskError for an exercise outside the committed revision`() {
                    prepareParticipantTask()

                    assertRaises(ResourceNotInCommittedTaskError(task.id, ExerciseId(5))) {
                        download(resourceId = ExerciseId(5))
                    }
                }

                @Test
                fun `should raise ResourceNotInCommittedTaskError for a test`() {
                    prepareParticipantTask(currentTask = taskWithTest())

                    assertRaises(ResourceNotInCommittedTaskError(task.id, TestId(7))) {
                        download(resourceId = TestId(7))
                    }
                }
            }

            @Nested
            inner class InvariantTests {

                @Test
                fun `should return the committed statement of an Uncommitted task`() {
                    prepareParticipantTask(currentTask = uncommittedTask())
                    every { statementRepository.findById(statement.id) } returns statement

                    val file = download(resourceId = statement.id).getOrThrow()

                    assertEquals(statement.data.file.uploadedFilename, file.uploadedFilename)
                    assertArrayEquals(fileContentReader.read(statement.data.file), fileContentReader.read(file))
                }

                @Test
                fun `should raise ResourceNotInCommittedTaskError for a statement of the work-in-progress revision`() {
                    prepareParticipantTask(currentTask = uncommittedTask())

                    assertRaises(ResourceNotInCommittedTaskError(task.id, StatementId(2))) {
                        download(resourceId = StatementId(2))
                    }

                    verify { statementRepository wasNot Called }
                    verify { fileContentReader wasNot Called }
                }

                @Test
                fun `should raise ResourceNotInCommittedTaskError for an exercise of the work-in-progress revision`() {
                    prepareParticipantTask(currentTask = uncommittedTask())

                    assertRaises(ResourceNotInCommittedTaskError(task.id, ExerciseId(2))) {
                        download(resourceId = ExerciseId(2))
                    }

                    verify { exerciseRepository wasNot Called }
                }

                @Test
                fun `should raise ResourceNotInCommittedTaskError if the task has no committed revision`() {
                    prepareParticipantTask(currentTask = newTaskWithStatement())

                    assertRaises(ResourceNotInCommittedTaskError(task.id, statement.id)) {
                        download(resourceId = statement.id)
                    }

                    verify { statementRepository wasNot Called }
                    verify { fileContentReader wasNot Called }
                }

                @Test
                fun `should not save an entry or change the task, contest or resources when downloading after the contest end`() {
                    prepareParticipantTask(current = endedTaskContest())
                    every { statementRepository.findById(statement.id) } returns statement

                    download(resourceId = statement.id).getOrThrow()

                    verifyNothingChangedByDownload()
                    verify(exactly = 0) { participantEntries.findOrCreate(any()) }
                }
            }

            private fun download(resourceId: DomainId, user: SingleRoleUser = participant, contestId: ContestId = taskContest.id) =
                operations.downloadTaskResource(user = user, contestId = contestId, taskId = task.id, resourceId = resourceId)
        }

        @Nested
        inner class StudentTests {

            @Nested
            inner class HappyPathTests {

                @Test
                fun `should return the file of the committed statement`() {
                    prepareStudentTask()
                    every { statementRepository.findById(statement.id) } returns statement

                    val file = download(resourceId = statement.id).getOrThrow()

                    assertEquals(statement.data.file.uploadedFilename, file.uploadedFilename)
                    assertArrayEquals(fileContentReader.read(statement.data.file), fileContentReader.read(file))
                }

                @Test
                fun `should return the file of a committed exercise`() {
                    prepareStudentTask()
                    every { exerciseRepository.findById(exercise.id) } returns exercise

                    val file = download(resourceId = exercise.id).getOrThrow()

                    assertEquals(exercise.data.file.uploadedFilename, file.uploadedFilename)
                    assertArrayEquals(fileContentReader.read(exercise.data.file), fileContentReader.read(file))
                }

                @Test
                fun `should allow downloading after the contest end`() {
                    prepareStudentTask(current = endedTaskContest())
                    every { statementRepository.findById(statement.id) } returns statement

                    val file = download(resourceId = statement.id).getOrThrow()

                    assertEquals(statement.data.file.uploadedFilename, file.uploadedFilename)
                    assertArrayEquals(fileContentReader.read(statement.data.file), fileContentReader.read(file))
                }
            }

            @Nested
            inner class RefusalTests {

                @Test
                fun `should raise MissedStudentRoleError if user has other roles without Student`() {
                    val user = testMultipleRoleUser { roles { developer { data = developerData {} } } }

                    assertRaises(MissedStudentRoleError) {
                        download(resourceId = statement.id, user = user)
                    }

                    verify { classes wasNot Called }
                }

                @Test
                fun `should raise ClassNotExistsError if selected class is missing`() {
                    every { classes.findById(studyClass.id) } returns null

                    assertRaises(ClassNotExistsError(studyClass.id)) {
                        download(resourceId = statement.id)
                    }
                }

                @Test
                fun `should raise ContestNotExistsError if contest is missing`() {
                    every { classes.findById(studyClass.id) } returns studyClass
                    every { contests.findById(taskContest.id) } returns null

                    assertRaises(ContestNotExistsError(taskContest.id)) {
                        download(resourceId = statement.id)
                    }
                }

                @Test
                fun `should raise TaskNotExistsError if task is missing`() {
                    every { classes.findById(studyClass.id) } returns studyClass
                    every { contests.findById(taskContest.id) } returns taskContest
                    every { taskRepository.findById(task.id) } returns null

                    assertRaises(TaskNotExistsError(task.id)) {
                        download(resourceId = statement.id)
                    }
                }

                @Test
                fun `should raise ClassAccessDeniedError if student is not enrolled in selected class`() {
                    every { classes.findById(studyClass.id) } returns studyClass.withData { students = mutableListOf() }
                    every { contests.findById(taskContest.id) } returns taskContest
                    every { taskRepository.findById(task.id) } returns task

                    assertRaises(ClassAccessDeniedError(studyClass.id)) {
                        download(resourceId = statement.id)
                    }
                }

                @Test
                fun `should raise ContestAccessDeniedError if contest is outside selected class`() {
                    every { classes.findById(studyClass.id) } returns studyClass.withData { contests = mutableListOf() }
                    every { contests.findById(taskContest.id) } returns taskContest
                    every { taskRepository.findById(task.id) } returns task

                    assertRaises(ContestAccessDeniedError(taskContest.id)) {
                        download(resourceId = statement.id)
                    }
                }

                @Test
                fun `should raise TaskAccessDeniedError if task is outside the contest`() {
                    prepareStudentTask(current = contest)

                    assertRaises(TaskAccessDeniedError(task.id)) {
                        download(resourceId = statement.id, contestId = contest.id)
                    }
                }

                @Test
                fun `should raise ContestNotEnteredError if the contest was entered only in another class`() {
                    stubEntryInOtherClass(contestId = taskContest.id)
                    prepareStudentTask(enteredAt = null)

                    assertRaises(ContestNotEnteredError(taskContest.id)) {
                        download(resourceId = statement.id)
                    }

                    verify { statementRepository wasNot Called }
                    verify { fileContentReader wasNot Called }
                }

                @Test
                fun `should raise ResourceNotInCommittedTaskError for an exercise outside the committed revision`() {
                    prepareStudentTask()

                    assertRaises(ResourceNotInCommittedTaskError(task.id, ExerciseId(5))) {
                        download(resourceId = ExerciseId(5))
                    }
                }

                @Test
                fun `should raise ResourceNotInCommittedTaskError for a test`() {
                    prepareStudentTask(currentTask = taskWithTest())

                    assertRaises(ResourceNotInCommittedTaskError(task.id, TestId(7))) {
                        download(resourceId = TestId(7))
                    }
                }
            }

            @Nested
            inner class InvariantTests {

                @Test
                fun `should return the committed statement of an Uncommitted task`() {
                    prepareStudentTask(currentTask = uncommittedTask())
                    every { statementRepository.findById(statement.id) } returns statement

                    val file = download(resourceId = statement.id).getOrThrow()

                    assertEquals(statement.data.file.uploadedFilename, file.uploadedFilename)
                    assertArrayEquals(fileContentReader.read(statement.data.file), fileContentReader.read(file))
                }

                @Test
                fun `should raise ResourceNotInCommittedTaskError for a statement of the work-in-progress revision`() {
                    prepareStudentTask(currentTask = uncommittedTask())

                    assertRaises(ResourceNotInCommittedTaskError(task.id, StatementId(2))) {
                        download(resourceId = StatementId(2))
                    }

                    verify { statementRepository wasNot Called }
                    verify { fileContentReader wasNot Called }
                }

                @Test
                fun `should raise ResourceNotInCommittedTaskError if the task has no committed revision`() {
                    prepareStudentTask(currentTask = newTaskWithStatement())

                    assertRaises(ResourceNotInCommittedTaskError(task.id, statement.id)) {
                        download(resourceId = statement.id)
                    }

                    verify { statementRepository wasNot Called }
                    verify { fileContentReader wasNot Called }
                }

                @Test
                fun `should not save an entry or change the task, contest or resources when downloading after the contest end`() {
                    prepareStudentTask(current = endedTaskContest())
                    every { statementRepository.findById(statement.id) } returns statement

                    download(resourceId = statement.id).getOrThrow()

                    verifyNothingChangedByDownload()
                    verify(exactly = 0) { studentEntries.findOrCreate(any()) }
                }

                @Test
                fun `should deny class access despite ownership and communities of other roles`() {
                    val user = studentWithOtherRoles()
                    prepareContextOwnedWithoutEnrollment(user = user, current = taskContest)

                    assertRaises(ClassAccessDeniedError(studyClass.id)) {
                        download(resourceId = statement.id, user = user)
                    }

                    verify { statementRepository wasNot Called }
                    verify { fileContentReader wasNot Called }
                }
            }

            private fun download(resourceId: DomainId, user: MultipleRoleUser = student, contestId: ContestId = taskContest.id) =
                operations.downloadTaskResource(
                    user = user,
                    classId = studyClass.id,
                    contestId = contestId,
                    taskId = task.id,
                    resourceId = resourceId,
                )
        }

        private fun verifyNothingChangedByDownload() {
            verify(exactly = 0) {
                taskRepository.update(any<Task>())
                contests.update(any<Contest>())
                statementRepository.update(any<Statement>())
                exerciseRepository.update(any<Exercise>())
            }
        }

        private fun endedTaskContest(): Contest = taskContest.withData {
            startsAt = Instant.ofEpochSecond(-100)
            contestDuration = Duration.ofSeconds(10)
        }

        private fun uncommittedTask(): Task = testTask {
            uncommitted(
                wipBuilder = {
                    exercises = mutableListOf(ExerciseId(2))
                    statement = StatementId(2)
                },
                lastCommittedBuilder = {
                    exercises = mutableListOf(ExerciseId(1))
                    statement = StatementId(1)
                },
            )
        }

        private fun newTaskWithStatement(): Task = testTask { new { statement = StatementId(1) } }

        private fun taskWithTest(): Task = testTask {
            committed {
                exercises(listOf(1))
                statement(1)
                tests(listOf(7))
            }
        }

        private fun testExercise(exerciseId: Long): Exercise = exercise {
            id = exerciseId
            createdAt = Instant.EPOCH
            data = exerciseData {
                name = "Exercise"
                description = "Exercise description"
                versionBucket = VersionBucket(UUID(0, 1))
                file("exercise.qrs", "exercise".toByteArray())
                language.python()
            }
        }
    }

    @Nested
    inner class SendSolutionTests {

        private val file = FileData(uploadedFilename = "solution.py", content = "print(1)".toByteArray())
        private val sentSolution = slot<SolutionData>()
        private val sentSubmission = slot<SubmissionData>()
        private val savedSolution = solution {
            id = 101
            createdAt = now
            data = solutionData {
                file(file)
                language.python()
            }
        }
        private val savedSubmission = submission {
            id = 111
            createdAt = now
            data = submissionData {
                author(0)
                solution(101)
                task(0)
                status.queued()
                kind.grading { contest(19) }
            }
        }

        @Nested
        inner class ParticipantTests {

            @Nested
            inner class HappyPathTests {

                @Test
                fun `should save the solution and a queued grading submission of the participant and return it`() {
                    prepareParticipantSend()

                    val result = send().getOrThrow()

                    assertSame(savedSubmission, result)
                    assertEquals(file.uploadedFilename, sentSolution.captured.file.uploadedFilename)
                    assertSame(file, sentSolution.captured.file)
                    assertSame(TrikSupportedLanguage.Python, sentSolution.captured.language)
                    assertEquals(participant.id, sentSubmission.captured.author.id)
                    assertEquals(savedSolution.id, sentSubmission.captured.solution.id)
                    assertEquals(task.id, sentSubmission.captured.task.id)
                    assertSame(SubmissionStatus.Queued, sentSubmission.captured.status)
                    assertEquals(
                        taskContest.id,
                        assertInstanceOf(SubmissionKind.Grading::class.java, sentSubmission.captured.kind).contest.id,
                    )
                    assertEquals(emptyList<JudgmentOrderId>(), sentSubmission.captured.judgmentOrders.ids)
                }

                @Test
                fun `should save a solution in the visual language of TRIK Studio if the task allows it`() {
                    prepareParticipantSend(allowed = { visualLanguage() })

                    send(language = TrikSupportedLanguage.VisualLanguage).getOrThrow()

                    assertSame(TrikSupportedLanguage.VisualLanguage, sentSolution.captured.language)
                }

                @Test
                fun `should accept a solution sent one millisecond before the contest end`() {
                    val current = taskContest.withData {
                        startsAt = firstEntry
                        contestDuration = Duration.between(firstEntry, now).plusMillis(1)
                    }
                    prepareParticipantSend(current = current)

                    val result = send().getOrThrow()

                    assertSame(savedSubmission, result)
                }

                @Test
                fun `should accept a solution sent one millisecond before the end of the attempt`() {
                    val current = taskContest.withData { attemptDuration = Duration.between(firstEntry, now).plusMillis(1) }
                    prepareParticipantSend(current = current)

                    val result = send().getOrThrow()

                    assertSame(savedSubmission, result)
                }

                @Test
                fun `should accept a solution long after the first entry if contest has neither end nor attempt limit`() {
                    val current = taskContest.withData { startsAt = firstEntry }
                    prepareParticipantSend(current = current)

                    val result = send().getOrThrow()

                    assertSame(savedSubmission, result)
                }
            }

            @Nested
            inner class RefusalTests {

                @Test
                fun `should raise MissedParticipantRoleError if user is a Supervisor`() {
                    val user = testSupervisor()

                    assertRaises(MissedParticipantRoleError) { send(user = user) }

                    verify { competitions wasNot Called }
                    verifyNothingSaved()
                }

                @Test
                fun `should raise CompetitionNotExistsError if participant competition is missing`() {
                    every { competitions.findById(competition.id) } returns null

                    assertRaises(CompetitionNotExistsError(competition.id)) { send() }

                    verifyNothingSaved()
                }

                @Test
                fun `should raise ContestNotExistsError if contest is missing`() {
                    every { competitions.findById(competition.id) } returns competition
                    every { contests.findById(taskContest.id) } returns null

                    assertRaises(ContestNotExistsError(taskContest.id)) { send() }

                    verifyNothingSaved()
                }

                @Test
                fun `should raise TaskNotExistsError if task is missing`() {
                    every { competitions.findById(competition.id) } returns competition
                    every { contests.findById(taskContest.id) } returns taskContest
                    every { taskRepository.findById(task.id) } returns null

                    assertRaises(TaskNotExistsError(task.id)) { send() }

                    verifyNothingSaved()
                }

                @Test
                fun `should raise ContestAccessDeniedError if contest belongs to another competition`() {
                    every { competitions.findById(competition.id) } returns competition.withData { contests = mutableListOf() }
                    every { contests.findById(taskContest.id) } returns taskContest
                    every { taskRepository.findById(task.id) } returns task

                    assertRaises(ContestAccessDeniedError(taskContest.id)) { send() }

                    verifyNothingSaved()
                }

                @Test
                fun `should raise TaskAccessDeniedError if task is outside the contest`() {
                    prepareParticipantSend(current = contest)

                    assertRaises(TaskAccessDeniedError(task.id)) { send(contestId = contest.id) }

                    verify { participantEntries wasNot Called }
                    verifyNothingSaved()
                }

                @Test
                fun `should raise ContestNotEnteredError if participant has not entered the contest`() {
                    prepareParticipantSend(enteredAt = null)

                    assertRaises(ContestNotEnteredError(taskContest.id)) { send() }

                    verifyNothingSaved()
                }

                @Test
                fun `should raise ContestEndedError if solution is sent exactly at the contest end`() {
                    val current = taskContest.withData {
                        startsAt = firstEntry
                        contestDuration = Duration.between(firstEntry, now)
                    }
                    prepareParticipantSend(current = current)

                    assertRaises(ContestEndedError(current.id, now)) { send() }

                    verifyNothingSaved()
                }

                @Test
                fun `should raise ContestAttemptExpiredError if solution is sent exactly at the end of the attempt`() {
                    val current = taskContest.withData { attemptDuration = Duration.between(firstEntry, now) }
                    prepareParticipantSend(current = current)

                    assertRaises(ContestAttemptExpiredError(current.id, now)) { send() }

                    verifyNothingSaved()
                }

                @Test
                fun `should raise SolutionLanguageNotAllowedError if committed task has no developer solution in that language`() {
                    prepareParticipantSend()

                    assertRaises(SolutionLanguageNotAllowedError(task.id, TrikSupportedLanguage.VisualLanguage)) {
                        send(language = TrikSupportedLanguage.VisualLanguage)
                    }

                    verifyNothingSaved()
                }
            }

            @Nested
            inner class InvariantTests {

                @Test
                fun `should save a new solution and a new submission if the same file was already sent`() {
                    prepareParticipantSend()
                    stubEarlierSubmissionOfSameFile(author = participant.id.value)

                    val result = send().getOrThrow()

                    assertSame(savedSubmission, result)
                    verifyNewSolutionAndSubmissionSaved()
                }

                @Test
                fun `should not change the task, contest, competition or entry when sending a solution`() {
                    prepareParticipantSend()

                    send().getOrThrow()

                    verify(exactly = 0) {
                        taskRepository.update(any<Task>())
                        contests.update(any<Contest>())
                        competitions.update(any<Competition>())
                        participantEntries.findOrCreate(any())
                    }
                }

                @Test
                fun `should raise SolutionLanguageNotAllowedError if the language is only in the working revision`() {
                    prepareParticipantSend(currentTask = uncommittedSolutionTask(committed = listOf(81), wip = listOf(81, 82)))
                    stubDeveloperSolutions(
                        stubReferenceSolution(developerSolutionId = 81) { python() },
                        stubReferenceSolution(developerSolutionId = 82) { javaScript() },
                    )

                    assertRaises(SolutionLanguageNotAllowedError(task.id, TrikSupportedLanguage.JavaScript)) {
                        send(language = TrikSupportedLanguage.JavaScript)
                    }

                    verifyNothingSaved()
                }

                @Test
                fun `should accept a language of the last committed revision of an uncommitted task`() {
                    prepareParticipantSend(currentTask = uncommittedSolutionTask(committed = listOf(82), wip = listOf(81)))
                    stubDeveloperSolutions(stubReferenceSolution(developerSolutionId = 82) { javaScript() })

                    val result = send(language = TrikSupportedLanguage.JavaScript).getOrThrow()

                    assertSame(savedSubmission, result)
                }

                @Test
                fun `should raise SolutionLanguageNotAllowedError for a new task without a committed revision`() {
                    prepareParticipantSend(currentTask = testNewTask())

                    assertRaises(SolutionLanguageNotAllowedError(task.id, TrikSupportedLanguage.Python)) { send() }

                    verify { developerSolutionRepository wasNot Called }
                    verifyNothingSaved()
                }
            }

            @Nested
            inner class ModuleRuleTests {

                @Test
                fun `should propagate a technical exception while saving the submission`() {
                    val failure = IllegalStateException("Submission storage failed")
                    prepareParticipantSend()
                    every { submissionRepository.save(any<SubmissionData>()) } throws failure

                    val thrown = assertThrows(IllegalStateException::class.java) { send() }

                    assertSame(failure, thrown)
                }

                @Test
                fun `should load the solutions of all committed developer solutions with one call if they share a solution`() {
                    prepareParticipantSend(
                        currentTask = testTask {
                            committed {
                                exercises(listOf(1))
                                statement(1)
                                developerSolutions(listOf(81, 82))
                            }
                        },
                    )
                    val first = stubReferenceSolution(developerSolutionId = 81) { python() }
                    stubDeveloperSolutions(
                        first,
                        developerSolution {
                            id = 82
                            createdAt = Instant.EPOCH
                            data = first.data
                        },
                    )

                    send().getOrThrow()

                    verify(exactly = 1) {
                        solutionRepository.load(
                            match<LazyEntityList<SolutionId, Solution>> { list -> list.ids == listOf(first.data.solution.id) },
                        )
                    }
                }
            }

            private fun send(
                user: SingleRoleUser = participant,
                contestId: ContestId = taskContest.id,
                language: TrikSupportedLanguage = TrikSupportedLanguage.Python,
            ) = operations.sendSolution(user = user, contestId = contestId, taskId = task.id, file = file, language = language)

            private fun prepareParticipantSend(
                current: Contest = taskContest,
                currentTask: Task = solutionTask(),
                enteredAt: Instant? = firstEntry,
                allowed: LanguageChooser.() -> Unit = { python() },
            ) {
                prepareParticipantTask(current = current, currentTask = currentTask, enteredAt = enteredAt)
                stubDeveloperSolutions(stubReferenceSolution(developerSolutionId = 81, chooseLanguage = allowed))
                stubSaving()
            }
        }

        @Nested
        inner class StudentTests {

            @Nested
            inner class HappyPathTests {

                @Test
                fun `should save the solution and a queued grading submission of the student and return it`() {
                    prepareStudentSend()

                    val result = send().getOrThrow()

                    assertSame(savedSubmission, result)
                    assertEquals(file.uploadedFilename, sentSolution.captured.file.uploadedFilename)
                    assertSame(file, sentSolution.captured.file)
                    assertSame(TrikSupportedLanguage.JavaScript, sentSolution.captured.language)
                    assertEquals(student.id, sentSubmission.captured.author.id)
                    assertEquals(savedSolution.id, sentSubmission.captured.solution.id)
                    assertEquals(task.id, sentSubmission.captured.task.id)
                    assertSame(SubmissionStatus.Queued, sentSubmission.captured.status)
                    assertEquals(
                        taskContest.id,
                        assertInstanceOf(SubmissionKind.Grading::class.java, sentSubmission.captured.kind).contest.id,
                    )
                    assertEquals(emptyList<JudgmentOrderId>(), sentSubmission.captured.judgmentOrders.ids)
                }

                @Test
                fun `should accept a solution sent one millisecond before the contest end`() {
                    val current = taskContest.withData {
                        startsAt = firstEntry
                        contestDuration = Duration.between(firstEntry, now).plusMillis(1)
                    }
                    prepareStudentSend(current = current)

                    val result = send().getOrThrow()

                    assertSame(savedSubmission, result)
                }

                @Test
                fun `should accept a solution sent one millisecond before the end of the attempt in selected class`() {
                    val current = taskContest.withData { attemptDuration = Duration.between(firstEntry, now).plusMillis(1) }
                    prepareStudentSend(current = current)

                    val result = send().getOrThrow()

                    assertSame(savedSubmission, result)
                }

                @Test
                fun `should accept a solution long after the first entry if contest has neither end nor attempt limit`() {
                    val current = taskContest.withData { startsAt = firstEntry }
                    prepareStudentSend(current = current)

                    val result = send().getOrThrow()

                    assertSame(savedSubmission, result)
                }
            }

            @Nested
            inner class RefusalTests {

                @Test
                fun `should raise MissedStudentRoleError if user has other roles without Student`() {
                    val user = testMultipleRoleUser { roles { developer { data = developerData {} } } }

                    assertRaises(MissedStudentRoleError) { send(user = user) }

                    verify { classes wasNot Called }
                    verifyNothingSaved()
                }

                @Test
                fun `should raise ClassNotExistsError if selected class is missing`() {
                    every { classes.findById(studyClass.id) } returns null

                    assertRaises(ClassNotExistsError(studyClass.id)) { send() }

                    verifyNothingSaved()
                }

                @Test
                fun `should raise ContestNotExistsError if contest is missing`() {
                    every { classes.findById(studyClass.id) } returns studyClass
                    every { contests.findById(taskContest.id) } returns null

                    assertRaises(ContestNotExistsError(taskContest.id)) { send() }

                    verifyNothingSaved()
                }

                @Test
                fun `should raise TaskNotExistsError if task is missing`() {
                    every { classes.findById(studyClass.id) } returns studyClass
                    every { contests.findById(taskContest.id) } returns taskContest
                    every { taskRepository.findById(task.id) } returns null

                    assertRaises(TaskNotExistsError(task.id)) { send() }

                    verifyNothingSaved()
                }

                @Test
                fun `should raise ClassAccessDeniedError if student is not enrolled in selected class`() {
                    every { classes.findById(studyClass.id) } returns studyClass.withData { students = mutableListOf() }
                    every { contests.findById(taskContest.id) } returns taskContest
                    every { taskRepository.findById(task.id) } returns task

                    assertRaises(ClassAccessDeniedError(studyClass.id)) { send() }

                    verifyNothingSaved()
                }

                @Test
                fun `should raise ContestAccessDeniedError if contest is outside selected class`() {
                    every { classes.findById(studyClass.id) } returns studyClass.withData { contests = mutableListOf() }
                    every { contests.findById(taskContest.id) } returns taskContest
                    every { taskRepository.findById(task.id) } returns task

                    assertRaises(ContestAccessDeniedError(taskContest.id)) { send() }

                    verifyNothingSaved()
                }

                @Test
                fun `should raise TaskAccessDeniedError if task is outside the contest`() {
                    prepareStudentSend(current = contest)

                    assertRaises(TaskAccessDeniedError(task.id)) { send(contestId = contest.id) }

                    verify { studentEntries wasNot Called }
                    verifyNothingSaved()
                }

                @Test
                fun `should raise ContestNotEnteredError if the contest was entered only in another class`() {
                    stubEntryInOtherClass(contestId = taskContest.id)
                    prepareStudentSend(enteredAt = null)

                    assertRaises(ContestNotEnteredError(taskContest.id)) { send() }

                    verifyNothingSaved()
                }

                @Test
                fun `should raise ContestEndedError if solution is sent exactly at the contest end`() {
                    val current = taskContest.withData {
                        startsAt = firstEntry
                        contestDuration = Duration.between(firstEntry, now)
                    }
                    prepareStudentSend(current = current)

                    assertRaises(ContestEndedError(current.id, now)) { send() }

                    verifyNothingSaved()
                }

                @Test
                fun `should raise ContestAttemptExpiredError if solution is sent exactly at the end of the attempt in selected class`() {
                    val current = taskContest.withData { attemptDuration = Duration.between(firstEntry, now) }
                    prepareStudentSend(current = current)

                    assertRaises(ContestAttemptExpiredError(current.id, now)) { send() }

                    verifyNothingSaved()
                }

                @Test
                fun `should raise SolutionLanguageNotAllowedError if committed task has no developer solution in that language`() {
                    prepareStudentSend()

                    assertRaises(SolutionLanguageNotAllowedError(task.id, TrikSupportedLanguage.Python)) {
                        send(language = TrikSupportedLanguage.Python)
                    }

                    verifyNothingSaved()
                }
            }

            @Nested
            inner class InvariantTests {

                @Test
                fun `should save a new solution and a new submission if the same file was already sent`() {
                    prepareStudentSend()
                    stubEarlierSubmissionOfSameFile(author = student.id.value)

                    val result = send().getOrThrow()

                    assertSame(savedSubmission, result)
                    verifyNewSolutionAndSubmissionSaved()
                }

                @Test
                fun `should not change the task, contest, class or entry when sending a solution`() {
                    prepareStudentSend()

                    send().getOrThrow()

                    verify(exactly = 0) {
                        taskRepository.update(any<Task>())
                        contests.update(any<Contest>())
                        classes.update(any<Class>())
                        studentEntries.findOrCreate(any())
                    }
                }

                @Test
                fun `should raise SolutionLanguageNotAllowedError if the language is only in the working revision`() {
                    prepareStudentSend(currentTask = uncommittedSolutionTask(committed = listOf(81), wip = listOf(81, 82)))
                    stubDeveloperSolutions(
                        stubReferenceSolution(developerSolutionId = 81) { python() },
                        stubReferenceSolution(developerSolutionId = 82) { javaScript() },
                    )

                    assertRaises(SolutionLanguageNotAllowedError(task.id, TrikSupportedLanguage.JavaScript)) { send() }

                    verifyNothingSaved()
                }

                @Test
                fun `should accept a language of the last committed revision of an uncommitted task`() {
                    prepareStudentSend(currentTask = uncommittedSolutionTask(committed = listOf(82), wip = listOf(81)))
                    stubDeveloperSolutions(stubReferenceSolution(developerSolutionId = 82) { javaScript() })

                    val result = send().getOrThrow()

                    assertSame(savedSubmission, result)
                }

                @Test
                fun `should raise SolutionLanguageNotAllowedError for a new task without a committed revision`() {
                    prepareStudentSend(currentTask = testNewTask())

                    assertRaises(SolutionLanguageNotAllowedError(task.id, TrikSupportedLanguage.JavaScript)) { send() }

                    verify { developerSolutionRepository wasNot Called }
                    verifyNothingSaved()
                }

                @Test
                fun `should deny class access despite ownership and communities of other roles`() {
                    val user = studentWithOtherRoles()
                    prepareContextOwnedWithoutEnrollment(user = user, current = taskContest)

                    assertRaises(ClassAccessDeniedError(studyClass.id)) { send(user = user) }

                    verifyNothingSaved()
                }
            }

            @Nested
            inner class ModuleRuleTests {

                @Test
                fun `should propagate a technical exception while saving the solution`() {
                    val failure = IllegalStateException("Solution storage failed")
                    prepareStudentSend()
                    every { solutionRepository.save(any<SolutionData>()) } throws failure

                    val thrown = assertThrows(IllegalStateException::class.java) { send() }

                    assertSame(failure, thrown)
                    verify { submissionRepository wasNot Called }
                }
            }

            private fun send(
                user: MultipleRoleUser = student,
                contestId: ContestId = taskContest.id,
                language: TrikSupportedLanguage = TrikSupportedLanguage.JavaScript,
            ) = operations.sendSolution(
                user = user,
                classId = studyClass.id,
                contestId = contestId,
                taskId = task.id,
                file = file,
                language = language,
            )

            private fun prepareStudentSend(
                current: Contest = taskContest,
                currentTask: Task = solutionTask(),
                enteredAt: Instant? = firstEntry,
            ) {
                prepareStudentTask(current = current, currentTask = currentTask, enteredAt = enteredAt)
                stubDeveloperSolutions(stubReferenceSolution(developerSolutionId = 81) { javaScript() })
                stubSaving()
            }
        }

        private fun verifyNothingSaved() {
            verify(exactly = 0) {
                solutionRepository.save(any<SolutionData>())
                submissionRepository.save(any<SubmissionData>())
            }
        }

        private fun verifyNewSolutionAndSubmissionSaved() {
            verify(exactly = 1) {
                solutionRepository.save(any<SolutionData>())
                submissionRepository.save(any<SubmissionData>())
            }
            verify(exactly = 0) {
                solutionRepository.update(any<Solution>())
                submissionRepository.update(any<Submission>())
            }
        }

        private fun stubSaving() {
            every { solutionRepository.save(capture(sentSolution)) } returns savedSolution
            every { submissionRepository.save(capture(sentSubmission)) } returns savedSubmission
        }

        private fun stubEarlierSubmissionOfSameFile(author: Long) {
            val earlierSolution = solution {
                id = 3
                createdAt = firstEntry
                data = solutionData {
                    file(file)
                    language.python()
                }
            }
            every { solutionRepository.findById(earlierSolution.id) } returns earlierSolution
            every {
                submissionRepository.findGradingByContext(authorId = any(), taskId = task.id, contestId = taskContest.id)
            } returns listOf(testSubmission(submissionId = 51, author = author) { queued() })
        }

        private fun solutionTask(): Task = testTask {
            committed {
                exercises(listOf(1))
                statement(1)
                developerSolutions(listOf(81))
            }
        }

        private fun uncommittedSolutionTask(committed: List<Long>, wip: List<Long>): Task = testTask {
            uncommitted(
                wipBuilder = { developerSolutions(wip) },
                lastCommittedBuilder = {
                    exercises(listOf(1))
                    statement(1)
                    developerSolutions(committed)
                },
            )
        }

        private fun stubReferenceSolution(developerSolutionId: Long, chooseLanguage: LanguageChooser.() -> Unit): DeveloperSolution {
            val reference = solution {
                id = developerSolutionId + 10
                createdAt = Instant.EPOCH
                data = solutionData {
                    file("reference.qrs", "reference".toByteArray())
                    language.chooseLanguage()
                }
            }
            storedSolutions[reference.id] = reference
            every { solutionRepository.load(any<LazyEntityList<SolutionId, Solution>>()) } answers {
                firstArg<LazyEntityList<SolutionId, Solution>>().ids.reversed().map(storedSolutions::getValue)
            }
            return developerSolution {
                id = developerSolutionId
                createdAt = Instant.EPOCH
                data = developerSolutionData {
                    name = "Reference"
                    description = "Reference solution"
                    solution(reference.id.value)
                    expectedScore(100)
                    versionBucket = VersionBucket(UUID(0, developerSolutionId))
                }
            }
        }

        private fun stubDeveloperSolutions(vararg developerSolutions: DeveloperSolution) {
            val ids = developerSolutions.map { developerSolution -> developerSolution.id }
            every {
                developerSolutionRepository.load(match<LazyEntityList<DeveloperSolutionId, DeveloperSolution>> { list -> list.ids == ids })
            } returns developerSolutions.toList()
        }
    }

    private fun prepareParticipantTask(current: Contest = taskContest, currentTask: Task = task, enteredAt: Instant? = firstEntry) {
        prepareParticipantView(current = current, enteredAt = enteredAt)
        every { taskRepository.findById(currentTask.id) } returns currentTask
    }

    private fun prepareStudentTask(current: Contest = taskContest, currentTask: Task = task, enteredAt: Instant? = firstEntry) {
        prepareStudentView(current = current, enteredAt = enteredAt)
        every { taskRepository.findById(currentTask.id) } returns currentTask
    }

    /**
     * Stubs an entry of the student into [contestId] for any class; a later stub for the selected class takes precedence.
     */
    private fun stubEntryInOtherClass(contestId: ContestId) {
        every {
            studentEntries.findByContext(userId = student.id, studyClassId = any(), contestId = contestId)
        } returns studentEntry(selectedClass = otherClass, enteredAt = firstEntry)
    }

    private fun studentWithOtherRoles(): MultipleRoleUser = testMultipleRoleUser {
        roles {
            student {
                data = studentData {}
                memberOf(listOf(4))
            }
            developer {
                data = developerData {}
                memberOf(listOf(4))
            }
            administrator { memberOf(listOf(4)) }
        }
    }

    /**
     * Stubs the selected class and [current] contest owned by [user] and shared to its community, with [task] owned by [user],
     * while [user] is not enrolled in the class.
     */
    private fun prepareContextOwnedWithoutEnrollment(user: MultipleRoleUser, current: Contest) {
        every { classes.findById(studyClass.id) } returns studyClass.withData {
            owner = user.id
            students = mutableListOf()
        }
        every { contests.findById(current.id) } returns current.withData {
            owner = user.id
            sharedTo(listOf(4))
        }
        every { taskRepository.findById(task.id) } returns task.withData { owner = user.id }
    }

    private fun testSubmission(
        submissionId: Long,
        author: Long = participant.id.value,
        at: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        orders: List<Long> = emptyList(),
        chooseStatus: SubmissionStatusChooser.() -> Unit = { graded { status.success { verdict(submissionId) } } },
    ): Submission = submission {
        id = submissionId
        createdAt = at
        data = submissionData {
            author(author)
            solution(3)
            task(0)
            status.chooseStatus()
            kind.grading { contest(taskContest.id.value) }
            judgmentOrders(orders)
        }
    }

    private fun testVerdict(verdictId: Long, scores: List<Int>): Verdict = verdict {
        id = verdictId
        createdAt = Instant.EPOCH
        data = verdictData {
            task(0)
            submission(verdictId)
            scores.forEachIndexed { index, value ->
                testVerdict {
                    score = value
                    test(index.toLong())
                    logs(index.toLong())
                }
            }
        }
    }

    private fun testJudgmentOrder(orderId: Long, score: Int, at: Instant = Instant.parse("2026-01-01T00:00:00Z")): JudgmentOrder =
        judgmentOrder {
            id = orderId
            createdAt = at
            data = judgmentOrderData {
                judge(19)
                submission(51)
                this.score = score
                reason = "Ruling"
            }
        }

    /** Stores a verdict; list loads of verdicts answer with the requested stored ones in reverse request order. */
    private fun stubVerdict(verdictId: Long, scores: List<Int>) {
        storedVerdicts[VerdictId(verdictId)] = testVerdict(verdictId = verdictId, scores = scores)
        every { verdictRepository.load(any<LazyEntityList<VerdictId, Verdict>>()) } answers {
            firstArg<LazyEntityList<VerdictId, Verdict>>().ids.reversed().map(storedVerdicts::getValue)
        }
    }

    /** Stores [orders]; list loads of judgment orders answer with the requested stored ones in reverse request order. */
    private fun stubJudgmentOrders(vararg orders: JudgmentOrder) {
        storedJudgmentOrders.putAll(orders.associateBy { order -> order.id })
        every { judgmentOrderRepository.load(any<LazyEntityList<JudgmentOrderId, JudgmentOrder>>()) } answers {
            firstArg<LazyEntityList<JudgmentOrderId, JudgmentOrder>>().ids.reversed().map(storedJudgmentOrders::getValue)
        }
    }

    private fun Submission.verdictReference(): LazyEntity<VerdictId, Verdict> {
        val status = data.status as? SubmissionStatus.Graded
        val grade = status?.grade as? GradingResult.Success
        return checkNotNull(grade) { "Submission ${id.value} has no successful verdict" }.verdict
    }

    private fun prepareParticipantView(current: Contest = contest, enteredAt: Instant? = null) {
        every { competitions.findById(competition.id) } returns competition
        every { contests.findById(current.id) } returns current
        val entry = enteredAt?.let { at ->
            participantContestEntry {
                id = 71
                createdAt = Instant.EPOCH
                data = participantContestEntryData {
                    participant = this@StudyOperationsTests.participant.id
                    competition = this@StudyOperationsTests.competition.id
                    this.contest = current.id
                    this.enteredAt = at
                }
            }
        }
        every {
            participantEntries.findByContext(participantId = participant.id, competitionId = competition.id, contestId = current.id)
        } returns entry
    }

    private fun prepareStudentView(current: Contest = contest, enteredAt: Instant? = null) {
        every { classes.findById(studyClass.id) } returns studyClass
        every { contests.findById(current.id) } returns current
        val entry = enteredAt?.let { at -> studentEntry(enteredAt = at) }
        every {
            studentEntries.findByContext(userId = student.id, studyClassId = studyClass.id, contestId = current.id)
        } returns entry
    }

    private fun studentEntry(selectedClass: Class = studyClass, enteredAt: Instant) = studentContestEntry {
        id = 72
        createdAt = Instant.EPOCH
        data = studentContestEntryData {
            user = student.id
            studyClass = selectedClass.id
            this.contest = this@StudyOperationsTests.contest.id
            this.enteredAt = enteredAt
        }
    }
}
