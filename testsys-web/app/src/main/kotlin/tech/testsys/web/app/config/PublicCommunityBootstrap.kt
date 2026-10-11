package tech.testsys.web.app.config

import org.springframework.core.env.Environment
import org.springframework.transaction.support.TransactionOperations
import tech.testsys.domain.builder.api.communityData
import tech.testsys.domain.builder.api.communityInviteData
import tech.testsys.domain.builder.api.multipleRoleUserData
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.ManagerCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.user.Administrator
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.config.CommunityInviteConfig
import tech.testsys.operation.config.PublicCommunityOwner
import tech.testsys.operation.util.generateInviteCode
import tech.testsys.operation.util.inviteExpiresAt
import tech.testsys.operation.util.nextAccessToken
import java.time.Clock
import java.util.random.RandomGenerator

private const val MAX_COMMUNITY_NAME_CODE_POINTS = 255

/**
 * Creates or verifies the configured public community before operations use its id.
 *
 * @property ownerEmail the reserved address of this bootstrap's technical owner.
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalOperationsApi::class)
class PublicCommunityBootstrap(
    private val communities: CommunityRepository,
    private val users: MultipleRoleUserRepository,
    private val managerInvites: ManagerCommunityInviteRepository,
    private val developerInvites: DeveloperCommunityInviteRepository,
    private val transactions: TransactionOperations,
    private val inviteConfig: CommunityInviteConfig,
    private val clock: Clock,
    private val random: RandomGenerator,
    private val ownerEmail: String = PublicCommunityOwner.EMAIL,
) {
    /**
     * Returns the configured id after creating or checking the public community.
     *
     * @since %CURRENT_VERSION%
     */
    fun ensure(environment: Environment): CommunityId {
        val rawId = environment.getRequiredProperty("testsys.operation.community.public-community-id", Long::class.java)
        require(rawId > 0) { "Public community id must be positive" }
        val id = CommunityId(rawId)
        val name = environment.getProperty("testsys.operation.community.public-community-name")
        return transactions.execute { ensureInTransaction(id, name) }
    }

    private fun ensureInTransaction(id: CommunityId, name: String?): CommunityId {
        val community = communities.findById(id)
        val owner = users.findByEmail(ownerEmail)
        if (community != null || owner != null) {
            check(community != null && owner != null && community.data.owner.id == owner.id) {
                "Public community id ${id.value} conflicts with the reserved technical owner"
            }
            val adminRoles = owner.data.roles.filterIsInstance<Administrator>()
            check(owner.data.roles.size == 1 && adminRoles.size == 1 && adminRoles.single().memberOf.ids == listOf(id)) {
                "Public community owner has inconsistent roles or membership"
            }
            check(communities.findByOwner(owner.id).map { it.id } == listOf(id)) {
                "Public community owner has inconsistent communities"
            }
            check(
                community.data.managerInvite.id != community.data.developerInvite.id &&
                    managerInvites.findById(community.data.managerInvite.id) != null &&
                    developerInvites.findById(community.data.developerInvite.id) != null,
            ) { "Public community invites are missing or inconsistent" }
            return id
        }

        require(!name.isNullOrBlank() && name.codePointCount(0, name.length) <= MAX_COMMUNITY_NAME_CODE_POINTS) {
            "Public community name must contain 1..255 Unicode characters"
        }
        val newOwner = users.save(
            multipleRoleUserData {
                accessToken(random.nextAccessToken(), HashAlgorithm.Identity)
                this.name = name
                email = ownerEmail
                roles { administrator {} }
            },
        )

        val expiresAt = inviteExpiresAt(clock.instant(), inviteConfig.ttl)
        fun invite() = communityInviteData {
            code(generateInviteCode(random, null), HashAlgorithm.Identity)
            this.expiresAt = expiresAt
        }
        communities.saveWithIdAndInvites(id, managerInvite = invite(), developerInvite = invite()) { managerId, developerId ->
            communityData {
                this.owner = newOwner.id
                this.name = name
                description = ""
                managerInvite = managerId
                developerInvite = developerId
            }
        }
        users.addCommunityMembership(newOwner.id, id, CommunityRole.Administrator)
        return id
    }
}
