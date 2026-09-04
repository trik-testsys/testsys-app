package tech.testsys.infra.database.api.persistence.adapter.user.single

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.supervisorData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.SupervisorRepository
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.SupervisorData
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTest
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.SingleRoleToUserJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.SupervisorDataJpaEntityRepository
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(InternalDatabaseApi::class)
class SupervisorPersistenceAdapterTest : PersistenceAdapterContractTest<SupervisorData, SingleRoleUserId, Supervisor>() {

    @Autowired
    override lateinit var repository: SupervisorRepository

    @Autowired
    private lateinit var userJpaEntityRepository: UserJpaEntityRepository

    @Autowired
    private lateinit var supervisorDataJpaEntityRepository: SupervisorDataJpaEntityRepository

    @Autowired
    private lateinit var singleRoleToUserJpaEntityRepository: SingleRoleToUserJpaEntityRepository

    override fun newData() = supervisorData {
        accessToken = fixtures.unique("token")
        name = fixtures.unique("Supervisor")
    }

    override fun modified(entity: Supervisor) = entity.withData {
        accessToken = fixtures.unique("token")
        name = fixtures.unique("Renamed supervisor")
    }

    override fun idOf(value: Long) = SingleRoleUserId(value)

    override fun assertSameData(expected: Supervisor, actual: Supervisor) {
        assertEquals(expected.data.accessToken, actual.data.accessToken)
        assertEquals(expected.data.name, actual.data.name)
    }

    @Test
    fun `removeById deletes the user together with its role and data rows`() {
        val saved = repository.save(newData())

        repository.removeById(saved.id)

        assertNull(supervisorDataJpaEntityRepository.findByUserId(saved.id.value))
        assertTrue(singleRoleToUserJpaEntityRepository.findAllByUserId(saved.id.value).isEmpty())
        assertTrue(userJpaEntityRepository.findById(saved.id.value).isEmpty)
    }

    @Test
    fun `users of other kinds are invisible to the adapter`() {
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
