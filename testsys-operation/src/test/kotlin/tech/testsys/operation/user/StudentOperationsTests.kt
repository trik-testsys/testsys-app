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
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.builder.api.`class`
import tech.testsys.domain.builder.api.classInvite
import tech.testsys.domain.builder.api.classInviteData
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.builder.api.studentContestEntry
import tech.testsys.domain.builder.api.studentContestEntryData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.ClassInviteRepository
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.StudentContestEntryRepository
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.entry.StudentContestEntryData
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.ClassInviteId
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.operation.error.ClassAccessDeniedError
import tech.testsys.operation.error.ClassInviteCodeExpiredError
import tech.testsys.operation.error.ClassInviteCodeNotValidError
import tech.testsys.operation.error.ClassNotExistsError
import tech.testsys.operation.error.ContestAccessDeniedError
import tech.testsys.operation.error.ContestEndedError
import tech.testsys.operation.error.ContestNotExistsError
import tech.testsys.operation.error.ContestNotStartedError
import tech.testsys.operation.error.MissedStudentRoleError
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.testContest
import tech.testsys.operation.util.testMultipleRoleUser
import tech.testsys.operation.util.testStudent
import tech.testsys.operation.util.testStudyClass
import java.time.Clock
import java.time.Duration
import java.time.Instant
import tech.testsys.domain.builder.api.contest as contestEntity

class StudentOperationsTests {

    private val groups = mockk<ClassRepository>()
    private val contests = mockk<ContestRepository>()
    private val entries = mockk<StudentContestEntryRepository>()
    private val clock = mockk<Clock>()
    private val invites = mockk<ClassInviteRepository>()
    private val operations = StudentOperations(
        classRepository = groups,
        contestRepository = contests,
        contestEntryRepository = entries,
        clock = clock,
        classInviteRepository = invites,
    )
    private val user = testStudent { data = studentData {} }
    private val group = testStudyClass {
        students(listOf(0))
        contests(listOf(19))
    }
    private val contest = testContest()
    private fun savedEntry(at: Instant = Instant.ofEpochSecond(100), classId: Long = group.id.value, contestId: Long = contest.id.value) =
        studentContestEntry {
            id = 71
            createdAt = Instant.EPOCH
            data = studentContestEntryData {
                this.user = this@StudentOperationsTests.user.id
                studyClass = ClassId(classId)
                this.contest = ContestId(contestId)
                enteredAt = at
            }
        }

    private fun contestWithId(contestId: Long): Contest = contestEntity {
        id = contestId
        createdAt = contest.createdAt
        data = contest.data
    }

    private fun classWithId(classId: Long, base: Class = group): Class = `class` {
        id = classId
        createdAt = base.createdAt
        data = base.data
    }

    @Nested
    inner class ViewContestsTests {

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should return null entry time with the contest if the student has not entered it`() {
                every { groups.findById(group.id) } returns group
                every { contests.load(any<LazyEntityList<ContestId, Contest>>()) } returns listOf(contest)
                every {
                    entries.findByContests(userId = user.id, studyClassId = group.id, contestIds = listOf(contest.id))
                } returns emptyList()

                val result = operations.viewContests(user = user, classId = group.id).getOrThrow()

                assertEquals(listOf(null to contest), result)
            }

            @Test
            fun `should return the persisted first entry time with the entered contest`() {
                val entry = savedEntry()
                every { groups.findById(group.id) } returns group
                every { contests.load(any<LazyEntityList<ContestId, Contest>>()) } returns listOf(contest)
                every {
                    entries.findByContests(userId = user.id, studyClassId = group.id, contestIds = listOf(contest.id))
                } returns listOf(entry)

                val result = operations.viewContests(user = user, classId = group.id).getOrThrow()

                assertEquals(listOf(Instant.ofEpochSecond(100) to contest), result)
            }

            @Test
            fun `should return an empty list if the class has no contests`() {
                every { groups.findById(group.id) } returns group.withData { this.contests = mutableListOf() }
                every { contests.load(any<LazyEntityList<ContestId, Contest>>()) } returns emptyList()
                every { entries.findByContests(userId = user.id, studyClassId = group.id, contestIds = emptyList()) } returns emptyList()

                val result = operations.viewContests(user = user, classId = group.id).getOrThrow()

                assertEquals(emptyList<Pair<Instant?, Contest>>(), result)
            }

            @ParameterizedTest
            @ValueSource(longs = [-100, 100])
            fun `should include a completed or a future contest with null entry time`(start: Long) {
                val scheduled = contest.withData {
                    startsAt = Instant.ofEpochSecond(start)
                    contestDuration = Duration.ofSeconds(10)
                }
                every { groups.findById(group.id) } returns group
                every { contests.load(any<LazyEntityList<ContestId, Contest>>()) } returns listOf(scheduled)
                every {
                    entries.findByContests(userId = user.id, studyClassId = group.id, contestIds = listOf(contest.id))
                } returns emptyList()

                val result = operations.viewContests(user = user, classId = group.id).getOrThrow()

                assertEquals(listOf(null to scheduled), result)
            }
        }

        @Nested
        inner class RefusalTests {

            @ParameterizedTest
            @MethodSource("tech.testsys.operation.user.StudentOperationsTests#usersWithoutStudentRole")
            fun `should raise MissedStudentRoleError before reading classes if user has no Student role`(wrongRole: MultipleRoleUser) {
                assertRaises(MissedStudentRoleError) {
                    operations.viewContests(user = wrongRole, classId = group.id)
                }

                verify { groups wasNot Called }
            }

            @Test
            fun `should raise ClassNotExistsError if the selected class does not exist`() {
                every { groups.findById(group.id) } returns null

                assertRaises(ClassNotExistsError(group.id)) {
                    operations.viewContests(user = user, classId = group.id)
                }
            }

            @Test
            fun `should raise ClassAccessDeniedError if the student is not a member of the selected class`() {
                every { groups.findById(group.id) } returns group.withData { students = mutableListOf() }

                assertRaises(ClassAccessDeniedError(group.id)) {
                    operations.viewContests(user = user, classId = group.id)
                }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should neither save an entry nor read the clock when viewing contests`() {
                every { groups.findById(group.id) } returns group
                every { contests.load(any<LazyEntityList<ContestId, Contest>>()) } returns listOf(contest)
                every {
                    entries.findByContests(userId = user.id, studyClassId = group.id, contestIds = listOf(contest.id))
                } returns emptyList()

                operations.viewContests(user = user, classId = group.id).getOrThrow()

                verify(exactly = 0) { entries.findOrCreate(any()) }
                verify { clock wasNot Called }
            }

            @Test
            fun `should not update contests or the class when viewing contests`() {
                every { groups.findById(group.id) } returns group
                every { contests.load(any<LazyEntityList<ContestId, Contest>>()) } returns listOf(contest)
                every {
                    entries.findByContests(userId = user.id, studyClassId = group.id, contestIds = listOf(contest.id))
                } returns emptyList()

                operations.viewContests(user = user, classId = group.id).getOrThrow()

                verify(exactly = 0) { contests.update(any<Contest>()) }
                verify(exactly = 0) { groups.update(any<Class>()) }
            }

            @Test
            fun `should raise ClassAccessDeniedError if the user owns the class as a Manager but is not its student`() {
                val studentAndOwner = testMultipleRoleUser {
                    roles {
                        student { data = studentData {} }
                        manager { data = managerData { classes(listOf(23)) } }
                    }
                }
                every { groups.findById(group.id) } returns group.withData {
                    owner(0)
                    students = mutableListOf()
                }

                assertRaises(ClassAccessDeniedError(group.id)) {
                    operations.viewContests(user = studentAndOwner, classId = group.id)
                }
            }

            @Test
            fun `should attach the entry time only to the entered contest if the class has several contests`() {
                val entered = contestWithId(20)
                every { groups.findById(group.id) } returns group.withData { contests(listOf(19, 20)) }
                every { contests.load(any<LazyEntityList<ContestId, Contest>>()) } returns listOf(contest, entered)
                every {
                    entries.findByContests(
                        userId = user.id,
                        studyClassId = group.id,
                        contestIds = listOf(ContestId(19), ContestId(20)),
                    )
                } returns listOf(savedEntry(at = Instant.ofEpochSecond(300), contestId = 20))

                val result = operations.viewContests(user = user, classId = group.id).getOrThrow()

                assertEquals(listOf(null to contest, Instant.ofEpochSecond(300) to entered), result)
            }

            @Test
            fun `should not use the entry time of the same contest in another class`() {
                every { groups.findById(group.id) } returns group
                every { contests.load(any<LazyEntityList<ContestId, Contest>>()) } returns listOf(contest)
                every {
                    entries.findByContests(userId = user.id, studyClassId = group.id, contestIds = listOf(contest.id))
                } returns emptyList()
                every {
                    entries.findByContests(userId = user.id, studyClassId = ClassId(31), contestIds = listOf(contest.id))
                } returns listOf(savedEntry(classId = 31))

                val result = operations.viewContests(user = user, classId = group.id).getOrThrow()

                assertEquals(listOf(null to contest), result)
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should propagate storage exceptions`() {
                val failure = IllegalStateException("storage failed")
                every { groups.findById(group.id) } throws failure

                val thrown = assertThrows(IllegalStateException::class.java) {
                    operations.viewContests(user = user, classId = group.id)
                }

                assertSame(failure, thrown)
            }
        }
    }

    @Nested
    inner class EnterContestTests {

        @Nested
        inner class HappyPathTests {

            @ParameterizedTest
            @ValueSource(longs = [100, 159])
            fun `should save the first entry with the current time exactly at the start and just before the end`(seconds: Long) {
                val scheduled = contest.withData {
                    startsAt = Instant.ofEpochSecond(100)
                    contestDuration = Duration.ofSeconds(60)
                }
                val data = slot<StudentContestEntryData>()
                val saved = savedEntry(Instant.ofEpochSecond(seconds))
                every { groups.findById(group.id) } returns group
                every { contests.findById(contest.id) } returns scheduled
                every { entries.findByContext(userId = user.id, studyClassId = group.id, contestId = contest.id) } returns null
                every { clock.instant() } returns Instant.ofEpochSecond(seconds)
                every { entries.findOrCreate(capture(data)) } returns saved

                val result = operations.enterContest(user = user, classId = group.id, contestId = contest.id).getOrThrow()

                assertSame(saved, result)
                assertEquals(Instant.ofEpochSecond(seconds), data.captured.enteredAt)
            }

            @Test
            fun `should save the first entry if the contest has neither start nor end`() {
                val saved = savedEntry()
                every { groups.findById(group.id) } returns group
                every { contests.findById(contest.id) } returns contest
                every { entries.findByContext(userId = user.id, studyClassId = group.id, contestId = contest.id) } returns null
                every { clock.instant() } returns Instant.ofEpochSecond(100)
                every { entries.findOrCreate(any()) } returns saved

                val result = operations.enterContest(user = user, classId = group.id, contestId = contest.id).getOrThrow()

                assertSame(saved, result)
            }

            @Test
            fun `should save the first entry long after the start if the contest has no end`() {
                val unbounded = contest.withData { startsAt = Instant.ofEpochSecond(100) }
                val saved = savedEntry(Instant.ofEpochSecond(1_000_000))
                every { groups.findById(group.id) } returns group
                every { contests.findById(contest.id) } returns unbounded
                every { entries.findByContext(userId = user.id, studyClassId = group.id, contestId = contest.id) } returns null
                every { clock.instant() } returns Instant.ofEpochSecond(1_000_000)
                every { entries.findOrCreate(any()) } returns saved

                val result = operations.enterContest(user = user, classId = group.id, contestId = contest.id).getOrThrow()

                assertSame(saved, result)
            }

            @Test
            fun `should return the existing entry without reading the clock on a repeat entry inside the contest window`() {
                val saved = savedEntry()
                val running = contest.withData {
                    startsAt = Instant.ofEpochSecond(100)
                    contestDuration = Duration.ofSeconds(60)
                }
                every { groups.findById(group.id) } returns group
                every { contests.findById(contest.id) } returns running
                every { entries.findByContext(userId = user.id, studyClassId = group.id, contestId = contest.id) } returns saved

                val result = operations.enterContest(user = user, classId = group.id, contestId = contest.id).getOrThrow()

                assertSame(saved, result)
                verify { clock wasNot Called }
                verify(exactly = 0) { entries.findOrCreate(any()) }
            }
        }

        @Nested
        inner class RefusalTests {

            @ParameterizedTest
            @MethodSource("tech.testsys.operation.user.StudentOperationsTests#usersWithoutStudentRole")
            fun `should raise MissedStudentRoleError before reading classes if user has no Student role`(wrongRole: MultipleRoleUser) {
                assertRaises(MissedStudentRoleError) {
                    operations.enterContest(user = wrongRole, classId = group.id, contestId = contest.id)
                }

                verify { groups wasNot Called }
            }

            @Test
            fun `should raise ClassNotExistsError if the selected class does not exist`() {
                every { groups.findById(group.id) } returns null

                assertRaises(ClassNotExistsError(group.id)) {
                    operations.enterContest(user = user, classId = group.id, contestId = contest.id)
                }
            }

            @Test
            fun `should raise ContestNotExistsError if the contest does not exist`() {
                every { groups.findById(group.id) } returns group
                every { contests.findById(contest.id) } returns null

                assertRaises(ContestNotExistsError(contest.id)) {
                    operations.enterContest(user = user, classId = group.id, contestId = contest.id)
                }
            }

            @Test
            fun `should raise ClassAccessDeniedError if the student is not a member of the selected class`() {
                every { groups.findById(group.id) } returns group.withData { students = mutableListOf() }
                every { contests.findById(contest.id) } returns contest

                assertRaises(ClassAccessDeniedError(group.id)) {
                    operations.enterContest(user = user, classId = group.id, contestId = contest.id)
                }
            }

            @Test
            fun `should raise ContestAccessDeniedError if the contest is not in the selected class`() {
                every { groups.findById(group.id) } returns group.withData { this.contests = mutableListOf() }
                every { contests.findById(contest.id) } returns contest

                assertRaises(ContestAccessDeniedError(contest.id)) {
                    operations.enterContest(user = user, classId = group.id, contestId = contest.id)
                }
            }

            @Test
            fun `should raise ContestNotExistsError before ClassAccessDeniedError if a non-member enters a missing contest`() {
                every { groups.findById(group.id) } returns group.withData { students = mutableListOf() }
                every { contests.findById(contest.id) } returns null

                assertRaises(ContestNotExistsError(contest.id)) {
                    operations.enterContest(user = user, classId = group.id, contestId = contest.id)
                }
            }

            @Test
            fun `should raise ContestNotStartedError without saving if the first entry is before the start`() {
                val scheduled = contest.withData {
                    startsAt = Instant.ofEpochSecond(100)
                    contestDuration = Duration.ofSeconds(60)
                }
                every { groups.findById(group.id) } returns group
                every { contests.findById(contest.id) } returns scheduled
                every { entries.findByContext(userId = user.id, studyClassId = group.id, contestId = contest.id) } returns null
                every { clock.instant() } returns Instant.ofEpochSecond(99)

                assertRaises(ContestNotStartedError(contest.id, Instant.ofEpochSecond(100))) {
                    operations.enterContest(user = user, classId = group.id, contestId = contest.id)
                }

                verify(exactly = 0) { entries.findOrCreate(any()) }
            }

            @Test
            fun `should raise ContestEndedError without saving if the first entry is exactly at the end`() {
                val scheduled = contest.withData {
                    startsAt = Instant.ofEpochSecond(100)
                    contestDuration = Duration.ofSeconds(60)
                }
                every { groups.findById(group.id) } returns group
                every { contests.findById(contest.id) } returns scheduled
                every { entries.findByContext(userId = user.id, studyClassId = group.id, contestId = contest.id) } returns null
                every { clock.instant() } returns Instant.ofEpochSecond(160)

                assertRaises(ContestEndedError(contest.id, Instant.ofEpochSecond(160))) {
                    operations.enterContest(user = user, classId = group.id, contestId = contest.id)
                }

                verify(exactly = 0) { entries.findOrCreate(any()) }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should save the entry for the user, the selected class and the selected contest`() {
                val data = slot<StudentContestEntryData>()
                every { groups.findById(group.id) } returns group
                every { contests.findById(contest.id) } returns contest
                every { entries.findByContext(userId = user.id, studyClassId = group.id, contestId = contest.id) } returns null
                every { clock.instant() } returns Instant.ofEpochSecond(100)
                every { entries.findOrCreate(capture(data)) } returns savedEntry()

                operations.enterContest(user = user, classId = group.id, contestId = contest.id).getOrThrow()

                assertEquals(user.id, data.captured.user.id)
                assertEquals(group.id, data.captured.studyClass.id)
                assertEquals(contest.id, data.captured.contest.id)
            }

            @Test
            fun `should save a separate first entry in another class if the same contest was entered in the first class`() {
                val otherClass = classWithId(31)
                val data = slot<StudentContestEntryData>()
                val saved = savedEntry(at = Instant.ofEpochSecond(500), classId = 31)
                every { groups.findById(otherClass.id) } returns otherClass
                every { contests.findById(contest.id) } returns contest
                every { entries.findByContext(userId = user.id, studyClassId = group.id, contestId = contest.id) } returns savedEntry()
                every { entries.findByContext(userId = user.id, studyClassId = otherClass.id, contestId = contest.id) } returns null
                every { clock.instant() } returns Instant.ofEpochSecond(500)
                every { entries.findOrCreate(capture(data)) } returns saved

                val result = operations.enterContest(user = user, classId = otherClass.id, contestId = contest.id).getOrThrow()

                assertSame(saved, result)
                assertEquals(otherClass.id, data.captured.studyClass.id)
                assertEquals(Instant.ofEpochSecond(500), data.captured.enteredAt)
            }

            @Test
            fun `should return the original entry without reading the clock on a repeat entry after the contest ended`() {
                val saved = savedEntry()
                val ended = contest.withData {
                    startsAt = Instant.ofEpochSecond(1)
                    contestDuration = Duration.ofSeconds(1)
                }
                every { groups.findById(group.id) } returns group
                every { contests.findById(contest.id) } returns ended
                every { entries.findByContext(userId = user.id, studyClassId = group.id, contestId = contest.id) } returns saved

                val result = operations.enterContest(user = user, classId = group.id, contestId = contest.id).getOrThrow()

                assertSame(saved, result)
                verify { clock wasNot Called }
                verify(exactly = 0) { entries.findOrCreate(any()) }
            }

            @Test
            fun `should return an entry saved concurrently after the first lookup instead of ContestEndedError`() {
                val saved = savedEntry()
                val ended = contest.withData {
                    startsAt = Instant.ofEpochSecond(100)
                    contestDuration = Duration.ofSeconds(60)
                }
                every { groups.findById(group.id) } returns group
                every { contests.findById(contest.id) } returns ended
                every {
                    entries.findByContext(userId = user.id, studyClassId = group.id, contestId = contest.id)
                } returnsMany listOf(null, saved)
                every { clock.instant() } returns Instant.ofEpochSecond(160)

                val result = operations.enterContest(user = user, classId = group.id, contestId = contest.id).getOrThrow()

                assertSame(saved, result)
                verify(exactly = 0) { entries.findOrCreate(any()) }
            }

            @Test
            fun `should return an entry saved concurrently after the first lookup instead of ContestNotStartedError`() {
                val saved = savedEntry()
                val scheduled = contest.withData {
                    startsAt = Instant.ofEpochSecond(100)
                    contestDuration = Duration.ofSeconds(60)
                }
                every { groups.findById(group.id) } returns group
                every { contests.findById(contest.id) } returns scheduled
                every {
                    entries.findByContext(userId = user.id, studyClassId = group.id, contestId = contest.id)
                } returnsMany listOf(null, saved)
                every { clock.instant() } returns Instant.ofEpochSecond(99)

                val result = operations.enterContest(user = user, classId = group.id, contestId = contest.id).getOrThrow()

                assertSame(saved, result)
                verify(exactly = 0) { entries.findOrCreate(any()) }
            }

            @Test
            fun `should not update the contest when saving the first entry`() {
                every { groups.findById(group.id) } returns group
                every { contests.findById(contest.id) } returns contest
                every { entries.findByContext(userId = user.id, studyClassId = group.id, contestId = contest.id) } returns null
                every { clock.instant() } returns Instant.ofEpochSecond(100)
                every { entries.findOrCreate(any()) } returns savedEntry()

                operations.enterContest(user = user, classId = group.id, contestId = contest.id).getOrThrow()

                verify(exactly = 0) { contests.update(any<Contest>()) }
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should propagate an exception while persisting an entry`() {
                val failure = IllegalStateException("entry storage failed")
                every { groups.findById(group.id) } returns group
                every { contests.findById(contest.id) } returns contest
                every { entries.findByContext(userId = user.id, studyClassId = group.id, contestId = contest.id) } returns null
                every { clock.instant() } returns Instant.EPOCH
                every { entries.findOrCreate(any()) } throws failure

                val thrown = assertThrows(IllegalStateException::class.java) {
                    operations.enterContest(user = user, classId = group.id, contestId = contest.id)
                }

                assertSame(failure, thrown)
            }

            @Test
            fun `should truncate the saved entry time to microseconds`() {
                val data = slot<StudentContestEntryData>()
                every { groups.findById(group.id) } returns group
                every { contests.findById(contest.id) } returns contest
                every { entries.findByContext(userId = user.id, studyClassId = group.id, contestId = contest.id) } returns null
                every { clock.instant() } returns Instant.parse("2026-01-01T00:00:00.123456789Z")
                every { entries.findOrCreate(capture(data)) } returns savedEntry()

                operations.enterContest(user = user, classId = group.id, contestId = contest.id).getOrThrow()

                assertEquals(Instant.parse("2026-01-01T00:00:00.123456Z"), data.captured.enteredAt)
            }

            @Test
            fun `should read the current time once when saving the first entry`() {
                val scheduled = contest.withData {
                    startsAt = Instant.ofEpochSecond(100)
                    contestDuration = Duration.ofSeconds(60)
                }
                every { groups.findById(group.id) } returns group
                every { contests.findById(contest.id) } returns scheduled
                every { entries.findByContext(userId = user.id, studyClassId = group.id, contestId = contest.id) } returns null
                every { clock.instant() } returns Instant.ofEpochSecond(120)
                every { entries.findOrCreate(any()) } returns savedEntry()

                operations.enterContest(user = user, classId = group.id, contestId = contest.id).getOrThrow()

                verify(exactly = 1) { clock.instant() }
            }
        }
    }

    @Nested
    inner class ViewClassesTests {

        private val otherGroup = classWithId(31)

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should return classes of the student ordered by identifier`() {
                val student = testStudent { data = studentData { classes(listOf(31, 23)) } }
                every { groups.findByIds(listOf(ClassId(31), ClassId(23))) } returns listOf(otherGroup, group)

                val result = operations.viewClasses(user = student).getOrThrow()

                assertEquals(listOf(group, otherGroup), result)
            }

            @Test
            fun `should return an empty list if the student is not enrolled in any class`() {
                every { groups.findByIds(emptyList()) } returns emptyList()

                val result = operations.viewClasses(user = user).getOrThrow()

                assertEquals(emptyList<Class>(), result)
            }

            @Test
            fun `should exclude a class that no longer lists the student`() {
                val student = testStudent { data = studentData { classes(listOf(23, 31)) } }
                val leftGroup = otherGroup.withData { students = mutableListOf() }
                every { groups.findByIds(listOf(ClassId(23), ClassId(31))) } returns listOf(group, leftGroup)

                val result = operations.viewClasses(user = student).getOrThrow()

                assertEquals(listOf(group), result)
            }

            @Test
            fun `should include a class without contests`() {
                val student = testStudent { data = studentData { classes(listOf(23)) } }
                val emptyGroup = group.withData { this.contests = mutableListOf() }
                every { groups.findByIds(listOf(ClassId(23))) } returns listOf(emptyGroup)

                val result = operations.viewClasses(user = student).getOrThrow()

                assertEquals(listOf(emptyGroup), result)
            }
        }

        @Nested
        inner class RefusalTests {

            @ParameterizedTest
            @MethodSource("tech.testsys.operation.user.StudentOperationsTests#usersWithoutStudentRole")
            fun `should raise MissedStudentRoleError before reading classes if user has no Student role`(wrongRole: MultipleRoleUser) {
                assertRaises(MissedStudentRoleError) {
                    operations.viewClasses(user = wrongRole)
                }

                verify { groups wasNot Called }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should not update classes when viewing them`() {
                val student = testStudent { data = studentData { classes(listOf(31, 23)) } }
                every { groups.findByIds(listOf(ClassId(31), ClassId(23))) } returns listOf(otherGroup, group)

                operations.viewClasses(user = student).getOrThrow()

                verify(exactly = 0) { groups.update(any<Class>()) }
            }

            @Test
            fun `should not include classes owned in the Manager role`() {
                val studentAndManager = testMultipleRoleUser {
                    roles {
                        student { data = studentData { classes(listOf(23)) } }
                        manager { data = managerData { classes(listOf(31)) } }
                    }
                }
                every { groups.findByIds(listOf(ClassId(23))) } returns listOf(group)

                val result = operations.viewClasses(user = studentAndManager).getOrThrow()

                assertEquals(listOf(group), result)
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should propagate storage exceptions`() {
                val student = testStudent { data = studentData { classes(listOf(23)) } }
                val failure = IllegalStateException("storage failed")
                every { groups.findByIds(listOf(ClassId(23))) } throws failure

                val thrown = assertThrows(IllegalStateException::class.java) {
                    operations.viewClasses(user = student)
                }

                assertSame(failure, thrown)
            }
        }
    }

    @Nested
    inner class JoinClassTests {

        private val now = Instant.parse("2026-01-01T10:00:00Z")
        private val foreignGroup = testStudyClass { students(listOf(5)) }

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should enroll the student and return the class if the invite is valid`() {
                val enrolled = foreignGroup.withData { students.add(user.id) }
                every { invites.findByCode(InviteCodeHash("abcdefghjkmn", HashAlgorithm.Identity)) } returns testInvite()
                every { clock.instant() } returns now
                every { groups.findByInvite(ClassInviteId(31)) } returns foreignGroup
                every { groups.addStudent(foreignGroup.id, user.id) } returns enrolled

                val result = operations.joinClass(user = user, inviteCode = "abcdefghjkmn").getOrThrow()

                assertSame(enrolled, result)
                verify(exactly = 1) { groups.addStudent(foreignGroup.id, user.id) }
            }

            @Test
            fun `should look the invite up by the lowercase entered code stored with Identity`() {
                every { invites.findByCode(InviteCodeHash("abcdefghjkmn", HashAlgorithm.Identity)) } returns testInvite()
                every { clock.instant() } returns now
                every { groups.findByInvite(ClassInviteId(31)) } returns group

                val result = operations.joinClass(user = user, inviteCode = "ABCDEFGHJKMN").getOrThrow()

                assertSame(group, result)
            }

            @Test
            fun `should enroll the student if the invite expires one microsecond later`() {
                val enrolled = foreignGroup.withData { students.add(user.id) }
                every { invites.findByCode(any()) } returns testInvite(expiresAt = now.plusNanos(1_000))
                every { clock.instant() } returns now
                every { groups.findByInvite(ClassInviteId(31)) } returns foreignGroup
                every { groups.addStudent(foreignGroup.id, user.id) } returns enrolled

                val result = operations.joinClass(user = user, inviteCode = "abcdefghjkmn").getOrThrow()

                assertSame(enrolled, result)
                verify(exactly = 1) { groups.addStudent(foreignGroup.id, user.id) }
            }

            @Test
            fun `should return the class without enrolling again if the student is already enrolled`() {
                every { invites.findByCode(any()) } returns testInvite()
                every { clock.instant() } returns now
                every { groups.findByInvite(ClassInviteId(31)) } returns group

                val result = operations.joinClass(user = user, inviteCode = "abcdefghjkmn").getOrThrow()

                assertSame(group, result)
                verify(exactly = 0) { groups.addStudent(any(), any()) }
            }
        }

        @Nested
        inner class RefusalTests {

            @ParameterizedTest
            @MethodSource("tech.testsys.operation.user.StudentOperationsTests#usersWithoutStudentRole")
            fun `should raise MissedStudentRoleError before reading invites if user has no Student role`(wrongRole: MultipleRoleUser) {
                assertRaises(MissedStudentRoleError) { operations.joinClass(user = wrongRole, inviteCode = "abcdefghjkmn") }

                verify { listOf(invites, groups, clock) wasNot Called }
            }

            @Test
            fun `should raise ClassInviteCodeNotValidError with the entered code if no invite matches`() {
                every { invites.findByCode(any()) } returns null

                assertRaises(ClassInviteCodeNotValidError("abcdefghjkmn")) {
                    operations.joinClass(user = user, inviteCode = "abcdefghjkmn")
                }

                verify { listOf(groups, clock) wasNot Called }
            }

            @ParameterizedTest
            @CsvSource("' ABCDEFGHJKMN ', ' abcdefghjkmn '", "'ABCD-EFGH_JK№', 'abcd-efgh_jk№'")
            fun `should raise ClassInviteCodeNotValidError if the entered code with kept spaces or other characters matches no invite`(
                entered: String,
                lookedUp: String,
            ) {
                every { invites.findByCode(InviteCodeHash(lookedUp, HashAlgorithm.Identity)) } returns null

                assertRaises(ClassInviteCodeNotValidError(entered)) {
                    operations.joinClass(user = user, inviteCode = entered)
                }
            }

            @Test
            fun `should raise ClassInviteCodeExpiredError without enrolling if the invite expires at the current time`() {
                every { invites.findByCode(any()) } returns testInvite(expiresAt = now)
                every { clock.instant() } returns now

                assertRaises(ClassInviteCodeExpiredError("abcdefghjkmn")) { operations.joinClass(user = user, inviteCode = "abcdefghjkmn") }

                verify { groups wasNot Called }
            }

            @Test
            fun `should raise ClassInviteCodeExpiredError instead of returning the class if the student is already enrolled`() {
                every { invites.findByCode(any()) } returns testInvite(expiresAt = now.minusSeconds(1))
                every { clock.instant() } returns now
                every { groups.findByInvite(ClassInviteId(31)) } returns group

                assertRaises(ClassInviteCodeExpiredError("abcdefghjkmn")) { operations.joinClass(user = user, inviteCode = "abcdefghjkmn") }

                verify(exactly = 0) { groups.addStudent(any(), any()) }
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should propagate storage exceptions when enrolling`() {
                val failure = IllegalStateException("storage failed")
                every { invites.findByCode(any()) } returns testInvite()
                every { clock.instant() } returns now
                every { groups.findByInvite(ClassInviteId(31)) } returns foreignGroup
                every { groups.addStudent(foreignGroup.id, user.id) } throws failure

                val thrown = assertThrows(IllegalStateException::class.java) {
                    operations.joinClass(user = user, inviteCode = "abcdefghjkmn")
                }

                assertSame(failure, thrown)
            }

            @Test
            fun `should read the current time once when joining a class`() {
                every { invites.findByCode(any()) } returns testInvite()
                every { clock.instant() } returns now
                every { groups.findByInvite(ClassInviteId(31)) } returns foreignGroup
                every { groups.addStudent(foreignGroup.id, user.id) } returns foreignGroup.withData { students.add(user.id) }

                operations.joinClass(user = user, inviteCode = "abcdefghjkmn").getOrThrow()

                verify(exactly = 1) { clock.instant() }
            }
        }

        private fun testInvite(expiresAt: Instant = Instant.parse("2030-01-01T00:00:00Z")) = classInvite {
            id = 31
            createdAt = Instant.EPOCH
            data = classInviteData {
                code("abcdefghjkmn", HashAlgorithm.Identity)
                this.expiresAt = expiresAt
            }
        }
    }

    companion object {
        @JvmStatic
        fun usersWithoutStudentRole(): List<MultipleRoleUser> = listOf(
            testMultipleRoleUser {},
            testMultipleRoleUser {
                roles {
                    manager { data = managerData { classes(listOf(23)) } }
                    developer { data = developerData {} }
                }
            },
        )
    }
}
