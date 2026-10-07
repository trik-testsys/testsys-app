package tech.testsys.infra.database.api.persistence.adapter.group

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import tech.testsys.domain.builder.api.classInvite
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.ClassInviteRepository
import tech.testsys.domain.model.group.ClassInvite
import tech.testsys.domain.model.group.ClassInviteData
import tech.testsys.domain.model.group.ClassInviteId
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ClassInvitePersistenceAdapterTests : UpdatablePersistenceAdapterContractTests<ClassInviteData, ClassInviteId, ClassInvite>() {

    @Autowired
    override lateinit var repository: ClassInviteRepository

    override fun newData(): ClassInviteData = fixtures.classInviteDataOf()

    override fun modified(entity: ClassInvite) = entity.withData {
        code(fixtures.unique("renamed"), HashAlgorithm.Identity)
        expiresAt = Instant.parse("2031-01-01T00:00:00Z")
    }

    override fun detached(entity: ClassInvite) = classInvite {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long) = ClassInviteId(value)

    override fun assertSameData(expected: ClassInvite, actual: ClassInvite) {
        assertEquals(expected.data.codeHash, actual.data.codeHash)
        assertSameInstant(expected.data.expiresAt, actual.data.expiresAt)
    }

    @Test
    fun `should keep the stored code value and algorithm after saving and loading`() {
        val saved = fixtures.classInvite(code = "AbC 23 xyz")

        val found = assertNotNull(repository.findById(saved.id))

        assertEquals(InviteCodeHash(value = "AbC 23 xyz", algorithm = HashAlgorithm.Identity), found.data.codeHash)
    }

    @Test
    fun `should find the invite by the stored code value and algorithm`() {
        val saved = fixtures.classInvite(code = "abcdefghjkmn")
        fixtures.classInvite()

        val found = repository.findByCode(InviteCodeHash(value = "abcdefghjkmn", algorithm = HashAlgorithm.Identity))

        assertEquals(saved.id, assertNotNull(found).id)
    }

    @Test
    fun `should not find the invite by a code differing in case`() {
        fixtures.classInvite(code = "abcdefghjkmn")

        assertNull(repository.findByCode(InviteCodeHash(value = "ABCDEFGHJKMN", algorithm = HashAlgorithm.Identity)))
    }

    @Test
    fun `should find invites expiring at or before the moment in ascending id order`() {
        val boundary = Instant.parse("2030-01-01T00:00:00Z")
        // Snowflake ids grow with saving order, so the invite expiring at the boundary is saved first to get the
        // smaller id: ordering by the expiration moment instead of the id would put it second.
        val atBoundary = fixtures.classInvite(expiresAt = boundary)
        val earlier = fixtures.classInvite(expiresAt = boundary.minusSeconds(1))
        fixtures.classInvite(expiresAt = boundary.plusSeconds(1))

        val expired = repository.findExpired(boundary)

        assertEquals(listOf(atBoundary.id, earlier.id), expired)
    }

    @Test
    fun `should reject an invite with a code already used by another class invite`() {
        fixtures.classInvite(code = "abcdefghjkmn")

        assertFailsWith<DataIntegrityViolationException> { fixtures.classInvite(code = "abcdefghjkmn") }
    }
}
