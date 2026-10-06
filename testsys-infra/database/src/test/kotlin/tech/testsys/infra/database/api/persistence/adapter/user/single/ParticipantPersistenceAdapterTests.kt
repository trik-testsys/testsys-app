package tech.testsys.infra.database.api.persistence.adapter.user.single

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.participant
import tech.testsys.domain.builder.api.participantData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
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
import kotlin.test.assertEquals
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

    override fun newData(): ParticipantData {
        val competitionId = fixtures.competition().id.value
        return participantData {
            competition(competitionId)
            accessToken(fixtures.unique("token"), algorithm = HashAlgorithm.Identity)
            name = fixtures.unique("Participant")
        }
    }

    override fun modified(entity: Participant): Participant {
        val newCompetitionId = fixtures.competition().id.value
        return entity.withData {
            competition(newCompetitionId)
            accessToken(fixtures.unique("token"), algorithm = HashAlgorithm.Identity)
            name = fixtures.unique("Renamed participant")
        }
    }

    override fun detached(entity: Participant) = participant {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long) = SingleRoleUserId(value)

    override fun assertSameData(expected: Participant, actual: Participant) {
        assertEquals(expected.data.accessToken, actual.data.accessToken)
        assertEquals(expected.data.accessTokenHashAlgorithm, actual.data.accessTokenHashAlgorithm)
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

        assertEquals(data.accessToken, saved.data.accessToken)
        assertEquals(HashAlgorithm.Identity, saved.data.accessTokenHashAlgorithm)
        assertEquals(modified.data.accessToken, found.data.accessToken)
        assertEquals(HashAlgorithm.Identity, found.data.accessTokenHashAlgorithm)
        assertEquals(modified.data.accessToken, row.accessToken)
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
}
