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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.builder.api.*
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.EntityLoader
import tech.testsys.domain.contract.persistence.repository.ParticipantContestEntryRepository
import tech.testsys.domain.contract.persistence.repository.StudentContestEntryRepository
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.operation.error.*
import tech.testsys.operation.util.*
import java.time.Duration
import java.time.Instant

class StudyOperationsTests {

    private val competitions = mockk<CompetitionRepository>()
    private val classes = mockk<ClassRepository>()
    private val contests = mockk<ContestRepository>()
    private val participantEntries = mockk<ParticipantContestEntryRepository>()
    private val studentEntries = mockk<StudentContestEntryRepository>()
    private val operations = StudyOperations(
        competitionRepository = competitions,
        classRepository = classes,
        contestRepository = contests,
        participantContestEntryRepository = participantEntries,
        studentContestEntryRepository = studentEntries,
    )
    private val participant = testParticipant()
    private val student = testStudent { data = studentData {} }
    private val competition = testCompetition { contests(listOf(19)) }
    private val studyClass = testStudyClass {
        students(listOf(0))
        contests(listOf(19))
    }
    private val contest = testContest()

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
