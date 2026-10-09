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
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.builder.api.participantContestEntry
import tech.testsys.domain.builder.api.participantContestEntryData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantContestEntryRepository
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.entry.ParticipantContestEntryData
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.operation.error.CompetitionNotExistsError
import tech.testsys.operation.error.ContestAccessDeniedError
import tech.testsys.operation.error.ContestEndedError
import tech.testsys.operation.error.ContestNotExistsError
import tech.testsys.operation.error.ContestNotStartedError
import tech.testsys.operation.error.MissedParticipantRoleError
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.testCompetition
import tech.testsys.operation.util.testContest
import tech.testsys.operation.util.testParticipant
import tech.testsys.operation.util.testSupervisor
import java.time.Clock
import java.time.Duration
import java.time.Instant
import tech.testsys.domain.builder.api.contest as contestEntity

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
    private val wrongRole = testSupervisor()
    private fun savedEntry(at: Instant = Instant.ofEpochSecond(100), contestId: Long = contest.id.value) = participantContestEntry {
        id = 71
        createdAt = Instant.EPOCH
        data = participantContestEntryData {
            participant = this@ParticipantOperationsTests.user.id
            competition = group.id
            this.contest = ContestId(contestId)
            enteredAt = at
        }
    }

    private fun contestWithId(contestId: Long): Contest = contestEntity {
        id = contestId
        createdAt = contest.createdAt
        data = contest.data
    }

    @Nested
    inner class ViewContestsTests {

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should return null entry time with the contest if the participant has not entered it`() {
                every { groups.findById(group.id) } returns group
                every { contests.load(any<LazyEntityList<ContestId, Contest>>()) } returns listOf(contest)
                every {
                    entries.findByContests(participantId = user.id, competitionId = group.id, contestIds = listOf(contest.id))
                } returns emptyList()

                val result = operations.viewContests(user = user).getOrThrow()

                assertEquals(listOf(null to contest), result)
            }

            @Test
            fun `should return the persisted first entry time with the entered contest`() {
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
            fun `should return an empty list if the competition has no contests`() {
                every { groups.findById(group.id) } returns group.withData { this.contests = mutableListOf() }
                every { contests.load(any<LazyEntityList<ContestId, Contest>>()) } returns emptyList()
                every {
                    entries.findByContests(participantId = user.id, competitionId = group.id, contestIds = emptyList())
                } returns emptyList()

                val result = operations.viewContests(user = user).getOrThrow()

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
                    entries.findByContests(participantId = user.id, competitionId = group.id, contestIds = listOf(contest.id))
                } returns emptyList()

                val result = operations.viewContests(user = user).getOrThrow()

                assertEquals(listOf(null to scheduled), result)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedParticipantRoleError before reading competitions if user is a Supervisor`() {
                assertRaises(MissedParticipantRoleError) {
                    operations.viewContests(user = wrongRole)
                }

                verify { groups wasNot Called }
            }

            @Test
            fun `should raise CompetitionNotExistsError if the competition of the participant does not exist`() {
                every { groups.findById(group.id) } returns null

                assertRaises(CompetitionNotExistsError(group.id)) {
                    operations.viewContests(user = user)
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
                    entries.findByContests(participantId = user.id, competitionId = group.id, contestIds = listOf(contest.id))
                } returns emptyList()

                operations.viewContests(user = user).getOrThrow()

                verify(exactly = 0) { entries.findOrCreate(any()) }
                verify { clock wasNot Called }
            }

            @Test
            fun `should not update contests or the competition when viewing contests`() {
                every { groups.findById(group.id) } returns group
                every { contests.load(any<LazyEntityList<ContestId, Contest>>()) } returns listOf(contest)
                every {
                    entries.findByContests(participantId = user.id, competitionId = group.id, contestIds = listOf(contest.id))
                } returns emptyList()

                operations.viewContests(user = user).getOrThrow()

                verify(exactly = 0) { contests.update(any<Contest>()) }
                verify(exactly = 0) { groups.update(any<Competition>()) }
            }

            @Test
            fun `should attach the entry time only to the entered contest if the competition has several contests`() {
                val entered = contestWithId(20)
                every { groups.findById(group.id) } returns group.withData { contests(listOf(19, 20)) }
                every { contests.load(any<LazyEntityList<ContestId, Contest>>()) } returns listOf(contest, entered)
                every {
                    entries.findByContests(
                        participantId = user.id,
                        competitionId = group.id,
                        contestIds = listOf(ContestId(19), ContestId(20)),
                    )
                } returns listOf(savedEntry(at = Instant.ofEpochSecond(300), contestId = 20))

                val result = operations.viewContests(user = user).getOrThrow()

                assertEquals(listOf(null to contest, Instant.ofEpochSecond(300) to entered), result)
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should propagate storage exceptions`() {
                val failure = IllegalStateException("storage failed")
                every { groups.findById(group.id) } throws failure

                val thrown = assertThrows(IllegalStateException::class.java) {
                    operations.viewContests(user = user)
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
            }

            @Test
            fun `should save the first entry if the contest has neither start nor end`() {
                val saved = savedEntry()
                every { groups.findById(group.id) } returns group
                every { contests.findById(contest.id) } returns contest
                every { entries.findByContext(participantId = user.id, competitionId = group.id, contestId = contest.id) } returns null
                every { clock.instant() } returns Instant.ofEpochSecond(100)
                every { entries.findOrCreate(any()) } returns saved

                val result = operations.enterContest(user = user, contestId = contest.id).getOrThrow()

                assertSame(saved, result)
            }

            @Test
            fun `should save the first entry long after the start if the contest has no end`() {
                val unbounded = contest.withData { startsAt = Instant.ofEpochSecond(100) }
                val saved = savedEntry(Instant.ofEpochSecond(1_000_000))
                every { groups.findById(group.id) } returns group
                every { contests.findById(contest.id) } returns unbounded
                every { entries.findByContext(participantId = user.id, competitionId = group.id, contestId = contest.id) } returns null
                every { clock.instant() } returns Instant.ofEpochSecond(1_000_000)
                every { entries.findOrCreate(any()) } returns saved

                val result = operations.enterContest(user = user, contestId = contest.id).getOrThrow()

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
                every { entries.findByContext(participantId = user.id, competitionId = group.id, contestId = contest.id) } returns saved

                val result = operations.enterContest(user = user, contestId = contest.id).getOrThrow()

                assertSame(saved, result)
                verify { clock wasNot Called }
                verify(exactly = 0) { entries.findOrCreate(any()) }
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedParticipantRoleError before reading competitions if user is a Supervisor`() {
                assertRaises(MissedParticipantRoleError) {
                    operations.enterContest(user = wrongRole, contestId = contest.id)
                }

                verify { groups wasNot Called }
            }

            @Test
            fun `should raise CompetitionNotExistsError if the competition of the participant does not exist`() {
                every { groups.findById(group.id) } returns null

                assertRaises(CompetitionNotExistsError(group.id)) {
                    operations.enterContest(user = user, contestId = contest.id)
                }
            }

            @Test
            fun `should raise ContestNotExistsError if the contest does not exist`() {
                every { groups.findById(group.id) } returns group
                every { contests.findById(contest.id) } returns null

                assertRaises(ContestNotExistsError(contest.id)) {
                    operations.enterContest(user = user, contestId = contest.id)
                }
            }

            @Test
            fun `should raise ContestAccessDeniedError if the contest is not in the competition of the participant`() {
                every { groups.findById(group.id) } returns group.withData { this.contests = mutableListOf() }
                every { contests.findById(contest.id) } returns contest

                assertRaises(ContestAccessDeniedError(contest.id)) {
                    operations.enterContest(user = user, contestId = contest.id)
                }
            }

            @Test
            fun `should raise ContestNotExistsError before ContestAccessDeniedError if a missing contest is outside the competition`() {
                every { groups.findById(group.id) } returns group.withData { this.contests = mutableListOf() }
                every { contests.findById(ContestId(77)) } returns null

                assertRaises(ContestNotExistsError(ContestId(77))) {
                    operations.enterContest(user = user, contestId = ContestId(77))
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
                every { entries.findByContext(participantId = user.id, competitionId = group.id, contestId = contest.id) } returns null
                every { clock.instant() } returns Instant.ofEpochSecond(99)

                assertRaises(ContestNotStartedError(contest.id, Instant.ofEpochSecond(100))) {
                    operations.enterContest(user = user, contestId = contest.id)
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
                every { entries.findByContext(participantId = user.id, competitionId = group.id, contestId = contest.id) } returns null
                every { clock.instant() } returns Instant.ofEpochSecond(160)

                assertRaises(ContestEndedError(contest.id, Instant.ofEpochSecond(160))) {
                    operations.enterContest(user = user, contestId = contest.id)
                }

                verify(exactly = 0) { entries.findOrCreate(any()) }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should save the entry for the participant, their competition and the selected contest`() {
                val data = slot<ParticipantContestEntryData>()
                every { groups.findById(group.id) } returns group
                every { contests.findById(contest.id) } returns contest
                every { entries.findByContext(participantId = user.id, competitionId = group.id, contestId = contest.id) } returns null
                every { clock.instant() } returns Instant.ofEpochSecond(100)
                every { entries.findOrCreate(capture(data)) } returns savedEntry()

                operations.enterContest(user = user, contestId = contest.id).getOrThrow()

                assertEquals(user.id, data.captured.participant.id)
                assertEquals(group.id, data.captured.competition.id)
                assertEquals(contest.id, data.captured.contest.id)
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
                every { entries.findByContext(participantId = user.id, competitionId = group.id, contestId = contest.id) } returns saved

                val result = operations.enterContest(user = user, contestId = contest.id).getOrThrow()

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
                    entries.findByContext(participantId = user.id, competitionId = group.id, contestId = contest.id)
                } returnsMany listOf(null, saved)
                every { clock.instant() } returns Instant.ofEpochSecond(160)

                val result = operations.enterContest(user = user, contestId = contest.id).getOrThrow()

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
                    entries.findByContext(participantId = user.id, competitionId = group.id, contestId = contest.id)
                } returnsMany listOf(null, saved)
                every { clock.instant() } returns Instant.ofEpochSecond(99)

                val result = operations.enterContest(user = user, contestId = contest.id).getOrThrow()

                assertSame(saved, result)
                verify(exactly = 0) { entries.findOrCreate(any()) }
            }

            @Test
            fun `should not update the contest when saving the first entry`() {
                every { groups.findById(group.id) } returns group
                every { contests.findById(contest.id) } returns contest
                every { entries.findByContext(participantId = user.id, competitionId = group.id, contestId = contest.id) } returns null
                every { clock.instant() } returns Instant.ofEpochSecond(100)
                every { entries.findOrCreate(any()) } returns savedEntry()

                operations.enterContest(user = user, contestId = contest.id).getOrThrow()

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
                every { entries.findByContext(participantId = user.id, competitionId = group.id, contestId = contest.id) } returns null
                every { clock.instant() } returns Instant.EPOCH
                every { entries.findOrCreate(any()) } throws failure

                val thrown = assertThrows(IllegalStateException::class.java) {
                    operations.enterContest(user = user, contestId = contest.id)
                }

                assertSame(failure, thrown)
            }

            @Test
            fun `should truncate the saved entry time to microseconds`() {
                val data = slot<ParticipantContestEntryData>()
                every { groups.findById(group.id) } returns group
                every { contests.findById(contest.id) } returns contest
                every { entries.findByContext(participantId = user.id, competitionId = group.id, contestId = contest.id) } returns null
                every { clock.instant() } returns Instant.parse("2026-01-01T00:00:00.123456789Z")
                every { entries.findOrCreate(capture(data)) } returns savedEntry()

                operations.enterContest(user = user, contestId = contest.id).getOrThrow()

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
                every { entries.findByContext(participantId = user.id, competitionId = group.id, contestId = contest.id) } returns null
                every { clock.instant() } returns Instant.ofEpochSecond(120)
                every { entries.findOrCreate(any()) } returns savedEntry()

                operations.enterContest(user = user, contestId = contest.id).getOrThrow()

                verify(exactly = 1) { clock.instant() }
            }
        }
    }
}
