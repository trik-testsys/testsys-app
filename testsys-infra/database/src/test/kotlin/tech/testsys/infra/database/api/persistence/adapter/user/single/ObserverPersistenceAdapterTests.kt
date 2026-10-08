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
import tech.testsys.infra.database.internal.jpa.repository.user.single.ContestToObserverJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.ObserverDataJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.SingleRoleToUserJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.user.single.ObserverMapping
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
    private lateinit var contestToObserverJpaEntityRepository: ContestToObserverJpaEntityRepository

    override fun newData() = newData(fixtures.unique("token"))

    private fun newData(rawAccessToken: String): ObserverData {
        val communityId = fixtures.community().id.value
        val contestIds = listOf(fixtures.contest().id.value, fixtures.contest().id.value)
        return observerData {
            community(communityId)
            accessToken(rawAccessToken, algorithm = HashAlgorithm.Identity)
            name = fixtures.unique("Observer")
            contests(contestIds)
        }
    }

    override fun modified(entity: Observer): Observer {
        val newCommunityId = fixtures.community().id.value
        val keptContest = entity.data.contests.ids.first()
        val newContest = fixtures.contest().id
        return entity.withData {
            community(newCommunityId)
            accessToken(fixtures.unique("token"), algorithm = HashAlgorithm.Identity)
            name = fixtures.unique("Renamed observer")
            contests = mutableListOf(keptContest, newContest)
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
        assertEquals(expected.data.contests.ids.toSet(), actual.data.contests.ids.toSet())
    }

    @Test
    fun `should collapse duplicate contests into one join row`() {
        val communityId = fixtures.community().id.value
        val contestId = fixtures.contest().id.value

        val saved = repository.save(
            observerData {
                community(communityId)
                accessToken(fixtures.unique("token"), algorithm = HashAlgorithm.Identity)
                name = fixtures.unique("Observer")
                contests(listOf(contestId, contestId))
            },
        )

        assertEquals(listOf(contestId), saved.data.contests.ids.map { it.value })
        assertEquals(listOf(contestId), assertNotNull(repository.findById(saved.id)).data.contests.ids.map { it.value })
        assertEquals(1, contestToObserverJpaEntityRepository.findAllByObserverId(saved.id.value).size)
    }

    @Test
    fun `should map assigned contests and preserve the stored user fields and version`() {
        val saved = fixtures.observer(contests = listOf(fixtures.contest(), fixtures.contest()))
        val row = userJpaEntityRepository.findById(saved.id.value).orElseThrow()
        val data = requireNotNull(observerDataJpaEntityRepository.findByUserId(saved.id.value))

        val mapped = ObserverMapping.toDomain(row, data, saved.data.contests.ids)

        assertSameData(saved, mapped)
        assertEquals(saved.id, mapped.id)
        assertEquals(saved.createdAt, mapped.createdAt)
        assertEquals(saved.version, mapped.version)
    }

    @Test
    fun `should remove all contest associations when an update clears assignments`() {
        val saved = repository.save(newData())

        val updated = repository.update(saved.withData { contests.clear() })

        assertTrue(updated.data.contests.ids.isEmpty())
        assertTrue(requireNotNull(repository.findById(saved.id)).data.contests.ids.isEmpty())
        assertTrue(contestToObserverJpaEntityRepository.findAllByObserverId(saved.id.value).isEmpty())
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
        assertTrue(contestToObserverJpaEntityRepository.findAllByObserverId(saved.id.value).isEmpty())
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
