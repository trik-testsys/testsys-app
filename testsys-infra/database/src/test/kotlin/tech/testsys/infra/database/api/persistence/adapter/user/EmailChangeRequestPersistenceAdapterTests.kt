package tech.testsys.infra.database.api.persistence.adapter.user

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import tech.testsys.domain.builder.api.emailChangeRequest
import tech.testsys.domain.builder.api.emailChangeRequestData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.EmailChangeRequestRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.user.EmailChangeRequest
import tech.testsys.domain.model.user.EmailChangeRequestData
import tech.testsys.domain.model.user.EmailChangeRequestId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EmailChangeRequestPersistenceAdapterTests :
    UpdatablePersistenceAdapterContractTests<EmailChangeRequestData, EmailChangeRequestId, EmailChangeRequest>() {

    @Autowired
    override lateinit var repository: EmailChangeRequestRepository

    @Autowired
    private lateinit var multipleRoleUserRepository: MultipleRoleUserRepository

    override fun newData() = newData(fixtures.student())

    private fun newData(user: MultipleRoleUser) = emailChangeRequestData {
        this.user = user.id
        email = fixtures.email("change")
        confirmationCode = "12345678"
        expiresAt = Instant.parse("2030-01-01T00:00:00Z")
        attemptsLeft = 5
    }

    override fun modified(entity: EmailChangeRequest) = entity.withData {
        email = fixtures.email("modified")
        confirmationCode = "87654321"
        expiresAt = Instant.parse("2031-01-01T00:00:00Z")
        attemptsLeft = 4
    }

    override fun detached(entity: EmailChangeRequest) = emailChangeRequest {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long) = EmailChangeRequestId(value)

    override fun assertSameData(expected: EmailChangeRequest, actual: EmailChangeRequest) {
        assertEquals(expected.data.user.id, actual.data.user.id)
        assertEquals(expected.data.email, actual.data.email)
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
    fun `should allow requests of different users for the same email`() {
        val email = fixtures.email("change")
        val first = fixtures.emailChangeRequest(email = email)

        val second = fixtures.emailChangeRequest(email = email)

        assertEquals(email, assertNotNull(repository.findById(first.id)).data.email)
        assertEquals(email, assertNotNull(repository.findById(second.id)).data.email)
    }

    @Test
    fun `should cascade request deletion when its user is deleted`() {
        val request = fixtures.emailChangeRequest()

        multipleRoleUserRepository.removeById(request.data.user.id)

        assertNull(repository.findById(request.id))
    }
}
