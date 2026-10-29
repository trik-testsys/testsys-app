package tech.testsys.infra.database.api.persistence.adapter.entry

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import tech.testsys.domain.builder.api.participantContestEntryData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantContestEntryRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.model.entry.ParticipantContestEntry
import tech.testsys.domain.model.entry.ParticipantContestEntryData
import tech.testsys.domain.model.entry.ParticipantContestEntryId
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTests
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ParticipantContestEntryPersistenceAdapterTests :
    PersistenceAdapterContractTests<ParticipantContestEntryData, ParticipantContestEntryId, ParticipantContestEntry>() {

    @Autowired
    override lateinit var repository: ParticipantContestEntryRepository

    @Autowired
    private lateinit var competitionRepository: CompetitionRepository

    @Autowired
    private lateinit var participantRepository: ParticipantRepository

    @Autowired
    private lateinit var contestRepository: ContestRepository

    override fun newData(): ParticipantContestEntryData {
        val participant = fixtures.participant()
        val contest = fixtures.contest()
        return participantContestEntryData {
            this.participant = participant.id
            competition = participant.data.competition.id
            this.contest = contest.id
            enteredAt = Instant.parse("2026-01-01T00:00:00.123456Z")
        }
    }

    override fun idOf(value: Long) = ParticipantContestEntryId(value)

    override fun assertSameData(expected: ParticipantContestEntry, actual: ParticipantContestEntry) {
        assertEquals(expected.data.participant.id, actual.data.participant.id)
        assertEquals(expected.data.competition.id, actual.data.competition.id)
        assertEquals(expected.data.contest.id, actual.data.contest.id)
        assertEquals(expected.data.enteredAt, actual.data.enteredAt)
    }

    @Test
    fun `should reject update without changing the first moment`() {
        val saved = repository.save(newData())
        val changed = saved.withData { enteredAt = Instant.ofEpochSecond(70) }

        assertFailsWith<UnsupportedOperationException> { repository.update(changed) }

        assertSameData(saved, assertNotNull(repository.findById(saved.id)))
    }

    @Test
    fun `should find only the full context and preserve microsecond precision`() {
        val saved = repository.save(newData())

        val found = assertNotNull(
            repository.findByContext(
                participantId = saved.data.participant.id,
                competitionId = saved.data.competition.id,
                contestId = saved.data.contest.id,
            ),
        )

        assertSameEntity(saved, found)
    }

    @Test
    fun `should return null for a context without an entry`() {
        val data = newData()

        val found = repository.findByContext(
            participantId = data.participant.id,
            competitionId = data.competition.id,
            contestId = data.contest.id,
        )

        assertNull(found)
    }

    @Test
    fun `should keep the first moment when creation is repeated`() {
        val first = repository.findOrCreate(newData())
        val changed = participantContestEntryData {
            participant = first.data.participant.id
            competition = first.data.competition.id
            contest = first.data.contest.id
            enteredAt = Instant.ofEpochSecond(99)
        }

        val repeated = repository.findOrCreate(changed)

        assertSameEntity(first, repeated)
    }

    @Test
    fun `should reject a second direct save of the same context`() {
        val data = newData()
        repository.save(data)

        assertFailsWith<DataIntegrityViolationException> { repository.save(data) }
    }

    @Test
    fun `should atomically create one entry for concurrent calls`() {
        val data = newData()
        val gate = CountDownLatch(2)
        val executor = Executors.newFixedThreadPool(2)

        val entries = executor.use { pool ->
            pool.invokeAll(
                listOf(
                    Callable {
                        gate.countDown()
                        check(gate.await(10, TimeUnit.SECONDS))
                        repository.findOrCreate(data)
                    },
                    Callable {
                        gate.countDown()
                        check(gate.await(10, TimeUnit.SECONDS))
                        repository.findOrCreate(data)
                    },
                ),
            ).map { result -> result.get() }
        }

        assertEquals(entries[0].id, entries[1].id)
        assertEquals(data.enteredAt, entries[0].data.enteredAt)
        assertEquals(entries[0].data.enteredAt, entries[1].data.enteredAt)
    }

    @Test
    fun `should select entries only for requested contests in the selected context`() {
        val first = repository.save(newData())
        repository.save(newData())

        val found = repository.findByContests(
            participantId = first.data.participant.id,
            competitionId = first.data.competition.id,
            contestIds = listOf(first.data.contest.id),
        )

        assertEquals(listOf(first.id), found.map { it.id })
    }

    @Test
    fun `should return an empty list for no selected contests`() {
        val data = newData()

        val found = repository.findByContests(
            participantId = data.participant.id,
            competitionId = data.competition.id,
            contestIds = emptyList(),
        )

        assertEquals(emptyList(), found)
    }

    @Test
    fun `should keep first entries independent for another participant`() {
        val first = repository.findOrCreate(newData())
        val otherId = fixtures.participant().id
        val otherData = participantContestEntryData {
            enteredAt = Instant.ofEpochSecond(30)
            participant = otherId
            competition = first.data.competition.id
            contest = first.data.contest.id
        }

        val other = repository.findOrCreate(otherData)

        assertEquals(Instant.ofEpochSecond(30), other.data.enteredAt)
        assertEquals(first.data.enteredAt, assertNotNull(repository.findById(first.id)).data.enteredAt)
        kotlin.test.assertNotEquals(first.id, other.id)
    }

    @Test
    fun `should keep first entries independent for another competition`() {
        val first = repository.findOrCreate(newData())
        val otherId = fixtures.competition().id
        val otherData = participantContestEntryData {
            enteredAt = Instant.ofEpochSecond(30)
            participant = first.data.participant.id
            competition = otherId
            contest = first.data.contest.id
        }

        val other = repository.findOrCreate(otherData)

        assertEquals(Instant.ofEpochSecond(30), other.data.enteredAt)
        assertEquals(first.data.enteredAt, assertNotNull(repository.findById(first.id)).data.enteredAt)
        kotlin.test.assertNotEquals(first.id, other.id)
    }

    @Test
    fun `should keep first entries independent for another contest`() {
        val first = repository.findOrCreate(newData())
        val otherId = fixtures.contest().id
        val otherData = participantContestEntryData {
            enteredAt = Instant.ofEpochSecond(30)
            participant = first.data.participant.id
            competition = first.data.competition.id
            contest = otherId
        }

        val other = repository.findOrCreate(otherData)

        assertEquals(Instant.ofEpochSecond(30), other.data.enteredAt)
        assertEquals(first.data.enteredAt, assertNotNull(repository.findById(first.id)).data.enteredAt)
        kotlin.test.assertNotEquals(first.id, other.id)
    }

    @Test
    fun `should cascade entry deletion when its participant is deleted`() {
        val entry = repository.save(newData())

        participantRepository.removeById(entry.data.participant.id)

        assertNull(repository.findById(entry.id))
    }

    @Test
    fun `should cascade entry deletion when its historical competition is deleted`() {
        val participant = fixtures.participant()
        val historicalCompetition = fixtures.competition()
        val data = participantContestEntryData {
            this.participant = participant.id
            competition = historicalCompetition.id
            contest = fixtures.contest().id
            enteredAt = Instant.EPOCH
        }
        val entry = repository.save(data)

        competitionRepository.removeById(historicalCompetition.id)

        assertNull(repository.findById(entry.id))
    }

    @Test
    fun `should retain entry when competition contest links are removed and restored`() {
        val participant = fixtures.participant(fixtures.competition())
        // Saving the participant incremented the competition version, so the competition is read after it.
        val competition = assertNotNull(competitionRepository.findById(participant.data.competition.id))
        val contest = fixtures.contest()
        val assigned = competitionRepository.update(competition.withData { contests(listOf(contest.id.value)) })
        val entry = fixtures.participantContestEntry(participant = participant, contest = contest)
        val removed = competitionRepository.update(assigned.withData { contests = mutableListOf() })

        competitionRepository.update(removed.withData { contests(listOf(contest.id.value)) })

        assertEquals(entry.data.enteredAt, assertNotNull(repository.findById(entry.id)).data.enteredAt)
    }

    @Test
    fun `should cascade entry deletion when its contest is deleted`() {
        val entry = repository.save(newData())

        contestRepository.removeById(entry.data.contest.id)

        assertNull(repository.findById(entry.id))
    }
}
