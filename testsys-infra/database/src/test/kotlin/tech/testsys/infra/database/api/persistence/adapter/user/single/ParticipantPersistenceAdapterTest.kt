package tech.testsys.infra.database.api.persistence.adapter.user.single

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.participantData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.ParticipantData
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTest
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.ParticipantDataJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.SingleRoleToUserJpaEntityRepository
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(InternalDatabaseApi::class)
class ParticipantPersistenceAdapterTest : PersistenceAdapterContractTest<ParticipantData, SingleRoleUserId, Participant>() {

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
            accessToken = fixtures.unique("token")
            name = fixtures.unique("Participant")
        }
    }

    override fun modified(entity: Participant): Participant {
        val newCompetitionId = fixtures.competition().id.value
        return entity.withData {
            competition(newCompetitionId)
            accessToken = fixtures.unique("token")
            name = fixtures.unique("Renamed participant")
        }
    }

    override fun idOf(value: Long) = SingleRoleUserId(value)

    override fun assertSameData(expected: Participant, actual: Participant) {
        assertEquals(expected.data.accessToken, actual.data.accessToken)
        assertEquals(expected.data.name, actual.data.name)
        assertEquals(expected.data.competition.id, actual.data.competition.id)
    }

    @Test
    fun `removeById deletes the user together with its role and data rows`() {
        val saved = repository.save(newData())

        repository.removeById(saved.id)

        assertNull(participantDataJpaEntityRepository.findByUserId(saved.id.value))
        assertTrue(singleRoleToUserJpaEntityRepository.findAllByUserId(saved.id.value).isEmpty())
        assertTrue(userJpaEntityRepository.findById(saved.id.value).isEmpty)
    }

    @Test
    fun `users of other kinds are invisible to the adapter`() {
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
