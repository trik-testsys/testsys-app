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
import tech.testsys.domain.builder.api.participantContestEntry
import tech.testsys.domain.builder.api.participantContestEntryData
import tech.testsys.domain.builder.api.supervisor
import tech.testsys.domain.builder.api.supervisorData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantContestEntryRepository
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.entry.ParticipantContestEntryData
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.operation.error.*
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.testCompetition
import tech.testsys.operation.util.testContest
import tech.testsys.operation.util.testParticipant
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ParticipantOperationsTests {

    private val groups = mockk<CompetitionRepository>()
    private val contests = mockk<ContestRepository>()
    private val entries = mockk<ParticipantContestEntryRepository>()
    private val clock = mockk<Clock>()
    private val operations = ParticipantOperations(
        competitionRepository = groups,
        contestRepository = contests,
        contestEntryRepository = entries,
        clock = clock,
    )
    private val user = testParticipant()
    private val group = testCompetition {
        contests(listOf(19))
    }
    private val contest = testContest()
    private val wrongRole = supervisor {
        id = 18
        createdAt = Instant.EPOCH
        data = supervisorData {
            accessToken("supervisor", algorithm = HashAlgorithm.Identity)
            name = "Supervisor"
        }
    }
    private fun savedEntry(at: Instant = Instant.ofEpochSecond(100)) = participantContestEntry {
        id = 71
        createdAt = Instant.EPOCH
        data = participantContestEntryData {
            participant = this@ParticipantOperationsTests.user.id
            competition = group.id
            this.contest = this@ParticipantOperationsTests.contest.id
            enteredAt = at
        }
    }

    @Nested
    inner class ViewContestsTests {

        @Test
        fun `should reject the missing required role before reading groups`() {
            assertRaises(MissedParticipantRoleError) {
                operations.viewContests(user = wrongRole)
            }

            verify { groups wasNot Called }
        }

        @Test
        fun `should reject a missing selected group`() {
            every { groups.findById(group.id) } returns null

            assertRaises(CompetitionNotExistsError(group.id)) {
                operations.viewContests(user = user)
            }
        }

        @Test
        fun `should return null entry time alongside an unchanged unentered contest`() {
            every { groups.findById(group.id) } returns group
            every { contests.load(any<LazyEntityList<ContestId, Contest>>()) } returns listOf(contest)
            every {
                entries.findByContests(participantId = user.id, competitionId = group.id, contestIds = listOf(contest.id))
            } returns emptyList()

            val result = operations.viewContests(user = user).getOrThrow()

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
                entries.findByContests(participantId = user.id, competitionId = group.id, contestIds = listOf(contest.id))
            } returns listOf(entry)

            val result = operations.viewContests(user = user).getOrThrow()

            assertEquals(listOf(Instant.ofEpochSecond(100) to contest), result)
        }

        @Test
        fun `should return an empty list if the group has no contests`() {
            every { groups.findById(group.id) } returns group.withData { this.contests = mutableListOf() }
            every { contests.load(any<LazyEntityList<ContestId, Contest>>()) } returns emptyList()
            every {
                entries.findByContests(participantId = user.id, competitionId = group.id, contestIds = emptyList())
            } returns emptyList()

            val result = operations.viewContests(user = user).getOrThrow()

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
                entries.findByContests(participantId = user.id, competitionId = group.id, contestIds = listOf(contest.id))
            } returns emptyList()

            val result = operations.viewContests(user = user).getOrThrow()

            assertEquals(listOf(null to scheduled), result)
            verify { clock wasNot Called }
        }

        @Test
        fun `should propagate storage exceptions`() {
            val failure = IllegalStateException("storage failed")
            every { groups.findById(group.id) } throws failure

            val thrown = assertFailsWith<IllegalStateException> {
                operations.viewContests(user = user)
            }

            assertSame(failure, thrown)
        }
    }

    @Nested
    inner class EnterContestTests {

        @Test
        fun `should reject the missing required role before reading groups`() {
            assertRaises(MissedParticipantRoleError) {
                operations.enterContest(user = wrongRole, contestId = contest.id)
            }

            verify { groups wasNot Called }
        }

        @Test
        fun `should reject a missing selected group`() {
            every { groups.findById(group.id) } returns null

            assertRaises(CompetitionNotExistsError(group.id)) {
                operations.enterContest(user = user, contestId = contest.id)
            }
        }

        @Test
        fun `should reject a missing contest`() {
            every { groups.findById(group.id) } returns group
            every { contests.findById(contest.id) } returns null

            assertRaises(ContestNotExistsError(contest.id)) {
                operations.enterContest(user = user, contestId = contest.id)
            }
        }

        @Test
        fun `should reject a contest outside the selected group`() {
            every { groups.findById(group.id) } returns group.withData { this.contests = mutableListOf() }
            every { contests.findById(contest.id) } returns contest

            assertRaises(ContestAccessDeniedError(contest.id)) {
                operations.enterContest(user = user, contestId = contest.id)
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
            every { entries.findByContext(participantId = user.id, competitionId = group.id, contestId = contest.id) } returns null
            every { clock.instant() } returns Instant.ofEpochSecond(99)

            assertRaises(ContestNotStartedError(contest.id, Instant.ofEpochSecond(100))) {
                operations.enterContest(user = user, contestId = contest.id)
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
            every { entries.findByContext(participantId = user.id, competitionId = group.id, contestId = contest.id) } returns null
            every { clock.instant() } returns Instant.ofEpochSecond(160)

            assertRaises(ContestEndedError(contest.id, Instant.ofEpochSecond(160))) {
                operations.enterContest(user = user, contestId = contest.id)
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
            val data = slot<ParticipantContestEntryData>()
            val saved = savedEntry(Instant.ofEpochSecond(seconds))
            every { groups.findById(group.id) } returns group
            every { contests.findById(contest.id) } returns scheduled
            every { entries.findByContext(participantId = user.id, competitionId = group.id, contestId = contest.id) } returns null
            every { clock.instant() } returns Instant.ofEpochSecond(seconds)
            every { entries.findOrCreate(capture(data)) } returns saved

            val result = operations.enterContest(user = user, contestId = contest.id).getOrThrow()

            assertSame(saved, result)
            assertEquals(Instant.ofEpochSecond(seconds), data.captured.enteredAt)
            assertEquals(user.id, data.captured.participant.id)
            assertEquals(group.id, data.captured.competition.id)
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
            every { entries.findByContext(participantId = user.id, competitionId = group.id, contestId = contest.id) } returns saved

            val result = operations.enterContest(user = user, contestId = contest.id).getOrThrow()

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
                entries.findByContext(participantId = user.id, competitionId = group.id, contestId = contest.id)
            } returnsMany listOf(null, saved)
            every { clock.instant() } returns Instant.ofEpochSecond(160)

            val result = operations.enterContest(user = user, contestId = contest.id).getOrThrow()

            assertSame(saved, result)
            verify(exactly = 0) { entries.findOrCreate(any()) }
        }

        @Test
        fun `should propagate an exception while persisting an entry`() {
            val failure = IllegalStateException("entry storage failed")
            every { groups.findById(group.id) } returns group
            every { contests.findById(contest.id) } returns contest
            every { entries.findByContext(participantId = user.id, competitionId = group.id, contestId = contest.id) } returns null
            every { clock.instant() } returns Instant.EPOCH
            every { entries.findOrCreate(any()) } throws failure

            val thrown = assertFailsWith<IllegalStateException> {
                operations.enterContest(user = user, contestId = contest.id)
            }

            assertSame(failure, thrown)
        }

        @Test
        fun `should allow an unscheduled contest and normalize only its saved time`() {
            val data = slot<ParticipantContestEntryData>()
            val saved = savedEntry()
            every { groups.findById(group.id) } returns group
            every { contests.findById(contest.id) } returns contest
            every { entries.findByContext(participantId = user.id, competitionId = group.id, contestId = contest.id) } returns null
            every { clock.instant() } returns Instant.parse("2026-01-01T00:00:00.123456789Z")
            every { entries.findOrCreate(capture(data)) } returns saved

            operations.enterContest(user = user, contestId = contest.id).getOrThrow()

            assertEquals(Instant.parse("2026-01-01T00:00:00.123456Z"), data.captured.enteredAt)
        }
    }
}
