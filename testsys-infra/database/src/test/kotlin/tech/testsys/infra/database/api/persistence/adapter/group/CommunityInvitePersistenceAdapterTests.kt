package tech.testsys.infra.database.api.persistence.adapter.group

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import tech.testsys.domain.builder.api.developerCommunityInvite
import tech.testsys.domain.builder.api.managerCommunityInvite
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.DeveloperCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.ManagerCommunityInviteRepository
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.CommunityInviteData
import tech.testsys.domain.model.group.CommunityInviteId
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ManagerCommunityInvitePersistenceAdapterTests :
    UpdatablePersistenceAdapterContractTests<CommunityInviteData, CommunityInviteId, CommunityInvite.Manager>() {

    @Autowired
    override lateinit var repository: ManagerCommunityInviteRepository

    @Autowired
    private lateinit var developerInvites: DeveloperCommunityInviteRepository

    override fun newData(): CommunityInviteData = fixtures.communityInviteDataOf()

    override fun modified(entity: CommunityInvite.Manager) = entity.withData {
        code(fixtures.unique("renamed"), HashAlgorithm.Identity)
        expiresAt = Instant.parse("2031-01-01T00:00:00Z")
    }

    override fun detached(entity: CommunityInvite.Manager) = managerCommunityInvite {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long) = CommunityInviteId(value)

    override fun assertSameData(expected: CommunityInvite.Manager, actual: CommunityInvite.Manager) {
        assertEquals(expected.data.codeHash, actual.data.codeHash)
        assertSameInstant(expected.data.expiresAt, actual.data.expiresAt)
    }

    @Test
    fun `should keep the stored code value and algorithm after saving and loading`() {
        val saved = fixtures.managerCommunityInvite(code = "AbC 23 xyz")

        val found = assertNotNull(repository.findById(saved.id))

        assertEquals(InviteCodeHash(value = "AbC 23 xyz", algorithm = HashAlgorithm.Identity), found.data.codeHash)
        assertEquals(CommunityInvite.Kind.Manager, found.kind)
    }

    @Test
    fun `should find a manager invite by the stored code value and algorithm`() {
        val saved = fixtures.managerCommunityInvite(code = "abcdefghjkmn")
        fixtures.managerCommunityInvite()

        val found = repository.findByCode(InviteCodeHash(value = "abcdefghjkmn", algorithm = HashAlgorithm.Identity))

        assertEquals(saved.id, assertNotNull(found).id)
    }

    @Test
    fun `should not find a developer invite by id or code`() {
        val developer = fixtures.developerCommunityInvite(code = "abcdefghjkmn")

        assertNull(repository.findById(developer.id))
        assertEquals(emptyList(), repository.findByIds(listOf(developer.id)))
        assertNull(repository.findByCode(InviteCodeHash(value = "abcdefghjkmn", algorithm = HashAlgorithm.Identity)))
    }

    @Test
    fun `should find only expired manager invites at or before the moment in ascending id order`() {
        val boundary = Instant.parse("2030-01-01T00:00:00Z")
        // Snowflake ids grow with saving order, so the invite expiring at the boundary is saved first to get the
        // smaller id: ordering by the expiration moment instead of the id would put it second.
        val atBoundary = fixtures.managerCommunityInvite(expiresAt = boundary)
        val earlier = fixtures.managerCommunityInvite(expiresAt = boundary.minusSeconds(1))
        fixtures.managerCommunityInvite(expiresAt = boundary.plusSeconds(1))
        fixtures.developerCommunityInvite(expiresAt = boundary.minusSeconds(1))

        val expired = repository.findExpired(boundary)

        assertEquals(listOf(atBoundary.id, earlier.id), expired)
    }

    @Test
    fun `should not remove a developer invite by id`() {
        val developer = fixtures.developerCommunityInvite()

        repository.removeById(developer.id)

        assertNotNull(developerInvites.findById(developer.id))
    }

    @Test
    fun `should remove only manager invites by ids`() {
        val manager = fixtures.managerCommunityInvite()
        val developer = fixtures.developerCommunityInvite()

        repository.removeByIds(listOf(manager.id, developer.id))

        assertNull(repository.findById(manager.id))
        assertNotNull(developerInvites.findById(developer.id))
    }

    @Test
    fun `should fail to update a developer invite as a manager invite`() {
        val developer = fixtures.developerCommunityInvite()
        val asManager = managerCommunityInvite {
            id = developer.id.value
            createdAt = developer.createdAt
            version = developer.version
            data = developer.data
        }

        assertFailsWith<IllegalArgumentException> { repository.update(asManager) }

        assertEquals(CommunityInvite.Kind.Developer, assertNotNull(developerInvites.findById(developer.id)).kind)
    }

    @Test
    fun `should reject an invite with a code already used by a community invite of another role`() {
        fixtures.developerCommunityInvite(code = "abcdefghjkmn")

        assertFailsWith<DataIntegrityViolationException> { fixtures.managerCommunityInvite(code = "abcdefghjkmn") }
    }
}

class DeveloperCommunityInvitePersistenceAdapterTests :
    UpdatablePersistenceAdapterContractTests<CommunityInviteData, CommunityInviteId, CommunityInvite.Developer>() {

    @Autowired
    override lateinit var repository: DeveloperCommunityInviteRepository

    @Autowired
    private lateinit var managerInvites: ManagerCommunityInviteRepository

    override fun newData(): CommunityInviteData = fixtures.communityInviteDataOf()

    override fun modified(entity: CommunityInvite.Developer) = entity.withData {
        code(fixtures.unique("renamed"), HashAlgorithm.Identity)
        expiresAt = Instant.parse("2031-01-01T00:00:00Z")
    }

    override fun detached(entity: CommunityInvite.Developer) = developerCommunityInvite {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long) = CommunityInviteId(value)

    override fun assertSameData(expected: CommunityInvite.Developer, actual: CommunityInvite.Developer) {
        assertEquals(expected.data.codeHash, actual.data.codeHash)
        assertSameInstant(expected.data.expiresAt, actual.data.expiresAt)
    }

    @Test
    fun `should find a developer invite by the stored code value and algorithm`() {
        val saved = fixtures.developerCommunityInvite(code = "abcdefghjkmn")
        fixtures.managerCommunityInvite()

        val found = repository.findByCode(InviteCodeHash(value = "abcdefghjkmn", algorithm = HashAlgorithm.Identity))

        assertEquals(saved.id, assertNotNull(found).id)
        assertEquals(CommunityInvite.Kind.Developer, found.kind)
    }

    @Test
    fun `should not find a manager invite by id or code`() {
        val manager = fixtures.managerCommunityInvite(code = "abcdefghjkmn")

        assertNull(repository.findById(manager.id))
        assertNull(repository.findByCode(InviteCodeHash(value = "abcdefghjkmn", algorithm = HashAlgorithm.Identity)))
    }

    @Test
    fun `should find only expired developer invites`() {
        val boundary = Instant.parse("2030-01-01T00:00:00Z")
        val expired = fixtures.developerCommunityInvite(expiresAt = boundary)
        fixtures.managerCommunityInvite(expiresAt = boundary)

        assertEquals(listOf(expired.id), repository.findExpired(boundary))
    }

    @Test
    fun `should not remove a manager invite by id`() {
        val manager = fixtures.managerCommunityInvite()

        repository.removeById(manager.id)

        assertNotNull(managerInvites.findById(manager.id))
    }

    @Test
    fun `should remove only developer invites by ids`() {
        val developer = fixtures.developerCommunityInvite()
        val manager = fixtures.managerCommunityInvite()

        repository.removeByIds(listOf(developer.id, manager.id))

        assertNull(repository.findById(developer.id))
        assertNotNull(managerInvites.findById(manager.id))
    }
}
