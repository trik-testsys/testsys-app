package tech.testsys.infra.database.api.persistence.adapter.user.single

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.support.TransactionOperations
import tech.testsys.domain.builder.api.participant
import tech.testsys.domain.builder.api.participantData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.model.user.AccessTokenHash
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.ParticipantData
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.HashAlgorithmJpaEnum
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.ParticipantDataJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.SingleRoleToUserJpaEntityRepository
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(InternalDatabaseApi::class)
class ParticipantPersistenceAdapterTests : UpdatablePersistenceAdapterContractTests<ParticipantData, SingleRoleUserId, Participant>() {

    @Autowired
    override lateinit var repository: ParticipantRepository

    @Autowired
    private lateinit var userJpaEntityRepository: UserJpaEntityRepository

    @Autowired
    private lateinit var participantDataJpaEntityRepository: ParticipantDataJpaEntityRepository

    @Autowired
    private lateinit var singleRoleToUserJpaEntityRepository: SingleRoleToUserJpaEntityRepository

    @Autowired
    private lateinit var competitionRepository: CompetitionRepository

    @Autowired
    private lateinit var transactions: TransactionOperations

    override fun newData() = newData(fixtures.unique("token"))

    private fun newData(rawAccessToken: String): ParticipantData {
        val competitionId = fixtures.competition().id.value
        return participantData {
            competition(competitionId)
            accessToken(rawAccessToken, algorithm = HashAlgorithm.Identity)
            name = fixtures.unique("Participant")
        }
    }

    override fun modified(entity: Participant): Participant = entity.withData {
        accessToken(fixtures.unique("token"), algorithm = HashAlgorithm.Identity)
        name = fixtures.unique("Renamed participant")
    }

    override fun detached(entity: Participant) = participant {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long) = SingleRoleUserId(value)

    override fun assertSameData(expected: Participant, actual: Participant) {
        assertEquals(expected.data.accessTokenHash, actual.data.accessTokenHash)
        assertEquals(expected.data.name, actual.data.name)
        assertEquals(expected.data.competition.id, actual.data.competition.id)
    }

    @Test
    fun `should store the access code and its algorithm through save and update`() {
        val data = newData()

        val saved = repository.save(data)
        val modified = modified(saved)
        val updated = repository.update(modified)
        val found = requireNotNull(repository.findById(updated.id))
        val row = userJpaEntityRepository.findById(updated.id.value).orElseThrow()

        assertEquals(data.accessTokenHash.value, saved.data.accessTokenHash.value)
        assertEquals(HashAlgorithm.Identity, saved.data.accessTokenHash.algorithm)
        assertEquals(modified.data.accessTokenHash.value, found.data.accessTokenHash.value)
        assertEquals(HashAlgorithm.Identity, found.data.accessTokenHash.algorithm)
        assertEquals(modified.data.accessTokenHash.value, row.accessToken)
        assertEquals(HashAlgorithmJpaEnum.IDENTITY, row.accessTokenHashAlgorithm)
    }

    @Test
    fun `should delete the user together with its role and data rows by id`() {
        val saved = repository.save(newData())

        repository.removeById(saved.id)

        assertNull(participantDataJpaEntityRepository.findByUserId(saved.id.value))
        assertTrue(singleRoleToUserJpaEntityRepository.findAllByUserId(saved.id.value).isEmpty())
        assertTrue(userJpaEntityRepository.findById(saved.id.value).isEmpty)
    }

    @Test
    fun `should not find users of other kinds`() {
        val participant = repository.save(newData())
        val observerId = fixtures.observer().id
        val supervisorId = fixtures.supervisor().id
        val developerId = SingleRoleUserId(fixtures.developer().id.value)

        assertNull(repository.findById(observerId))
        assertNull(repository.findById(supervisorId))
        assertNull(repository.findById(developerId))
        assertEquals(listOf(participant.id), repository.findByIds(listOf(observerId, participant.id, developerId)).map { it.id })
        repository.removeById(observerId)
        assertTrue(userJpaEntityRepository.findById(observerId.value).isPresent)
    }

    @Test
    fun `should save a participant of the competition per access code in order with the name computed from its id`() {
        val competition = fixtures.competition()
        val hashes = listOf(identityHash(fixtures.unique("first")), identityHash(fixtures.unique("second")))

        val saved = repository.saveToCompetition(competition.id, hashes) { participantId -> "named-${participantId.value}" }

        val found = saved.map { participant -> assertNotNull(repository.findById(participant.id)) }
        assertEquals(hashes, found.map { participant -> participant.data.accessTokenHash })
        assertEquals(saved.map { participant -> "named-${participant.id.value}" }, found.map { participant -> participant.data.name })
        assertEquals(saved.map { participant -> "named-${participant.id.value}" }, saved.map { participant -> participant.data.name })
        assertEquals(listOf(competition.id, competition.id), found.map { participant -> participant.data.competition.id })
        assertEquals(
            saved.map { participant -> participant.id }.toSet(),
            assertNotNull(competitionRepository.findById(competition.id)).data.participants.ids.toSet(),
        )
    }

    @Test
    fun `should save no participant of the batch if an access code is already held by another user`() {
        val existing = fixtures.participant()
        val competition = fixtures.competition()
        val usersBefore = userJpaEntityRepository.count()
        val hashes = listOf(identityHash(fixtures.unique("fresh")), existing.data.accessTokenHash)

        assertFailsWith<DataIntegrityViolationException> {
            repository.saveToCompetition(competition.id, hashes) { participantId -> "st${participantId.value}" }
        }

        assertEquals(emptyList(), participantDataJpaEntityRepository.findAllByCompetitionId(competition.id.value))
        assertEquals(usersBefore, userJpaEntityRepository.count())
    }

    @Test
    fun `should save participants of the competition with the same statement count for one and twenty access codes`() {
        val competition = fixtures.competition()
        val oneHash = listOf(identityHash(fixtures.unique("one")))
        val twentyHashes = List(20) { index -> identityHash(fixtures.unique("twenty-$index")) }

        val (one, oneCodeStatements) = withStatementCount {
            repository.saveToCompetition(competition.id, oneHash) { participantId -> "named-${participantId.value}" }
        }
        val (twenty, twentyCodesStatements) = withStatementCount {
            repository.saveToCompetition(competition.id, twentyHashes) { participantId -> "named-${participantId.value}" }
        }

        assertEquals(oneHash, one.map { participant -> participant.data.accessTokenHash })
        assertEquals(twentyHashes, twenty.map { participant -> participant.data.accessTokenHash })
        assertEquals(oneCodeStatements, twentyCodesStatements)
    }

    @Test
    fun `should return saved participants whose version allows a later update`() {
        val competition = fixtures.competition()
        val saved = repository.saveToCompetition(competition.id, listOf(identityHash(fixtures.unique("token")))) { participantId ->
            "named-${participantId.value}"
        }.single()

        val updated = repository.update(saved.withData { name = "Renamed participant" })

        assertEquals("Renamed participant", assertNotNull(repository.findById(updated.id)).data.name)
    }

    @Test
    fun `should keep the competition if another competition is passed on update`() {
        val saved = repository.save(newData())
        val otherCompetition = fixtures.competition().id

        val updated = repository.update(saved.withData { competition = otherCompetition })

        assertEquals(saved.data.competition.id, updated.data.competition.id)
        assertEquals(saved.data.competition.id, assertNotNull(repository.findById(saved.id)).data.competition.id)
        assertEquals(assertNotNull(saved.version).value + 1, assertNotNull(updated.version).value)
    }

    @Test
    fun `should increment the competition version when a participant is removed`() {
        val participant = fixtures.participant()
        val competition = assertNotNull(competitionRepository.findById(participant.data.competition.id))

        repository.removeById(participant.id)

        val stored = assertNotNull(competitionRepository.findById(competition.id))
        assertEquals(assertNotNull(competition.version).value + 1, assertNotNull(stored.version).value)
    }

    @Test
    fun `should increment the competition version when a participant is saved`() {
        val competition = fixtures.competition()

        repository.save(
            participantData {
                competition(competition.id.value)
                accessToken(fixtures.unique("token"), algorithm = HashAlgorithm.Identity)
                name = fixtures.unique("Participant")
            },
        )

        val stored = assertNotNull(competitionRepository.findById(competition.id))
        assertEquals(assertNotNull(competition.version).value + 1, assertNotNull(stored.version).value)
    }

    @Test
    fun `should not exceed the participant limit when participants are created concurrently`() {
        val competition = fixtures.competition()
        val limit = 1
        val haveRead = CountDownLatch(2)

        Executors.newFixedThreadPool(2).use { pool ->
            List(2) { index ->
                pool.submit(
                    Callable {
                        transactions.execute {
                            val count = assertNotNull(competitionRepository.findById(competition.id)).data.participants.ids.size
                            haveRead.countDown()
                            check(haveRead.await(10, TimeUnit.SECONDS))
                            if (count < limit) {
                                val hashes = listOf(identityHash(fixtures.unique("concurrent-$index")))
                                repository.saveToCompetition(competition.id, hashes) { participantId -> "named-${participantId.value}" }
                            }
                        }
                    },
                )
            }.forEach { future -> future.get() }
        }

        assertEquals(limit, participantDataJpaEntityRepository.findAllByCompetitionId(competition.id.value).size)
    }

    @Test
    fun `should save nothing for an empty list of access codes`() {
        val competition = fixtures.competition()

        val saved = repository.saveToCompetition(competition.id, emptyList()) { participantId -> "st${participantId.value}" }

        assertEquals(emptyList(), saved)
        assertEquals(emptyList(), participantDataJpaEntityRepository.findAllByCompetitionId(competition.id.value))
    }

    @Test
    fun `should find user by its access code`() {
        val token = fixtures.unique("token")
        val saved = repository.save(newData(token))

        val found = repository.findByAccessToken(token)

        assertNotNull(found)
        assertEquals(saved.id, found.id)
        assertSameData(saved, found)
    }

    @Test
    fun `should not find user of another kind by its access code`() {
        val token = fixtures.unique("token")
        fixtures.observer(rawAccessToken = token)

        val found = repository.findByAccessToken(token)

        assertNull(found)
    }

    @Test
    fun `should not find user by unknown access code`() {
        repository.save(newData())

        val found = repository.findByAccessToken(fixtures.unique("unknown"))

        assertNull(found)
    }

    @Test
    fun `should find participants by ids with the same statement count for one and twenty ids`() {
        val competition = fixtures.competition()
        val ids = List(20) { fixtures.participant(competition).id }

        val (one, oneIdStatements) = withStatementCount { repository.findByIds(ids.take(1)) }
        val (twenty, twentyIdsStatements) = withStatementCount { repository.findByIds(ids) }

        assertEquals(ids.take(1), one.map { participant -> participant.id })
        assertEquals(ids.toSet(), twenty.map { participant -> participant.id }.toSet())
        assertEquals(List(20) { competition.id }, twenty.map { participant -> participant.data.competition.id })
        assertEquals(oneIdStatements, twentyIdsStatements)
    }

    private fun identityHash(value: String) = AccessTokenHash(value = value, algorithm = HashAlgorithm.Identity)
}
