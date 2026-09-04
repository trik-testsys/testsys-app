package tech.testsys.infra.database.api.persistence.adapter.user.single

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.observerData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.ObserverData
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTest
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.CompetitionToObserverJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.ObserverDataJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.SingleRoleToUserJpaEntityRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(InternalDatabaseApi::class)
class ObserverPersistenceAdapterTest : PersistenceAdapterContractTest<ObserverData, SingleRoleUserId, Observer>() {

    @Autowired
    override lateinit var repository: ObserverRepository

    @Autowired
    private lateinit var userJpaEntityRepository: UserJpaEntityRepository

    @Autowired
    private lateinit var observerDataJpaEntityRepository: ObserverDataJpaEntityRepository

    @Autowired
    private lateinit var singleRoleToUserJpaEntityRepository: SingleRoleToUserJpaEntityRepository

    @Autowired
    private lateinit var competitionToObserverJpaEntityRepository: CompetitionToObserverJpaEntityRepository

    override fun newData(): ObserverData {
        val communityId = fixtures.community().id.value
        val competitionIds = listOf(fixtures.competition().id.value, fixtures.competition().id.value)
        return observerData {
            community(communityId)
            accessToken = fixtures.unique("token")
            name = fixtures.unique("Observer")
            competitions(competitionIds)
        }
    }

    override fun modified(entity: Observer): Observer {
        val newCommunityId = fixtures.community().id.value
        val keptCompetition = entity.data.competitions.ids.first()
        val newCompetition = fixtures.competition().id
        return entity.withData {
            community(newCommunityId)
            accessToken = fixtures.unique("token")
            name = fixtures.unique("Renamed observer")
            competitions = mutableListOf(keptCompetition, newCompetition)
        }
    }

    override fun idOf(value: Long) = SingleRoleUserId(value)

    override fun assertSameData(expected: Observer, actual: Observer) {
        assertEquals(expected.data.accessToken, actual.data.accessToken)
        assertEquals(expected.data.name, actual.data.name)
        assertEquals(expected.data.community.id, actual.data.community.id)
        assertEquals(expected.data.competitions.ids.toSet(), actual.data.competitions.ids.toSet())
    }

    @Test
    fun `duplicate competitions collapse into one join row`() {
        val communityId = fixtures.community().id.value
        val competitionId = fixtures.competition().id.value

        val saved = repository.save(
            observerData {
                community(communityId)
                accessToken = fixtures.unique("token")
                name = fixtures.unique("Observer")
                competitions(listOf(competitionId, competitionId))
            },
        )

        assertEquals(listOf(competitionId), saved.data.competitions.ids.map { it.value })
        assertEquals(listOf(competitionId), assertNotNull(repository.findById(saved.id)).data.competitions.ids.map { it.value })
        assertEquals(1, competitionToObserverJpaEntityRepository.findAllByObserverId(saved.id.value).size)
    }

    @Test
    fun `removeById deletes the user together with its role, data and join rows`() {
        val saved = repository.save(newData())

        repository.removeById(saved.id)

        assertNull(observerDataJpaEntityRepository.findByUserId(saved.id.value))
        assertTrue(competitionToObserverJpaEntityRepository.findAllByObserverId(saved.id.value).isEmpty())
        assertTrue(singleRoleToUserJpaEntityRepository.findAllByUserId(saved.id.value).isEmpty())
        assertTrue(userJpaEntityRepository.findById(saved.id.value).isEmpty)
    }

    @Test
    fun `users of other kinds are invisible to the adapter`() {
        val observer = repository.save(newData())
        val participantId = fixtures.participant().id
        val developerId = SingleRoleUserId(fixtures.developer().id.value)

        assertNull(repository.findById(participantId))
        assertNull(repository.findById(developerId))
        assertEquals(listOf(observer.id), repository.findByIds(listOf(participantId, observer.id, developerId)).map { it.id })
        repository.removeById(participantId)
        assertTrue(userJpaEntityRepository.findById(participantId.value).isPresent)
    }
}
