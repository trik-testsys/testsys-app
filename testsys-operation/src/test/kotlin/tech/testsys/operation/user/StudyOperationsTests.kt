package tech.testsys.operation.user

import io.mockk.Called
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.builder.api.*
import tech.testsys.domain.builder.util.chooser.SubmissionStatusChooser
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.EntityLoader
import tech.testsys.domain.contract.persistence.repository.ExerciseRepository
import tech.testsys.domain.contract.persistence.repository.JudgmentOrderRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantContestEntryRepository
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.contract.persistence.repository.StudentContestEntryRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.operation.error.*
import tech.testsys.operation.util.*
import java.time.Duration
import java.time.Instant
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
    private val operations = StudyOperations(
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

    @Nested
    inner class ViewContestTests {

        @Nested
        inner class ParticipantTests {

            @Test
            fun `should raise MissedParticipantRoleError if user is a Supervisor`() {
                val user = supervisor {
                    id = 18
                    createdAt = Instant.EPOCH
                    data = supervisorData {
                        accessToken("supervisor", algorithm = HashAlgorithm.Identity)
                        name = "Supervisor"
                    }
                }

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

            @Test
            fun `should return the original unentered contest and perform only reads`() {
                prepareParticipantView()

                val result = operations.viewContest(user = participant, contestId = contest.id).getOrThrow()

                assertSame(contest, result.second)
                assertSame(contest.data, result.second.data)
                assertEquals(contest.version, result.second.version)
                assertNull(result.first)
                verify(exactly = 1) {
                    competitions.findById(competition.id)
                    contests.findById(contest.id)
                    participantEntries.findByContext(participantId = participant.id, competitionId = competition.id, contestId = contest.id)
                }
                confirmVerified(competitions, classes, contests, participantEntries, studentEntries)
            }

            @Test
            fun `should preserve nanosecond precision of the saved first entry and current contest limits`() {
                val enteredAt = Instant.parse("2020-01-01T00:00:00.123456789Z")
                val current = contest.withData {
                    startsAt = Instant.parse("2026-01-01T00:00:00Z")
                    contestDuration = Duration.ofHours(2)
                    attemptDuration = Duration.ofMinutes(30)
                }
                prepareParticipantView(current = current, enteredAt = enteredAt)

                val result = operations.viewContest(user = participant, contestId = current.id).getOrThrow()

                assertSame(current, result.second)
                assertEquals(enteredAt, result.first)
                assertEquals(Instant.parse("2026-01-01T00:00:00Z"), result.second.data.startsAt)
                assertEquals(Instant.parse("2026-01-01T02:00:00Z"), result.second.data.endsAt)
                assertEquals(Duration.ofMinutes(30), result.second.data.attemptDuration)
            }

            @Test
            fun `should preserve lazy task identifiers without resolving them`() {
                val task = testNewTask()
                val current = contest.withData { tasks(listOf(0)) }
                val loader = mockk<EntityLoader<TaskId, Task>>()
                every { loader.load(current.data.tasks) } returns listOf(task)
                prepareParticipantView(current = current)

                val result = operations.viewContest(user = participant, contestId = current.id).getOrThrow()

                assertSame(current, result.second)
                assertSame(current.data.tasks, result.second.data.tasks)
                assertEquals(listOf(TaskId(0)), result.second.data.tasks.ids)
                assertEquals(listOf(task), result.second.data.tasks.load(loader))
                verify(exactly = 1) { loader.load(current.data.tasks) }
            }

            @ParameterizedTest
            @ValueSource(longs = [-100, 4_102_444_800])
            fun `should allow viewing completed and future contests without entering them`(start: Long) {
                val current = contest.withData {
                    startsAt = Instant.ofEpochSecond(start)
                    contestDuration = Duration.ofSeconds(10)
                }
                prepareParticipantView(current = current)

                val result = operations.viewContest(user = participant, contestId = current.id).getOrThrow()

                assertSame(current, result.second)
                assertNull(result.first)
                verify(exactly = 0) { participantEntries.findOrCreate(any()) }
            }

            @Test
            fun `should propagate a technical exception while reading participant entry`() {
                val failure = IllegalStateException("Participant entry storage failed")
                prepareParticipantView()
                every {
                    participantEntries.findByContext(participantId = participant.id, competitionId = competition.id, contestId = contest.id)
                } throws failure

                val thrown = assertThrows(IllegalStateException::class.java) {
                    operations.viewContest(user = participant, contestId = contest.id)
                }

                assertSame(failure, thrown)
            }
        }

        @Nested
        inner class StudentTests {

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

            @Test
            fun `should deny class access despite ownership and communities of other roles`() {
                val user = testMultipleRoleUser {
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
                val ownedClass = studyClass.withData {
                    owner = user.id
                    students = mutableListOf()
                }
                val ownedContest = contest.withData {
                    owner = user.id
                    sharedTo(listOf(4))
                }
                every { classes.findById(studyClass.id) } returns ownedClass
                every { contests.findById(contest.id) } returns ownedContest

                assertRaises(ClassAccessDeniedError(studyClass.id)) {
                    operations.viewContest(user = user, classId = studyClass.id, contestId = contest.id)
                }
            }

            @Test
            fun `should return an unentered original contest and perform only reads`() {
                prepareStudentView()

                val result = operations.viewContest(user = student, classId = studyClass.id, contestId = contest.id).getOrThrow()

                assertSame(contest, result.second)
                assertNull(result.first)
                verify(exactly = 1) {
                    classes.findById(studyClass.id)
                    contests.findById(contest.id)
                    studentEntries.findByContext(userId = student.id, studyClassId = studyClass.id, contestId = contest.id)
                }
                confirmVerified(competitions, classes, contests, participantEntries, studentEntries)
            }

            @Test
            fun `should return the exact first entry time and original contest for the selected class`() {
                val enteredAt = Instant.parse("2020-01-01T00:00:00.123456789Z")
                val current = contest.withData { tasks(listOf(31, 32)) }
                prepareStudentView(current = current, enteredAt = enteredAt)

                val result = operations.viewContest(user = student, classId = studyClass.id, contestId = current.id).getOrThrow()

                assertSame(current, result.second)
                assertEquals(enteredAt, result.first)
                assertEquals(listOf(TaskId(31), TaskId(32)), result.second.data.tasks.ids)
            }

            @Test
            fun `should return no entry if the same contest was entered only in another class`() {
                val otherClass = `class` {
                    id = 24
                    createdAt = Instant.EPOCH
                    data = studyClass.data
                }
                val otherEntry = studentEntry(selectedClass = otherClass, enteredAt = Instant.ofEpochSecond(100))
                prepareStudentView()
                every {
                    studentEntries.findByContext(userId = student.id, studyClassId = otherClass.id, contestId = contest.id)
                } returns otherEntry

                val result = operations.viewContest(user = student, classId = studyClass.id, contestId = contest.id).getOrThrow()

                assertNull(result.first)
                verify(exactly = 0) {
                    studentEntries.findByContext(userId = student.id, studyClassId = ClassId(24), contestId = contest.id)
                    studentEntries.findOrCreate(any())
                }
            }

            @ParameterizedTest
            @ValueSource(longs = [-100, 4_102_444_800])
            fun `should allow viewing completed and future class contests without entering them`(start: Long) {
                val current = contest.withData {
                    startsAt = Instant.ofEpochSecond(start)
                    contestDuration = Duration.ofSeconds(10)
                }
                prepareStudentView(current = current)

                val result = operations.viewContest(user = student, classId = studyClass.id, contestId = current.id).getOrThrow()

                assertSame(current, result.second)
                assertNull(result.first)
                verify(exactly = 0) { studentEntries.findOrCreate(any()) }
            }

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

    @Nested
    inner class ViewTaskTests {

        @Nested
        inner class ParticipantTests {

            @Test
            fun `should raise MissedParticipantRoleError if user is a Supervisor`() {
                val user = supervisor {
                    id = 18
                    createdAt = Instant.EPOCH
                    data = supervisorData {
                        accessToken("supervisor", algorithm = HashAlgorithm.Identity)
                        name = "Supervisor"
                    }
                }

                assertRaises(MissedParticipantRoleError) {
                    operations.viewTask(user = user, contestId = taskContest.id, taskId = task.id)
                }

                verify { competitions wasNot Called }
            }

            @Test
            fun `should raise CompetitionNotExistsError if participant competition is missing`() {
                every { competitions.findById(competition.id) } returns null

                assertRaises(CompetitionNotExistsError(competition.id)) {
                    operations.viewTask(user = participant, contestId = taskContest.id, taskId = task.id)
                }
            }

            @Test
            fun `should raise ContestNotExistsError if contest is missing`() {
                every { competitions.findById(competition.id) } returns competition
                every { contests.findById(taskContest.id) } returns null

                assertRaises(ContestNotExistsError(taskContest.id)) {
                    operations.viewTask(user = participant, contestId = taskContest.id, taskId = task.id)
                }
            }

            @Test
            fun `should raise TaskNotExistsError if task is missing`() {
                every { competitions.findById(competition.id) } returns competition
                every { contests.findById(taskContest.id) } returns taskContest
                every { taskRepository.findById(task.id) } returns null

                assertRaises(TaskNotExistsError(task.id)) {
                    operations.viewTask(user = participant, contestId = taskContest.id, taskId = task.id)
                }
            }

            @Test
            fun `should raise ContestAccessDeniedError if contest belongs to another competition`() {
                every { competitions.findById(competition.id) } returns competition.withData { contests = mutableListOf() }
                every { contests.findById(taskContest.id) } returns taskContest
                every { taskRepository.findById(task.id) } returns task

                assertRaises(ContestAccessDeniedError(taskContest.id)) {
                    operations.viewTask(user = participant, contestId = taskContest.id, taskId = task.id)
                }
            }

            @Test
            fun `should raise TaskAccessDeniedError if task is outside the contest`() {
                prepareParticipantTask(current = contest)

                assertRaises(TaskAccessDeniedError(task.id)) {
                    operations.viewTask(user = participant, contestId = contest.id, taskId = task.id)
                }

                verify { participantEntries wasNot Called }
            }

            @Test
            fun `should raise ContestNotEnteredError if participant has not entered the contest`() {
                prepareParticipantTask(enteredAt = null)

                assertRaises(ContestNotEnteredError(taskContest.id)) {
                    operations.viewTask(user = participant, contestId = taskContest.id, taskId = task.id)
                }

                verify { submissionRepository wasNot Called }
            }

            @Test
            fun `should return the original task, submissions of the participant and the best submission`() {
                val submission = testSubmission(submissionId = 51)
                prepareParticipantTask()
                every {
                    submissionRepository.findGradingByContext(authorId = participant.id, taskId = task.id, contestId = taskContest.id)
                } returns listOf(submission)
                stubVerdict(verdictId = 51, scores = listOf(40, 60))

                val (resultTask, submissions, best) = operations.viewTask(
                    user = participant,
                    contestId = taskContest.id,
                    taskId = task.id,
                ).getOrThrow()

                assertSame(task, resultTask)
                assertEquals(listOf(submission), submissions)
                assertSame(submission, best)
                verify(exactly = 1) {
                    competitions.findById(competition.id)
                    contests.findById(taskContest.id)
                    taskRepository.findById(task.id)
                    participantEntries.findByContext(
                        participantId = participant.id,
                        competitionId = competition.id,
                        contestId = taskContest.id,
                    )
                    submissionRepository.findGradingByContext(authorId = participant.id, taskId = task.id, contestId = taskContest.id)
                    verdictRepository.load(submission.verdictReference())
                }
                confirmVerified(
                    competitions,
                    classes,
                    contests,
                    participantEntries,
                    studentEntries,
                    taskRepository,
                    submissionRepository,
                    verdictRepository,
                    judgmentOrderRepository,
                    statementRepository,
                    exerciseRepository,
                )
            }

            @ParameterizedTest
            @ValueSource(longs = [-100, 4_102_444_800])
            fun `should allow viewing a task of completed and future contests without saving an entry`(start: Long) {
                val current = taskContest.withData {
                    startsAt = Instant.ofEpochSecond(start)
                    contestDuration = Duration.ofSeconds(10)
                }
                prepareParticipantTask(current = current)
                every {
                    submissionRepository.findGradingByContext(authorId = participant.id, taskId = task.id, contestId = current.id)
                } returns emptyList()

                val (resultTask, submissions, best) = operations.viewTask(
                    user = participant,
                    contestId = current.id,
                    taskId = task.id,
                ).getOrThrow()

                assertSame(task, resultTask)
                assertEquals(emptyList<Submission>(), submissions)
                assertNull(best)
                verify(exactly = 0) { participantEntries.findOrCreate(any()) }
            }

            @Test
            fun `should propagate a technical exception while reading submissions`() {
                val failure = IllegalStateException("Submission storage failed")
                prepareParticipantTask()
                every {
                    submissionRepository.findGradingByContext(authorId = participant.id, taskId = task.id, contestId = taskContest.id)
                } throws failure

                val thrown = assertThrows(IllegalStateException::class.java) {
                    operations.viewTask(user = participant, contestId = taskContest.id, taskId = task.id)
                }

                assertSame(failure, thrown)
            }
        }

        @Nested
        inner class StudentTests {

            @Test
            fun `should raise MissedStudentRoleError if user has other roles without Student`() {
                val user = testMultipleRoleUser { roles { developer { data = developerData {} } } }

                assertRaises(MissedStudentRoleError) {
                    operations.viewTask(user = user, classId = studyClass.id, contestId = taskContest.id, taskId = task.id)
                }

                verify { classes wasNot Called }
            }

            @Test
            fun `should raise ClassNotExistsError if selected class is missing`() {
                every { classes.findById(studyClass.id) } returns null

                assertRaises(ClassNotExistsError(studyClass.id)) {
                    operations.viewTask(user = student, classId = studyClass.id, contestId = taskContest.id, taskId = task.id)
                }
            }

            @Test
            fun `should raise ContestNotExistsError if contest is missing`() {
                every { classes.findById(studyClass.id) } returns studyClass
                every { contests.findById(taskContest.id) } returns null

                assertRaises(ContestNotExistsError(taskContest.id)) {
                    operations.viewTask(user = student, classId = studyClass.id, contestId = taskContest.id, taskId = task.id)
                }
            }

            @Test
            fun `should raise TaskNotExistsError if task is missing`() {
                every { classes.findById(studyClass.id) } returns studyClass
                every { contests.findById(taskContest.id) } returns taskContest
                every { taskRepository.findById(task.id) } returns null

                assertRaises(TaskNotExistsError(task.id)) {
                    operations.viewTask(user = student, classId = studyClass.id, contestId = taskContest.id, taskId = task.id)
                }
            }

            @Test
            fun `should raise ClassAccessDeniedError if student is not enrolled in selected class`() {
                every { classes.findById(studyClass.id) } returns studyClass.withData { students = mutableListOf() }
                every { contests.findById(taskContest.id) } returns taskContest
                every { taskRepository.findById(task.id) } returns task

                assertRaises(ClassAccessDeniedError(studyClass.id)) {
                    operations.viewTask(user = student, classId = studyClass.id, contestId = taskContest.id, taskId = task.id)
                }
            }

            @Test
            fun `should raise ContestAccessDeniedError if contest is outside selected class`() {
                every { classes.findById(studyClass.id) } returns studyClass.withData { contests = mutableListOf() }
                every { contests.findById(taskContest.id) } returns taskContest
                every { taskRepository.findById(task.id) } returns task

                assertRaises(ContestAccessDeniedError(taskContest.id)) {
                    operations.viewTask(user = student, classId = studyClass.id, contestId = taskContest.id, taskId = task.id)
                }
            }

            @Test
            fun `should raise TaskAccessDeniedError if task is outside the contest`() {
                prepareStudentTask(current = contest)

                assertRaises(TaskAccessDeniedError(task.id)) {
                    operations.viewTask(user = student, classId = studyClass.id, contestId = contest.id, taskId = task.id)
                }

                verify { studentEntries wasNot Called }
            }

            @Test
            fun `should raise ContestNotEnteredError if the contest was entered only in another class`() {
                val otherClass = `class` {
                    id = 24
                    createdAt = Instant.EPOCH
                    data = studyClass.data
                }
                prepareStudentTask(enteredAt = null)
                every {
                    studentEntries.findByContext(userId = student.id, studyClassId = otherClass.id, contestId = taskContest.id)
                } returns studentEntry(selectedClass = otherClass, enteredAt = firstEntry)

                assertRaises(ContestNotEnteredError(taskContest.id)) {
                    operations.viewTask(user = student, classId = studyClass.id, contestId = taskContest.id, taskId = task.id)
                }

                verify { submissionRepository wasNot Called }
            }

            @Test
            fun `should return the original task, submissions of the student and the best submission`() {
                val submission = testSubmission(submissionId = 51, author = student.id.value)
                prepareStudentTask()
                every {
                    submissionRepository.findGradingByContext(authorId = student.id, taskId = task.id, contestId = taskContest.id)
                } returns listOf(submission)
                stubVerdict(verdictId = 51, scores = listOf(100))

                val (resultTask, submissions, best) = operations.viewTask(
                    user = student,
                    classId = studyClass.id,
                    contestId = taskContest.id,
                    taskId = task.id,
                ).getOrThrow()

                assertSame(task, resultTask)
                assertEquals(listOf(submission), submissions)
                assertSame(submission, best)
                verify(exactly = 1) {
                    studentEntries.findByContext(userId = student.id, studyClassId = studyClass.id, contestId = taskContest.id)
                    submissionRepository.findGradingByContext(authorId = student.id, taskId = task.id, contestId = taskContest.id)
                }
                verify(exactly = 0) { studentEntries.findOrCreate(any()) }
            }

            @ParameterizedTest
            @ValueSource(longs = [-100, 4_102_444_800])
            fun `should allow viewing a task of completed and future class contests without saving an entry`(start: Long) {
                val current = taskContest.withData {
                    startsAt = Instant.ofEpochSecond(start)
                    contestDuration = Duration.ofSeconds(10)
                }
                prepareStudentTask(current = current)
                every {
                    submissionRepository.findGradingByContext(authorId = student.id, taskId = task.id, contestId = current.id)
                } returns emptyList()

                val (resultTask, _, best) = operations.viewTask(
                    user = student,
                    classId = studyClass.id,
                    contestId = current.id,
                    taskId = task.id,
                ).getOrThrow()

                assertSame(task, resultTask)
                assertNull(best)
                verify(exactly = 0) { studentEntries.findOrCreate(any()) }
            }

            @Test
            fun `should propagate a technical exception while reading submissions`() {
                val failure = IllegalStateException("Submission storage failed")
                prepareStudentTask()
                every {
                    submissionRepository.findGradingByContext(authorId = student.id, taskId = task.id, contestId = taskContest.id)
                } throws failure

                val thrown = assertThrows(IllegalStateException::class.java) {
                    operations.viewTask(user = student, classId = studyClass.id, contestId = taskContest.id, taskId = task.id)
                }

                assertSame(failure, thrown)
            }
        }

        @Nested
        inner class BestSubmissionTests {

            @BeforeEach
            fun prepare() {
                prepareParticipantTask()
            }

            @Test
            fun `should prefer the judgment order score over the verdict score`() {
                val judged = testSubmission(submissionId = 51, orders = listOf(61))
                val graded = testSubmission(submissionId = 52)
                stubVerdict(verdictId = 51, scores = listOf(100))
                stubVerdict(verdictId = 52, scores = listOf(50))
                stubJudgmentOrders(testJudgmentOrder(orderId = 61, score = 20))

                val best = viewSubmissions(judged, graded).third

                assertSame(graded, best)
            }

            @Test
            fun `should use the score of the judgment order created last`() {
                val judged = testSubmission(submissionId = 51, orders = listOf(61, 62))
                val graded = testSubmission(submissionId = 52)
                stubVerdict(verdictId = 52, scores = listOf(50))
                stubJudgmentOrders(
                    testJudgmentOrder(orderId = 61, score = 80, at = Instant.parse("2026-01-02T00:00:00Z")),
                    testJudgmentOrder(orderId = 62, score = 10, at = Instant.parse("2026-01-01T00:00:00Z")),
                )

                val best = viewSubmissions(judged, graded).third

                assertSame(judged, best)
            }

            @Test
            fun `should use the judgment order with the greater id if orders were created at the same time`() {
                val judged = testSubmission(submissionId = 51, orders = listOf(62, 61))
                val graded = testSubmission(submissionId = 52)
                stubVerdict(verdictId = 52, scores = listOf(50))
                stubJudgmentOrders(
                    testJudgmentOrder(orderId = 62, score = 80),
                    testJudgmentOrder(orderId = 61, score = 10),
                )

                val best = viewSubmissions(judged, graded).third

                assertSame(judged, best)
            }

            @Test
            fun `should choose the earlier submission if final scores are equal`() {
                val later = testSubmission(submissionId = 51, at = Instant.parse("2026-01-02T00:00:00Z"))
                val earlier = testSubmission(submissionId = 52, at = Instant.parse("2026-01-01T00:00:00Z"))
                stubVerdict(verdictId = 51, scores = listOf(30, 20))
                stubVerdict(verdictId = 52, scores = listOf(50))

                val best = viewSubmissions(later, earlier).third

                assertSame(earlier, best)
            }

            @Test
            fun `should choose the submission with the smaller id if final scores and creation times are equal`() {
                val greater = testSubmission(submissionId = 52)
                val smaller = testSubmission(submissionId = 51)
                stubVerdict(verdictId = 51, scores = listOf(50))
                stubVerdict(verdictId = 52, scores = listOf(50))

                val best = viewSubmissions(greater, smaller).third

                assertSame(smaller, best)
            }

            @Test
            fun `should ignore submissions without a successful verdict`() {
                val graded = testSubmission(submissionId = 51)
                val unsuccessful = listOf(
                    testSubmission(submissionId = 52) { queued() },
                    testSubmission(submissionId = 53) { inProgress() },
                    testSubmission(submissionId = 54, orders = listOf(64)) { graded { status.error { description = "Crash" } } },
                    testSubmission(submissionId = 55, orders = listOf(65)) { graded { status.timeout() } },
                )
                stubVerdict(verdictId = 51, scores = listOf(1))

                val best = viewSubmissions(*(unsuccessful + graded).toTypedArray()).third

                assertSame(graded, best)
                verify { judgmentOrderRepository wasNot Called }
            }

            @Test
            fun `should return no best submission if none has a successful verdict`() {
                val submissions = arrayOf(
                    testSubmission(submissionId = 52) { queued() },
                    testSubmission(submissionId = 54) { graded { status.error { description = "Crash" } } },
                )

                val (_, resultSubmissions, best) = viewSubmissions(*submissions)

                assertEquals(submissions.toList(), resultSubmissions)
                assertNull(best)
                verify { verdictRepository wasNot Called }
            }

            @Test
            fun `should keep verdict references of returned submissions unresolved`() {
                val submission = testSubmission(submissionId = 51)
                stubVerdict(verdictId = 51, scores = listOf(10))
                val loader = mockk<EntityLoader<VerdictId, Verdict>>()
                val verdict = testVerdict(verdictId = 51, scores = listOf(10))
                every { loader.load(submission.verdictReference()) } returns verdict

                val (_, submissions, _) = viewSubmissions(submission)

                assertSame(verdict, submissions.single().verdictReference().load(loader))
                verify(exactly = 1) { loader.load(submission.verdictReference()) }
            }

            private fun viewSubmissions(vararg submissions: Submission) = run {
                every {
                    submissionRepository.findGradingByContext(authorId = participant.id, taskId = task.id, contestId = taskContest.id)
                } returns submissions.toList()
                operations.viewTask(user = participant, contestId = taskContest.id, taskId = task.id).getOrThrow()
            }
        }
    }

    @Nested
    inner class DownloadTaskResourceTests {

        private val statement = testStatement(statementId = 1)
        private val exercise = testExercise(exerciseId = 1)

        @Nested
        inner class ParticipantTests {

            @Test
            fun `should raise MissedParticipantRoleError if user is a Supervisor`() {
                val user = supervisor {
                    id = 18
                    createdAt = Instant.EPOCH
                    data = supervisorData {
                        accessToken("supervisor", algorithm = HashAlgorithm.Identity)
                        name = "Supervisor"
                    }
                }

                assertRaises(MissedParticipantRoleError) {
                    operations.downloadTaskResource(user = user, contestId = taskContest.id, taskId = task.id, resourceId = statement.id)
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
                    operations.downloadTaskResource(
                        user = participant,
                        contestId = contest.id,
                        taskId = task.id,
                        resourceId = statement.id,
                    )
                }
            }

            @Test
            fun `should raise ContestNotEnteredError if participant has not entered the contest`() {
                prepareParticipantTask(enteredAt = null)

                assertRaises(ContestNotEnteredError(taskContest.id)) {
                    download(resourceId = statement.id)
                }

                verify { statementRepository wasNot Called }
            }

            @Test
            fun `should return the file of the committed statement`() {
                prepareParticipantTask()
                every { statementRepository.findById(statement.id) } returns statement

                val file = download(resourceId = statement.id).getOrThrow()

                assertSame(statement.data.file, file)
            }

            @Test
            fun `should return the file of a committed exercise`() {
                prepareParticipantTask()
                every { exerciseRepository.findById(exercise.id) } returns exercise

                val file = download(resourceId = exercise.id).getOrThrow()

                assertSame(exercise.data.file, file)
            }

            @Test
            fun `should return the committed statement of an Uncommitted task`() {
                prepareParticipantTask(currentTask = uncommittedTask())
                every { statementRepository.findById(statement.id) } returns statement

                val file = download(resourceId = statement.id).getOrThrow()

                assertSame(statement.data.file, file)
            }

            @Test
            fun `should raise ResourceNotInCommittedTaskError for a statement of the work-in-progress revision`() {
                prepareParticipantTask(currentTask = uncommittedTask())

                assertRaises(ResourceNotInCommittedTaskError(task.id, StatementId(2))) {
                    download(resourceId = StatementId(2))
                }

                verify { statementRepository wasNot Called }
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
            fun `should raise ResourceNotInCommittedTaskError for an exercise outside the committed revision`() {
                prepareParticipantTask()

                assertRaises(ResourceNotInCommittedTaskError(task.id, ExerciseId(5))) {
                    download(resourceId = ExerciseId(5))
                }
            }

            @Test
            fun `should raise ResourceNotInCommittedTaskError for a test`() {
                val taskWithTest = testTask {
                    committed {
                        exercises(listOf(1))
                        statement(1)
                        tests(listOf(7))
                    }
                }
                prepareParticipantTask(currentTask = taskWithTest)

                assertRaises(ResourceNotInCommittedTaskError(task.id, TestId(7))) {
                    download(resourceId = TestId(7))
                }
            }

            @Test
            fun `should raise ResourceNotInCommittedTaskError if the task has no committed revision`() {
                prepareParticipantTask(currentTask = testTask { new { statement = StatementId(1) } })

                assertRaises(ResourceNotInCommittedTaskError(task.id, statement.id)) {
                    download(resourceId = statement.id)
                }

                verify { statementRepository wasNot Called }
            }

            @Test
            fun `should allow downloading after the contest end without saving an entry`() {
                val current = taskContest.withData {
                    startsAt = Instant.ofEpochSecond(-100)
                    contestDuration = Duration.ofSeconds(10)
                }
                prepareParticipantTask(current = current)
                every { statementRepository.findById(statement.id) } returns statement

                val file = download(resourceId = statement.id).getOrThrow()

                assertSame(statement.data.file, file)
                verify(exactly = 0) { participantEntries.findOrCreate(any()) }
            }

            private fun download(resourceId: DomainId) =
                operations.downloadTaskResource(user = participant, contestId = taskContest.id, taskId = task.id, resourceId = resourceId)
        }

        @Nested
        inner class StudentTests {

            @Test
            fun `should raise MissedStudentRoleError if user has other roles without Student`() {
                val user = testMultipleRoleUser { roles { developer { data = developerData {} } } }

                assertRaises(MissedStudentRoleError) {
                    operations.downloadTaskResource(
                        user = user,
                        classId = studyClass.id,
                        contestId = taskContest.id,
                        taskId = task.id,
                        resourceId = statement.id,
                    )
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
                    operations.downloadTaskResource(
                        user = student,
                        classId = studyClass.id,
                        contestId = contest.id,
                        taskId = task.id,
                        resourceId = statement.id,
                    )
                }
            }

            @Test
            fun `should raise ContestNotEnteredError if the contest was entered only in another class`() {
                val otherClass = `class` {
                    id = 24
                    createdAt = Instant.EPOCH
                    data = studyClass.data
                }
                prepareStudentTask(enteredAt = null)
                every {
                    studentEntries.findByContext(userId = student.id, studyClassId = otherClass.id, contestId = taskContest.id)
                } returns studentEntry(selectedClass = otherClass, enteredAt = firstEntry)

                assertRaises(ContestNotEnteredError(taskContest.id)) {
                    download(resourceId = statement.id)
                }

                verify { statementRepository wasNot Called }
            }

            @Test
            fun `should return the file of a committed exercise`() {
                prepareStudentTask()
                every { exerciseRepository.findById(exercise.id) } returns exercise

                val file = download(resourceId = exercise.id).getOrThrow()

                assertSame(exercise.data.file, file)
            }

            @Test
            fun `should raise ResourceNotInCommittedTaskError for a statement of the work-in-progress revision`() {
                prepareStudentTask(currentTask = uncommittedTask())

                assertRaises(ResourceNotInCommittedTaskError(task.id, StatementId(2))) {
                    download(resourceId = StatementId(2))
                }
            }

            private fun download(resourceId: DomainId) = operations.downloadTaskResource(
                user = student,
                classId = studyClass.id,
                contestId = taskContest.id,
                taskId = task.id,
                resourceId = resourceId,
            )
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

    private fun prepareParticipantTask(current: Contest = taskContest, currentTask: Task = task, enteredAt: Instant? = firstEntry) {
        prepareParticipantView(current = current, enteredAt = enteredAt)
        every { taskRepository.findById(currentTask.id) } returns currentTask
    }

    private fun prepareStudentTask(current: Contest = taskContest, currentTask: Task = task, enteredAt: Instant? = firstEntry) {
        prepareStudentView(current = current, enteredAt = enteredAt)
        every { taskRepository.findById(currentTask.id) } returns currentTask
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

    private fun stubVerdict(verdictId: Long, scores: List<Int>) {
        every {
            verdictRepository.load(match<LazyEntity<VerdictId, Verdict>> { reference -> reference.id == VerdictId(verdictId) })
        } returns testVerdict(verdictId = verdictId, scores = scores)
    }

    private fun stubJudgmentOrders(vararg orders: JudgmentOrder) {
        val ids = orders.map { order -> order.id }.toSet()
        every {
            judgmentOrderRepository.load(match<LazyEntityList<JudgmentOrderId, JudgmentOrder>> { list -> list.ids.toSet() == ids })
        } returns orders.toList()
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
