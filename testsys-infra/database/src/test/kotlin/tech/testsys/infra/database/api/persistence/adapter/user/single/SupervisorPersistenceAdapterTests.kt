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

    override fun newData() = supervisorData {
        accessToken(fixtures.unique("token"), algorithm = HashAlgorithm.Identity)
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
        assertEquals(expected.data.accessToken, actual.data.accessToken)
        assertEquals(expected.data.accessTokenHashAlgorithm, actual.data.accessTokenHashAlgorithm)
        assertEquals(expected.data.name, actual.data.name)
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
}
