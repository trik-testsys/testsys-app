package tech.testsys.operation.user

import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.ManagerCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.domain.model.group.RawInviteCodeDependency
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.Manager
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.CommunityInviteCodeExpiredError
import tech.testsys.operation.error.CommunityInviteCodeNotValidError
import tech.testsys.operation.error.JoinCommunityError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.operation
import tech.testsys.operation.util.normalizeInviteCode
import java.time.Clock

/**
 * Operations available to any user with non-fixed roles.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalOperationsApi::class)
class MultipleRoleUserOperations(
    private val multipleRoleUserRepository: MultipleRoleUserRepository,
    private val communityRepository: CommunityRepository,
    private val managerInviteRepository: ManagerCommunityInviteRepository,
    private val developerInviteRepository: DeveloperCommunityInviteRepository,
    private val clock: Clock,
) {

    /**
     * Makes [user] a member, in the invite role, of the community whose valid invite code matches [inviteCode]
     * case-insensitively, granting the role if needed. A member in that role is returned unchanged.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.joinCommunity")
    @RawInviteCodeDependency(reason = "Finds the invite by hashing the normalized input with the current deterministic algorithm.")
    fun joinCommunity(user: MultipleRoleUser, inviteCode: String): OperationResult<MultipleRoleUser, JoinCommunityError> =
        operation<MultipleRoleUser, JoinCommunityError> {
            val codeHash = InviteCodeHash.hashInviteCode(normalizeInviteCode(inviteCode), HashAlgorithm.Identity)
            val invite = managerInviteRepository.findByCode(codeHash) ?: developerInviteRepository.findByCode(codeHash)
            ensure(invite != null) { CommunityInviteCodeNotValidError(inviteCode) }
            ensure(clock.instant() < invite.data.expiresAt) { CommunityInviteCodeExpiredError(inviteCode) }
            val community = checkNotNull(communityRepository.findByInvite(invite.id)) {
                "No community references community invite id=${invite.id.value}"
            }
            if (community.id in memberOf(user = user, kind = invite.kind)) return user.asSuccess()
            return multipleRoleUserRepository.addCommunityMembership(userId = user.id, communityId = community.id, kind = invite.kind)
                .asSuccess()
        }

    private fun memberOf(user: MultipleRoleUser, kind: CommunityInvite.Kind): List<CommunityId> {
        val heldRole = when (kind) {
            CommunityInvite.Kind.Manager -> user.data.roles.filterIsInstance<Manager>().singleOrNull()
            CommunityInvite.Kind.Developer -> user.data.roles.filterIsInstance<Developer>().singleOrNull()
        }
        return heldRole?.memberOf?.ids.orEmpty()
    }
}
