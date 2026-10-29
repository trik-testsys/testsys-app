package tech.testsys.infra.database.api.persistence.adapter.user

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import tech.testsys.domain.builder.api.accessRestorationRequest
import tech.testsys.domain.builder.api.accessRestorationRequestData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.AccessRestorationRequestRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.user.AccessRestorationRequest
import tech.testsys.domain.model.user.AccessRestorationRequestData
import tech.testsys.domain.model.user.AccessRestorationRequestId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AccessRestorationRequestPersistenceAdapterTests :
    UpdatablePersistenceAdapterContractTests<AccessRestorationRequestData, AccessRestorationRequestId, AccessRestorationRequest>() {

    @Autowired
    override lateinit var repository: AccessRestorationRequestRepository

    @Autowired
    private lateinit var multipleRoleUserRepository: MultipleRoleUserRepository

    override fun newData() = newData(fixtures.student())

    private fun newData(user: MultipleRoleUser) = accessRestorationRequestData {
        this.user = user.id
        confirmationCode = "12345678"
        expiresAt = Instant.parse("2030-01-01T00:00:00Z")
        attemptsLeft = 5
    }

    override fun modified(entity: AccessRestorationRequest) = entity.withData {
        confirmationCode = "87654321"
        expiresAt = Instant.parse("2031-01-01T00:00:00Z")
        attemptsLeft = 4
    }

    override fun detached(entity: AccessRestorationRequest) = accessRestorationRequest {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long) = AccessRestorationRequestId(value)

    override fun assertSameData(expected: AccessRestorationRequest, actual: AccessRestorationRequest) {
        assertEquals(expected.data.user.id, actual.data.user.id)
        assertEquals(expected.data.confirmationCode, actual.data.confirmationCode)
        assertSameInstant(expected.data.expiresAt, actual.data.expiresAt)
        assertEquals(expected.data.attemptsLeft, actual.data.attemptsLeft)
    }

    @Test
    fun `should keep the user if another user is passed on update`() {
        val saved = repository.save(newData())
        val otherUser = fixtures.student()

        val updated = repository.update(saved.withData { user = otherUser.id })

        assertEquals(saved.data.user.id, updated.data.user.id)
        assertEquals(saved.data.user.id, assertNotNull(repository.findById(saved.id)).data.user.id)
    }

    @Test
    fun `should find request by its user`() {
        val user = fixtures.student()
        val saved = repository.save(newData(user))

        val found = repository.findByUser(user.id)

        assertNotNull(found)
        assertEquals(saved.id, found.id)
        assertSameData(saved, found)
    }

    @Test
    fun `should not find request of a user without one`() {
        repository.save(newData())

        val found = repository.findByUser(fixtures.student().id)

        assertNull(found)
    }

    @Test
    fun `should reject a second request of the same user`() {
        val user = fixtures.student()
        repository.save(newData(user))

        assertFailsWith<DataIntegrityViolationException> { repository.save(newData(user)) }
    }

    @Test
    fun `should cascade request deletion when its user is deleted`() {
        val request = fixtures.accessRestorationRequest()

        multipleRoleUserRepository.removeById(request.data.user.id)

        assertNull(repository.findById(request.id))
    }
}
