package tech.testsys.operation.user

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.api.communityInviteData
import tech.testsys.domain.builder.api.developerCommunityInvite
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.managerCommunityInvite
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.ManagerCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.CommunityInviteData
import tech.testsys.domain.model.group.CommunityInviteId
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.operation.error.CommunityInviteCodeExpiredError
import tech.testsys.operation.error.CommunityInviteCodeNotValidError
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.testCommunity
import tech.testsys.operation.util.testDeveloper
import tech.testsys.operation.util.testManager
import tech.testsys.operation.util.testMultipleRoleUser
import tech.testsys.operation.util.testStudent
import java.time.Clock
import java.time.Instant

class MultipleRoleUserOperationsTests {

    private val users = mockk<MultipleRoleUserRepository>()
    private val communities = mockk<CommunityRepository>()
    private val managerInvites = mockk<ManagerCommunityInviteRepository>()
    private val developerInvites = mockk<DeveloperCommunityInviteRepository>()
    private val clock = mockk<Clock>()
    private val operations = MultipleRoleUserOperations(
        multipleRoleUserRepository = users,
        communityRepository = communities,
        managerInviteRepository = managerInvites,
        developerInviteRepository = developerInvites,
        clock = clock,
    )
    private val communityId = CommunityId(41)
    private val now = Instant.parse("2026-01-01T10:00:00Z")

    @Test
    fun `should raise CommunityInviteCodeNotValidError with the entered code if no invite of either role matches`() {
        every { managerInvites.findByCode(any()) } returns null
        every { developerInvites.findByCode(any()) } returns null

        assertRaises(CommunityInviteCodeNotValidError("abcdefghjkmn")) {
            operations.joinCommunity(user = testStudent { data = studentData {} }, inviteCode = "abcdefghjkmn")
        }

        verify { listOf(users, communities, clock) wasNot Called }
    }

    @Test
    fun `should look the invite up by the lowercase entered code stored with Identity`() {
        val member = testManager {
            memberOf(listOf(41))
            data = managerData {}
        }
        every { managerInvites.findByCode(InviteCodeHash("abcdefghjkmn", HashAlgorithm.Identity)) } returns managerInvite()
        prepareCommunity(51)
        every { clock.instant() } returns now

        val actual = operations.joinCommunity(user = member, inviteCode = "AbCdEfGhJkMn").getOrThrow()

        assertSame(member, actual)
    }

    @Test
    fun `should keep spaces of the entered code when looking the invite up`() {
        every { managerInvites.findByCode(InviteCodeHash("abcdefghjkmn ", HashAlgorithm.Identity)) } returns null
        every { developerInvites.findByCode(InviteCodeHash("abcdefghjkmn ", HashAlgorithm.Identity)) } returns null

        assertRaises(CommunityInviteCodeNotValidError("abcdefghjkmn ")) {
            operations.joinCommunity(user = testMultipleRoleUser {}, inviteCode = "abcdefghjkmn ")
        }
    }

    @Test
    fun `should raise CommunityInviteCodeExpiredError without joining if the invite expires at the current time`() {
        prepareDeveloperInvite(expiresAt = now)
        every { clock.instant() } returns now

        assertRaises(CommunityInviteCodeExpiredError("abcdefghjkmn")) {
            operations.joinCommunity(user = testMultipleRoleUser {}, inviteCode = "abcdefghjkmn")
        }

        verify { listOf(users, communities) wasNot Called }
    }

    @Test
    fun `should raise CommunityInviteCodeExpiredError for a user already in the community`() {
        val member = testDeveloper {
            memberOf(listOf(41))
            data = developerData {}
        }
        prepareDeveloperInvite(expiresAt = now.minusSeconds(1))
        every { clock.instant() } returns now

        assertRaises(CommunityInviteCodeExpiredError("abcdefghjkmn")) {
            operations.joinCommunity(user = member, inviteCode = "abcdefghjkmn")
        }
    }

    @Test
    fun `should join the community referencing the manager invite without requiring other roles`() {
        val user = testMultipleRoleUser {}
        val joined = testManager {
            memberOf(listOf(41))
            data = managerData {}
        }
        every { managerInvites.findByCode(any()) } returns managerInvite(expiresAt = now.plusNanos(1_000))
        prepareCommunity(51)
        every { clock.instant() } returns now
        every { users.addCommunityMembership(user.id, communityId, CommunityInvite.Kind.Manager) } returns joined

        val actual = operations.joinCommunity(user = user, inviteCode = "abcdefghjkmn").getOrThrow()

        assertSame(joined, actual)
        verify(exactly = 1) { users.addCommunityMembership(user.id, communityId, CommunityInvite.Kind.Manager) }
        verify(exactly = 1) { clock.instant() }
    }

    @Test
    fun `should join if the user is a member of the community in another role only`() {
        val user = testManager {
            memberOf(listOf(41))
            data = managerData {}
        }
        val joined = testMultipleRoleUser {}
        prepareDeveloperInvite()
        prepareCommunity(52)
        every { clock.instant() } returns now
        every { users.addCommunityMembership(user.id, communityId, CommunityInvite.Kind.Developer) } returns joined

        val actual = operations.joinCommunity(user = user, inviteCode = "abcdefghjkmn").getOrThrow()

        assertSame(joined, actual)
    }

    @Test
    fun `should return the user unchanged if already a member in the invite role`() {
        val member = testDeveloper {
            memberOf(listOf(41))
            data = developerData {}
        }
        prepareDeveloperInvite()
        prepareCommunity(52)
        every { clock.instant() } returns now

        val actual = operations.joinCommunity(user = member, inviteCode = "abcdefghjkmn").getOrThrow()

        assertSame(member, actual)
        verify { users wasNot Called }
    }

    @Test
    fun `should propagate storage exceptions when joining`() {
        val user = testMultipleRoleUser {}
        val failure = IllegalStateException("storage failed")
        every { managerInvites.findByCode(any()) } returns managerInvite()
        prepareCommunity(51)
        every { clock.instant() } returns now
        every { users.addCommunityMembership(any(), any(), any()) } throws failure

        val actual = assertThrows(IllegalStateException::class.java) {
            operations.joinCommunity(user = user, inviteCode = "abcdefghjkmn")
        }

        assertSame(failure, actual)
    }

    private fun prepareDeveloperInvite(expiresAt: Instant = FAR_FUTURE) {
        every { managerInvites.findByCode(any()) } returns null
        every { developerInvites.findByCode(any()) } returns developerCommunityInvite {
            id = 52
            createdAt = Instant.EPOCH
            data = inviteData(expiresAt)
        }
    }

    private fun prepareCommunity(inviteId: Long) {
        every { communities.findByInvite(CommunityInviteId(inviteId)) } returns testCommunity(communityId.value)
    }

    private fun managerInvite(expiresAt: Instant = FAR_FUTURE): CommunityInvite.Manager = managerCommunityInvite {
        id = 51
        createdAt = Instant.EPOCH
        data = inviteData(expiresAt)
    }

    private fun inviteData(expiresAt: Instant): CommunityInviteData = communityInviteData {
        code("abcdefghjkmn", HashAlgorithm.Identity)
        this.expiresAt = expiresAt
    }

    private companion object {
        val FAR_FUTURE: Instant = Instant.parse("2030-01-01T00:00:00Z")
    }
}
