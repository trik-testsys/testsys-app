package tech.testsys.infra.database.api.persistence.adapter.user

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import tech.testsys.domain.builder.api.registrationRequest
import tech.testsys.domain.builder.api.registrationRequestData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.RegistrationRequestRepository
import tech.testsys.domain.model.user.RegistrationRequest
import tech.testsys.domain.model.user.RegistrationRequestData
import tech.testsys.domain.model.user.RegistrationRequestId
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RegistrationRequestPersistenceAdapterTests :
    UpdatablePersistenceAdapterContractTests<RegistrationRequestData, RegistrationRequestId, RegistrationRequest>() {

    @Autowired
    override lateinit var repository: RegistrationRequestRepository

    override fun newData() = newData(fixtures.email("registration"))

    private fun newData(email: String) = registrationRequestData {
        this.email = email
        confirmationCode = "123456"
        expiresAt = Instant.parse("2030-01-01T00:00:00Z")
        attemptsLeft = 5
    }

    override fun modified(entity: RegistrationRequest) = entity.withData {
        confirmationCode = "654321"
        expiresAt = Instant.parse("2031-01-01T00:00:00Z")
        attemptsLeft = 4
    }

    override fun detached(entity: RegistrationRequest) = registrationRequest {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long) = RegistrationRequestId(value)

    override fun assertSameData(expected: RegistrationRequest, actual: RegistrationRequest) {
        assertEquals(expected.data.email, actual.data.email)
        assertEquals(expected.data.confirmationCode, actual.data.confirmationCode)
        assertSameInstant(expected.data.expiresAt, actual.data.expiresAt)
        assertEquals(expected.data.attemptsLeft, actual.data.attemptsLeft)
    }

    @Test
    fun `should keep the email if another email is passed on update`() {
        val saved = repository.save(newData())

        val updated = repository.update(saved.withData { email = fixtures.email("other") })

        assertEquals(saved.data.email, updated.data.email)
        assertEquals(saved.data.email, assertNotNull(repository.findById(saved.id)).data.email)
    }

    @Test
    fun `should find request by its email`() {
        val email = fixtures.email("registration")
        val saved = repository.save(newData(email))

        val found = repository.findByEmail(email)

        assertNotNull(found)
        assertEquals(saved.id, found.id)
        assertSameData(saved, found)
    }

    @Test
    fun `should not find request by unknown email`() {
        repository.save(newData())

        val found = repository.findByEmail(fixtures.email("unknown"))

        assertNull(found)
    }

    @Test
    fun `should not find request by email with different letter case`() {
        val email = fixtures.email("registration")
        repository.save(newData(email))

        val found = repository.findByEmail(email.uppercase())

        assertNull(found)
    }

    @Test
    fun `should reject a second request with the same email`() {
        val email = fixtures.email("registration")
        repository.save(newData(email))

        assertFailsWith<DataIntegrityViolationException> { repository.save(newData(email)) }
    }

    @Test
    fun `should not find request by email after it is removed`() {
        val saved = fixtures.registrationRequest()

        repository.remove(saved)

        assertNull(repository.findByEmail(saved.data.email))
    }
}
