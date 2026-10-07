package tech.testsys.operation.user

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.builder.api.`class`
import tech.testsys.domain.builder.api.classInvite
import tech.testsys.domain.builder.api.classInviteData
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
import tech.testsys.operation.error.*
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.testContest
import tech.testsys.operation.util.testMultipleRoleUser
import tech.testsys.operation.util.testStudent
import tech.testsys.operation.util.testStudyClass
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

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
    private val wrongRole = testMultipleRoleUser {}
    private fun savedEntry(at: Instant = Instant.ofEpochSecond(100)) = studentContestEntry {
        id = 71
        createdAt = Instant.EPOCH
        data = studentContestEntryData {
            this.user = this@StudentOperationsTests.user.id
            studyClass = group.id
            this.contest = this@StudentOperationsTests.contest.id
            enteredAt = at
        }
    }

    @Nested
    inner class ViewContestsTests {

        @Test
        fun `should reject the missing required role before reading groups`() {
            assertRaises(MissedStudentRoleError) {
                operations.viewContests(user = wrongRole, classId = group.id)
            }

            verify { groups wasNot Called }
        }

        @Test
        fun `should reject a missing selected group`() {
            every { groups.findById(group.id) } returns null

            assertRaises(ClassNotExistsError(group.id)) {
                operations.viewContests(user = user, classId = group.id)
            }
        }

        @Test
        fun `should reject access to a class without membership`() {
            every { groups.findById(group.id) } returns group.withData { students = mutableListOf() }

            assertRaises(ClassAccessDeniedError(group.id)) {
                operations.viewContests(user = user, classId = group.id)
            }
        }

        @Test
        fun `should return null entry time alongside an unchanged unentered contest`() {
            every { groups.findById(group.id) } returns group
            every { contests.load(any<LazyEntityList<ContestId, Contest>>()) } returns listOf(contest)
            every {
                entries.findByContests(userId = user.id, studyClassId = group.id, contestIds = listOf(contest.id))
            } returns emptyList()

            val result = operations.viewContests(user = user, classId = group.id).getOrThrow()

            assertEquals(listOf(null to contest), result)
            assertSame(contest, result.single().second)
            verify(exactly = 0) { entries.findOrCreate(any()) }
            verify { clock wasNot Called }
        }

        @Test
        fun `should return the persisted first entry in the selected context`() {
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
        fun `should return an empty list if the group has no contests`() {
            every { groups.findById(group.id) } returns group.withData { this.contests = mutableListOf() }
            every { contests.load(any<LazyEntityList<ContestId, Contest>>()) } returns emptyList()
            every { entries.findByContests(userId = user.id, studyClassId = group.id, contestIds = emptyList()) } returns emptyList()

            val result = operations.viewContests(user = user, classId = group.id).getOrThrow()

            assertEquals(emptyList(), result)
        }

        @ParameterizedTest
        @ValueSource(longs = [-100, 100])
        fun `should include completed and future contests without a clock read`(start: Long) {
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
            verify { clock wasNot Called }
        }

        @Test
        fun `should propagate storage exceptions`() {
            val failure = IllegalStateException("storage failed")
            every { groups.findById(group.id) } throws failure

            val thrown = assertFailsWith<IllegalStateException> {
                operations.viewContests(user = user, classId = group.id)
            }

            assertSame(failure, thrown)
        }
    }

    @Nested
    inner class EnterContestTests {

        @Test
        fun `should reject the missing required role before reading groups`() {
            assertRaises(MissedStudentRoleError) {
                operations.enterContest(user = wrongRole, classId = group.id, contestId = contest.id)
            }

            verify { groups wasNot Called }
        }

        @Test
        fun `should reject a missing selected group`() {
            every { groups.findById(group.id) } returns null

            assertRaises(ClassNotExistsError(group.id)) {
                operations.enterContest(user = user, classId = group.id, contestId = contest.id)
            }
        }

        @Test
        fun `should reject a missing contest`() {
            every { groups.findById(group.id) } returns group
            every { contests.findById(contest.id) } returns null

            assertRaises(ContestNotExistsError(contest.id)) {
                operations.enterContest(user = user, classId = group.id, contestId = contest.id)
            }
        }

        @Test
        fun `should reject entry to a class without membership`() {
            every { groups.findById(group.id) } returns group.withData { students = mutableListOf() }
            every { contests.findById(contest.id) } returns contest

            assertRaises(ClassAccessDeniedError(group.id)) {
                operations.enterContest(user = user, classId = group.id, contestId = contest.id)
            }
        }

        @Test
        fun `should reject a contest outside the selected group`() {
            every { groups.findById(group.id) } returns group.withData { this.contests = mutableListOf() }
            every { contests.findById(contest.id) } returns contest

            assertRaises(ContestAccessDeniedError(contest.id)) {
                operations.enterContest(user = user, classId = group.id, contestId = contest.id)
            }
        }

        @Test
        fun `should reject first entry before start`() {
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
        fun `should reject first entry exactly at the end`() {
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

        @ParameterizedTest
        @ValueSource(longs = [100, 159])
        fun `should save first entry exactly at start and before the end`(seconds: Long) {
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
            assertEquals(user.id, data.captured.user.id)
            assertEquals(group.id, data.captured.studyClass.id)
            assertEquals(contest.id, data.captured.contest.id)
            verify(exactly = 1) { clock.instant() }
        }

        @Test
        fun `should preserve the previous entry even after contest expiry`() {
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
        fun `should return a concurrently saved entry before refusing an expired first entry`() {
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
        fun `should propagate an exception while persisting an entry`() {
            val failure = IllegalStateException("entry storage failed")
            every { groups.findById(group.id) } returns group
            every { contests.findById(contest.id) } returns contest
            every { entries.findByContext(userId = user.id, studyClassId = group.id, contestId = contest.id) } returns null
            every { clock.instant() } returns Instant.EPOCH
            every { entries.findOrCreate(any()) } throws failure

            val thrown = assertFailsWith<IllegalStateException> {
                operations.enterContest(user = user, classId = group.id, contestId = contest.id)
            }

            assertSame(failure, thrown)
        }

        @Test
        fun `should allow an unscheduled contest and normalize only its saved time`() {
            val data = slot<StudentContestEntryData>()
            val saved = savedEntry()
            every { groups.findById(group.id) } returns group
            every { contests.findById(contest.id) } returns contest
            every { entries.findByContext(userId = user.id, studyClassId = group.id, contestId = contest.id) } returns null
            every { clock.instant() } returns Instant.parse("2026-01-01T00:00:00.123456789Z")
            every { entries.findOrCreate(capture(data)) } returns saved

            operations.enterContest(user = user, classId = group.id, contestId = contest.id).getOrThrow()

            assertEquals(Instant.parse("2026-01-01T00:00:00.123456Z"), data.captured.enteredAt)
        }
    }

    @Nested
    inner class ViewClassesTests {

        private val otherGroup = `class` {
            id = 31
            createdAt = Instant.EPOCH
            data = group.data
        }

        @Test
        fun `should reject the missing required role before reading classes`() {
            assertRaises(MissedStudentRoleError) {
                operations.viewClasses(user = wrongRole)
            }

            verify { groups wasNot Called }
        }

        @Test
        fun `should return classes of the student ordered by identifier`() {
            val student = testStudent { data = studentData { classes(listOf(31, 23)) } }
            every { groups.findByIds(listOf(ClassId(31), ClassId(23))) } returns listOf(otherGroup, group)

            val result = operations.viewClasses(user = student).getOrThrow()

            assertEquals(listOf(ClassId(23), ClassId(31)), result.map { studyClass -> studyClass.id })
            assertSame(group, result.first())
            verify(exactly = 0) { groups.update(any<Class>()) }
        }

        @Test
        fun `should return an empty list if the student is not enrolled in any class`() {
            every { groups.findByIds(emptyList()) } returns emptyList()

            val result = operations.viewClasses(user = user).getOrThrow()

            assertEquals(emptyList(), result)
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

        @Test
        fun `should not include classes owned in the manager role`() {
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

        @Test
        fun `should propagate storage exceptions`() {
            val student = testStudent { data = studentData { classes(listOf(23)) } }
            val failure = IllegalStateException("storage failed")
            every { groups.findByIds(listOf(ClassId(23))) } throws failure

            val thrown = assertFailsWith<IllegalStateException> {
                operations.viewClasses(user = student)
            }

            assertSame(failure, thrown)
        }
    }

    @Nested
    inner class JoinClassTests {

        private val now = Instant.parse("2026-01-01T10:00:00Z")
        private val foreignGroup = testStudyClass { students(listOf(5)) }

        @Test
        fun `should raise MissedStudentRoleError before reading invites if user is not a Student`() {
            assertRaises(MissedStudentRoleError) { operations.joinClass(user = wrongRole, inviteCode = "abcdefghjkmn") }

            verify { listOf(invites, groups, clock) wasNot Called }
        }

        @Test
        fun `should raise ClassInviteCodeNotValidError with the entered code if no invite matches`() {
            every { invites.findByCode(any()) } returns null

            assertRaises(ClassInviteCodeNotValidError("abcdefghjkmn")) { operations.joinClass(user = user, inviteCode = "abcdefghjkmn") }

            verify { listOf(groups, clock) wasNot Called }
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
        fun `should keep spaces of the entered code when looking the invite up`() {
            every { invites.findByCode(InviteCodeHash(" abcdefghjkmn ", HashAlgorithm.Identity)) } returns null

            assertRaises(ClassInviteCodeNotValidError(" ABCDEFGHJKMN ")) {
                operations.joinClass(user = user, inviteCode = " ABCDEFGHJKMN ")
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
        fun `should raise ClassInviteCodeExpiredError for an already enrolled student`() {
            every { invites.findByCode(any()) } returns testInvite(expiresAt = now.minusSeconds(1))
            every { clock.instant() } returns now

            assertRaises(ClassInviteCodeExpiredError("abcdefghjkmn")) { operations.joinClass(user = user, inviteCode = "abcdefghjkmn") }
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
            verify(exactly = 1) { clock.instant() }
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

        @Test
        fun `should propagate storage exceptions when enrolling`() {
            val failure = IllegalStateException("storage failed")
            every { invites.findByCode(any()) } returns testInvite()
            every { clock.instant() } returns now
            every { groups.findByInvite(ClassInviteId(31)) } returns foreignGroup
            every { groups.addStudent(foreignGroup.id, user.id) } throws failure

            val thrown = assertFailsWith<IllegalStateException> {
                operations.joinClass(user = user, inviteCode = "abcdefghjkmn")
            }

            assertSame(failure, thrown)
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
}
