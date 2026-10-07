package tech.testsys.infra.database.api.persistence.adapter.user.single

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.observer
import tech.testsys.domain.builder.api.observerData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.ObserverData
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.HashAlgorithmJpaEnum
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.CompetitionToObserverJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.ObserverDataJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.SingleRoleToUserJpaEntityRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(InternalDatabaseApi::class)
class ObserverPersistenceAdapterTests : UpdatablePersistenceAdapterContractTests<ObserverData, SingleRoleUserId, Observer>() {

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

    override fun newData() = newData(fixtures.unique("token"))

    private fun newData(rawAccessToken: String): ObserverData {
        val communityId = fixtures.community().id.value
        val competitionIds = listOf(fixtures.competition().id.value, fixtures.competition().id.value)
        return observerData {
            community(communityId)
            accessToken(rawAccessToken, algorithm = HashAlgorithm.Identity)
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
            accessToken(fixtures.unique("token"), algorithm = HashAlgorithm.Identity)
            name = fixtures.unique("Renamed observer")
            competitions = mutableListOf(keptCompetition, newCompetition)
        }
    }

    override fun detached(entity: Observer) = observer {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long) = SingleRoleUserId(value)

    override fun assertSameData(expected: Observer, actual: Observer) {
        assertEquals(expected.data.accessTokenHash, actual.data.accessTokenHash)
        assertEquals(expected.data.name, actual.data.name)
        assertEquals(expected.data.community.id, actual.data.community.id)
        assertEquals(expected.data.competitions.ids.toSet(), actual.data.competitions.ids.toSet())
    }

    @Test
    fun `should collapse duplicate competitions into one join row`() {
        val communityId = fixtures.community().id.value
        val competitionId = fixtures.competition().id.value

        val saved = repository.save(
            observerData {
                community(communityId)
                accessToken(fixtures.unique("token"), algorithm = HashAlgorithm.Identity)
                name = fixtures.unique("Observer")
                competitions(listOf(competitionId, competitionId))
            },
        )

        assertEquals(listOf(competitionId), saved.data.competitions.ids.map { it.value })
        assertEquals(listOf(competitionId), assertNotNull(repository.findById(saved.id)).data.competitions.ids.map { it.value })
        assertEquals(1, competitionToObserverJpaEntityRepository.findAllByObserverId(saved.id.value).size)
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
    fun `should delete the user together with its role, data and join rows by id`() {
        val saved = repository.save(newData())

        repository.removeById(saved.id)

        assertNull(observerDataJpaEntityRepository.findByUserId(saved.id.value))
        assertTrue(competitionToObserverJpaEntityRepository.findAllByObserverId(saved.id.value).isEmpty())
        assertTrue(singleRoleToUserJpaEntityRepository.findAllByUserId(saved.id.value).isEmpty())
        assertTrue(userJpaEntityRepository.findById(saved.id.value).isEmpty)
    }

    @Test
    fun `should not find users of other kinds`() {
        val observer = repository.save(newData())
        val participantId = fixtures.participant().id
        val developerId = SingleRoleUserId(fixtures.developer().id.value)

        assertNull(repository.findById(participantId))
        assertNull(repository.findById(developerId))
        assertEquals(listOf(observer.id), repository.findByIds(listOf(participantId, observer.id, developerId)).map { it.id })
        repository.removeById(participantId)
        assertTrue(userJpaEntityRepository.findById(participantId.value).isPresent)
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
        fixtures.participant(rawAccessToken = token)

        val found = repository.findByAccessToken(token)

        assertNull(found)
    }

    @Test
    fun `should not find user by unknown access code`() {
        repository.save(newData())

        val found = repository.findByAccessToken(fixtures.unique("unknown"))

        assertNull(found)
    }
}
