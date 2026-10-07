package tech.testsys.operation.user

import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.builder.group.CommunityInviteDataBuilder
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.UserFilter
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.ManagerCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.RawInviteCodeDependency
import tech.testsys.domain.model.user.Administrator
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.RawAccessTokenDependency
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.config.CommunityInviteConfig
import tech.testsys.operation.error.CommunityAccessDeniedError
import tech.testsys.operation.error.CommunityNotExistsError
import tech.testsys.operation.error.CreateCommunityInviteError
import tech.testsys.operation.error.ExtendCommunityInviteError
import tech.testsys.operation.error.MissedAdministratorRoleError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.RefreshCommunityInviteError
import tech.testsys.operation.error.UserAccessDeniedError
import tech.testsys.operation.error.UserNotExistsError
import tech.testsys.operation.error.ViewCommunityInvitesError
import tech.testsys.operation.error.ViewUserError
import tech.testsys.operation.error.ViewUsersError
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.operation
import tech.testsys.operation.error.raise
import tech.testsys.operation.util.generateInviteCode
import tech.testsys.operation.util.hasRole
import tech.testsys.operation.util.inviteExpiresAt
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant

/**
 * Operations of a user with the [Administrator] role.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalOperationsApi::class)
class AdministratorOperations(
    private val communityRepository: CommunityRepository,
    private val managerInviteRepository: ManagerCommunityInviteRepository,
    private val developerInviteRepository: DeveloperCommunityInviteRepository,
    private val communityInviteConfig: CommunityInviteConfig,
    private val clock: Clock,
    private val userRepository: UserRepository,
) {

    private val random = SecureRandom()

    /**
     * Returns a [pagination] page matching [filter] of users of communities created by [user], preserving stored state.
     * Missing administrator role is an expected failure; storage exceptions propagate to the caller.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.viewUsers")
    @RawAccessTokenDependency(
        reason = "Returned users carry stored access codes that the administrator sees as the original codes only with Identity.",
    )
    fun viewUsers(
        user: MultipleRoleUser,
        pagination: Pagination,
        filter: UserFilter = UserFilter(),
    ): OperationResult<Page<User<*>>, ViewUsersError> = operation<Page<User<*>>, ViewUsersError> {
        ensure(user.hasRole<Administrator>(), MissedAdministratorRoleError)
        val users = userRepository.findAvailableToAdministrator(administratorId = user.id, pagination = pagination, filter = filter)
        return users.asSuccess()
    }

    /**
     * Returns [userId] with all its stored data if it is available to [user], preserving stored state.
     * Missing role, user and access are expected failures; storage exceptions propagate to the caller.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.viewUser")
    @RawAccessTokenDependency(
        reason = "The returned user carries the stored access code that the administrator sees as the original code only with Identity.",
    )
    fun viewUser(user: MultipleRoleUser, userId: UserId): OperationResult<User<*>, ViewUserError> = operation<User<*>, ViewUserError> {
        ensure(user.hasRole<Administrator>(), MissedAdministratorRoleError)
        val viewed = userRepository.findAvailableToAdministratorById(administratorId = user.id, userId = userId)
        if (viewed == null) {
            ensure(userRepository.existsById(userId)) { UserNotExistsError(userId) }
            UserAccessDeniedError(userId).raise()
        }
        return viewed.asSuccess()
    }

    /**
     * Replaces the invite code for [kind] in [communityId] owned by [user] with a new code and a fresh expiration moment.
     * Missing role, community and access are expected failures; storage exceptions, including a code collision, propagate.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.inviteUser")
    @RawInviteCodeDependency(reason = "Returns the issued invite code from the stored value.")
    fun createCommunityInvite(
        user: MultipleRoleUser,
        communityId: CommunityId,
        kind: CommunityInvite.Kind,
    ): OperationResult<CommunityInvite, CreateCommunityInviteError> = operation<CommunityInvite, CreateCommunityInviteError> {
        ensure(user.hasRole<Administrator>(), MissedAdministratorRoleError)
        val community = communityRepository.findById(communityId)
        ensure(community != null) { CommunityNotExistsError(communityId) }
        ensure(community.data.owner.id == user.id) { CommunityAccessDeniedError(communityId) }
        val expiresAt = inviteExpiresAt(now = clock.instant(), ttl = communityInviteConfig.ttl)
        return replaceCode(invite = loadInvite(community, kind), expiresAt = expiresAt).asSuccess()
    }

    /**
     * Restarts the validity period of the invite code for [kind] in [communityId] owned by [user] without changing
     * the code, including an expired code. Missing role, community and access are expected failures.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.extendInvite")
    @RawInviteCodeDependency(reason = "Returns the issued invite code from the stored value.")
    fun extendCommunityInvite(
        user: MultipleRoleUser,
        communityId: CommunityId,
        kind: CommunityInvite.Kind,
    ): OperationResult<CommunityInvite, ExtendCommunityInviteError> = operation<CommunityInvite, ExtendCommunityInviteError> {
        ensure(user.hasRole<Administrator>(), MissedAdministratorRoleError)
        val community = communityRepository.findById(communityId)
        ensure(community != null) { CommunityNotExistsError(communityId) }
        ensure(community.data.owner.id == user.id) { CommunityAccessDeniedError(communityId) }
        val existing = loadInvite(community, kind)
        val expiresAt = inviteExpiresAt(now = clock.instant(), ttl = communityInviteConfig.ttl)
        return update(existing) { this.expiresAt = expiresAt }.asSuccess()
    }

    /**
     * Returns both invite codes of [communityId] owned by [user] as issued, the manager code before the developer one.
     * Expired codes are returned unchanged; missing role, community and access are expected failures; nothing is written.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.viewInvites")
    @RawInviteCodeDependency(reason = "Returns the issued invite codes from the stored values.")
    fun viewCommunityInvites(
        user: MultipleRoleUser,
        communityId: CommunityId,
    ): OperationResult<List<CommunityInvite>, ViewCommunityInvitesError> = operation<List<CommunityInvite>, ViewCommunityInvitesError> {
        ensure(user.hasRole<Administrator>(), MissedAdministratorRoleError)
        val community = communityRepository.findById(communityId)
        ensure(community != null) { CommunityNotExistsError(communityId) }
        ensure(community.data.owner.id == user.id) { CommunityAccessDeniedError(communityId) }
        val manager = community.data.managerInvite.load(managerInviteRepository)
        val developer = community.data.developerInvite.load(developerInviteRepository)
        return listOf(manager, developer).asSuccess()
    }

    /**
     * Replaces the expired invite code for [kind] in [communityId] owned by [user] with a new code and a fresh expiration
     * moment; a still valid code is returned unchanged. Missing role, community and access are expected failures.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.inviteUser")
    fun refreshCommunityInvite(
        user: MultipleRoleUser,
        communityId: CommunityId,
        kind: CommunityInvite.Kind,
    ): OperationResult<CommunityInvite, RefreshCommunityInviteError> = operation<CommunityInvite, RefreshCommunityInviteError> {
        ensure(user.hasRole<Administrator>(), MissedAdministratorRoleError)
        val community = communityRepository.findById(communityId)
        ensure(community != null) { CommunityNotExistsError(communityId) }
        ensure(community.data.owner.id == user.id) { CommunityAccessDeniedError(communityId) }
        val existing = loadInvite(community, kind)
        val now = clock.instant()
        if (now < existing.data.expiresAt) return existing.asSuccess()
        val expiresAt = inviteExpiresAt(now = now, ttl = communityInviteConfig.ttl)
        return replaceCode(invite = existing, expiresAt = expiresAt).asSuccess()
    }

    private fun loadInvite(community: Community, kind: CommunityInvite.Kind): CommunityInvite = when (kind) {
        CommunityInvite.Kind.Manager -> community.data.managerInvite.load(managerInviteRepository)
        CommunityInvite.Kind.Developer -> community.data.developerInvite.load(developerInviteRepository)
    }

    private fun replaceCode(invite: CommunityInvite, expiresAt: Instant): CommunityInvite {
        val rawCode = generateInviteCode(random = random, previous = invite.data.codeHash)
        return update(invite) {
            code(rawCode, HashAlgorithm.Identity)
            this.expiresAt = expiresAt
        }
    }

    private fun update(invite: CommunityInvite, builder: CommunityInviteDataBuilder.() -> Unit): CommunityInvite = when (invite) {
        is CommunityInvite.Manager -> managerInviteRepository.update(invite.withData(builder))
        is CommunityInvite.Developer -> developerInviteRepository.update(invite.withData(builder))
    }
}
