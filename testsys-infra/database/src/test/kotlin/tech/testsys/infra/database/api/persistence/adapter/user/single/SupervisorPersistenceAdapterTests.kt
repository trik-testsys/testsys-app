package tech.testsys.infra.database.api.persistence.adapter.user.single

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.supervisor
import tech.testsys.domain.builder.api.supervisorData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.SupervisorRepository
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.SupervisorData
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.HashAlgorithmJpaEnum
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.SingleRoleToUserJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.SupervisorDataJpaEntityRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(InternalDatabaseApi::class)
class SupervisorPersistenceAdapterTests : UpdatablePersistenceAdapterContractTests<SupervisorData, SingleRoleUserId, Supervisor>() {

    @Autowired
    override lateinit var repository: SupervisorRepository

    @Autowired
    private lateinit var userJpaEntityRepository: UserJpaEntityRepository

    @Autowired
    private lateinit var supervisorDataJpaEntityRepository: SupervisorDataJpaEntityRepository

    @Autowired
    private lateinit var singleRoleToUserJpaEntityRepository: SingleRoleToUserJpaEntityRepository

    override fun newData() = newData(fixtures.unique("token"))

    private fun newData(rawAccessToken: String) = supervisorData {
        accessToken(rawAccessToken, algorithm = HashAlgorithm.Identity)
        name = fixtures.unique("Supervisor")
    }

    override fun modified(entity: Supervisor) = entity.withData {
        accessToken(fixtures.unique("token"), algorithm = HashAlgorithm.Identity)
        name = fixtures.unique("Renamed supervisor")
    }

    override fun detached(entity: Supervisor) = supervisor {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long) = SingleRoleUserId(value)

    override fun assertSameData(expected: Supervisor, actual: Supervisor) {
        assertEquals(expected.data.accessTokenHash, actual.data.accessTokenHash)
        assertEquals(expected.data.name, actual.data.name)
    }

    @Test
    fun `should increment the user version on an update without changes`() {
        val saved = fixtures.supervisor()

        val updated = repository.update(saved)

        assertEquals(assertNotNull(saved.version).value + 1, assertNotNull(updated.version).value)
        assertEquals(updated.version, assertNotNull(repository.findById(saved.id)).version)
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

        assertNull(supervisorDataJpaEntityRepository.findByUserId(saved.id.value))
        assertTrue(singleRoleToUserJpaEntityRepository.findAllByUserId(saved.id.value).isEmpty())
        assertTrue(userJpaEntityRepository.findById(saved.id.value).isEmpty)
    }

    @Test
    fun `should not find users of other kinds`() {
        val supervisor = repository.save(newData())
        val participantId = fixtures.participant().id
        val developerId = SingleRoleUserId(fixtures.developer().id.value)

        assertNull(repository.findById(participantId))
        assertNull(repository.findById(developerId))
        assertEquals(listOf(supervisor.id), repository.findByIds(listOf(participantId, supervisor.id, developerId)).map { it.id })
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

    @Test
    fun `should find supervisors by ids with the same statement count for one and twenty ids`() {
        val ids = List(20) { fixtures.supervisor().id }

        val (one, oneIdStatements) = withStatementCount { repository.findByIds(ids.take(1)) }
        val (twenty, twentyIdsStatements) = withStatementCount { repository.findByIds(ids) }

        assertEquals(ids.take(1), one.map { supervisor -> supervisor.id })
        assertEquals(ids.toSet(), twenty.map { supervisor -> supervisor.id }.toSet())
        assertEquals(oneIdStatements, twentyIdsStatements)
    }
}
