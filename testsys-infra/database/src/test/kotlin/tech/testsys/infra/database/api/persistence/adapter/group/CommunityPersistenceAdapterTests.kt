package tech.testsys.infra.database.api.persistence.adapter.group

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import tech.testsys.domain.builder.api.community
import tech.testsys.domain.builder.api.communityData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.ManagerCommunityInviteRepository
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityData
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CommunityPersistenceAdapterTests : UpdatablePersistenceAdapterContractTests<CommunityData, CommunityId, Community>() {

    @Autowired
    override lateinit var repository: CommunityRepository

    @Autowired
    private lateinit var managerInvites: ManagerCommunityInviteRepository

    @Autowired
    private lateinit var developerInvites: DeveloperCommunityInviteRepository

    override fun newData(): CommunityData {
        val ownerId = fixtures.developer().id.value
        val managerInviteId = fixtures.managerCommunityInvite().id
        val developerInviteId = fixtures.developerCommunityInvite().id
        return communityData {
            owner(ownerId)
            name = fixtures.unique("Community")
            description = "Community description"
            managerInvite = managerInviteId
            developerInvite = developerInviteId
        }
    }

    override fun modified(entity: Community) = entity.withData {
        name = fixtures.unique("Renamed community")
        description = "Updated description"
    }

    override fun detached(entity: Community) = community {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long) = CommunityId(value)

    override fun assertSameData(expected: Community, actual: Community) {
        assertEquals(expected.data.owner.id, actual.data.owner.id)
        assertEquals(expected.data.name, actual.data.name)
        assertEquals(expected.data.description, actual.data.description)
        assertEquals(expected.data.managerInvite.id, actual.data.managerInvite.id)
        assertEquals(expected.data.developerInvite.id, actual.data.developerInvite.id)
    }

    @Test
    fun `should keep the owner if another owner is passed on update`() {
        val saved = repository.save(newData())
        val otherOwnerId = fixtures.developer().id.value

        val updated = repository.update(saved.withData { owner(otherOwnerId) })

        assertEquals(saved.data.owner.id, updated.data.owner.id)
        assertEquals(saved.data.owner.id, assertNotNull(repository.findById(saved.id)).data.owner.id)
    }

    @Test
    fun `should save both invites of their roles and the community referencing them on saveWithInvites`() {
        val ownerId = fixtures.administrator().id.value

        val saved = repository.saveWithInvites(
            managerInvite = fixtures.communityInviteDataOf(code = "manager23456"),
            developerInvite = fixtures.communityInviteDataOf(code = "developer234"),
        ) { managerInviteId, developerInviteId ->
            communityData {
                owner(ownerId)
                name = fixtures.unique("Community")
                description = ""
                managerInvite = managerInviteId
                developerInvite = developerInviteId
            }
        }

        assertEquals("manager23456", assertNotNull(managerInvites.findById(saved.data.managerInvite.id)).data.codeHash.value)
        assertEquals("developer234", assertNotNull(developerInvites.findById(saved.data.developerInvite.id)).data.codeHash.value)
        val found = assertNotNull(repository.findById(saved.id))
        assertEquals(saved.data.managerInvite.id, found.data.managerInvite.id)
        assertEquals(saved.data.developerInvite.id, found.data.developerInvite.id)
    }

    @Test
    fun `should save no invites if saving the community fails on saveWithInvites`() {
        val usedInvite = fixtures.community().data.managerInvite.id
        val ownerId = fixtures.administrator().id.value

        assertFailsWith<DataIntegrityViolationException> {
            repository.saveWithInvites(
                managerInvite = fixtures.communityInviteDataOf(code = "manager23456"),
                developerInvite = fixtures.communityInviteDataOf(code = "developer234"),
            ) { _, developerInviteId ->
                communityData {
                    owner(ownerId)
                    name = fixtures.unique("Community")
                    description = ""
                    managerInvite = usedInvite
                    developerInvite = developerInviteId
                }
            }
        }

        assertNull(managerInvites.findByCode(InviteCodeHash(value = "manager23456", algorithm = HashAlgorithm.Identity)))
        assertNull(developerInvites.findByCode(InviteCodeHash(value = "developer234", algorithm = HashAlgorithm.Identity)))
    }

    @Test
    fun `should find the community by either of its invites`() {
        val community = fixtures.community()
        fixtures.community()

        assertEquals(community.id, repository.findByInvite(community.data.managerInvite.id)?.id)
        assertEquals(community.id, repository.findByInvite(community.data.developerInvite.id)?.id)
    }

    @Test
    fun `should return null if no community references the invite`() {
        val invite = fixtures.managerCommunityInvite()

        assertNull(repository.findByInvite(invite.id))
    }

    @Test
    fun `should keep the invite references and the stored invites if other invites are passed on update`() {
        val saved = repository.save(newData())
        val storedInvite = assertNotNull(managerInvites.findById(saved.data.managerInvite.id))
        val otherInvite = fixtures.managerCommunityInvite()

        val updated = repository.update(saved.withData { managerInvite = otherInvite.id })

        assertEquals(saved.data.managerInvite.id, updated.data.managerInvite.id)
        assertEquals(saved.data.managerInvite.id, assertNotNull(repository.findById(saved.id)).data.managerInvite.id)
        val reloadedInvite = assertNotNull(managerInvites.findById(saved.data.managerInvite.id))
        assertEquals(storedInvite.data.codeHash, reloadedInvite.data.codeHash)
        assertEquals(storedInvite.version, reloadedInvite.version)
    }

    @Test
    fun `should remove both invites together with the community`() {
        val community = fixtures.community()

        repository.removeById(community.id)

        assertNull(managerInvites.findById(community.data.managerInvite.id))
        assertNull(developerInvites.findById(community.data.developerInvite.id))
    }
}
