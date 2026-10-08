package tech.testsys.operation.user

import tech.testsys.domain.builder.api.communityData
import tech.testsys.domain.builder.api.communityInviteData
import tech.testsys.domain.builder.api.observerData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.builder.group.CommunityInviteDataBuilder
import tech.testsys.domain.contract.persistence.ContestFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.UserFilter
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.ManagerCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.CommunityInviteData
import tech.testsys.domain.model.group.RawInviteCodeDependency
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.Administrator
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.RawAccessTokenDependency
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.config.CommunityInviteConfig
import tech.testsys.operation.error.CommunityAccessDeniedError
import tech.testsys.operation.error.CommunityNameBlankError
import tech.testsys.operation.error.CommunityNameTooLongError
import tech.testsys.operation.error.CommunityNotExistsError
import tech.testsys.operation.error.ContestAccessDeniedError
import tech.testsys.operation.error.ContestNotExistsError
import tech.testsys.operation.error.CreateCommunityError
import tech.testsys.operation.error.CreateCommunityInviteError
import tech.testsys.operation.error.CreateObserverError
import tech.testsys.operation.error.EditCommunityError
import tech.testsys.operation.error.ExtendCommunityInviteError
import tech.testsys.operation.error.GrantRoleError
import tech.testsys.operation.error.MissedAdministratorRoleError
import tech.testsys.operation.error.ObserverContestsEmptyError
import tech.testsys.operation.error.ObserverNameBlankError
import tech.testsys.operation.error.ObserverNameTooLongError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.RefreshCommunityInviteError
import tech.testsys.operation.error.UserAccessDeniedError
import tech.testsys.operation.error.UserHasFixedRoleError
import tech.testsys.operation.error.UserNotExistsError
import tech.testsys.operation.error.ViewCommunitiesError
import tech.testsys.operation.error.ViewCommunityContestsError
import tech.testsys.operation.error.ViewCommunityInvitesError
import tech.testsys.operation.error.ViewUserError
import tech.testsys.operation.error.ViewUsersError
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.operation
import tech.testsys.operation.error.raise
import tech.testsys.operation.util.findByIdsAsMap
import tech.testsys.operation.util.generateInviteCode
import tech.testsys.operation.util.hasRole
import tech.testsys.operation.util.inviteExpiresAt
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.UUID

private const val MAX_OBSERVER_NAME_CODE_POINTS = 255

private const val MAX_COMMUNITY_NAME_CODE_POINTS = 255

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
    private val contestRepository: ContestRepository,
    private val observerRepository: ObserverRepository,
    private val multipleRoleUserRepository: MultipleRoleUserRepository,
) {

    private val random = SecureRandom()

    /**
     * Creates an observer named [observerName] in [communityId] owned by [user], assigning exactly [contestIds].
     * Only contests shared to that community are allowed; a fresh access code is returned with the saved observer.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.createObserver")
    @RawAccessTokenDependency(reason = "Returns the issued access code in the saved observer's stored value with Identity.")
    fun createObserver(
        user: MultipleRoleUser,
        communityId: CommunityId,
        observerName: String,
        contestIds: Set<ContestId>,
    ): OperationResult<Observer, CreateObserverError> = operation<Observer, CreateObserverError> {
        ensure(user.hasRole<Administrator>(), MissedAdministratorRoleError)
        val community = communityRepository.findById(communityId)
        ensure(community != null) { CommunityNotExistsError(communityId) }
        val contestsById = contestRepository.findByIdsAsMap(contestIds)
        val contests = contestIds.map { contestId ->
            val contest = contestsById[contestId]
            ensure(contest != null) { ContestNotExistsError(contestId) }
            contest
        }
        ensure(community.data.owner.id == user.id) { CommunityAccessDeniedError(communityId) }
        contests.forEach { contest ->
            ensure(communityId in contest.data.sharedTo.ids) { ContestAccessDeniedError(contest.id) }
        }
        ensure(observerName.isNotBlank(), ObserverNameBlankError)
        ensure(observerName.codePointCount(0, observerName.length) <= MAX_OBSERVER_NAME_CODE_POINTS) {
            ObserverNameTooLongError(observerName)
        }
        ensure(contestIds.isNotEmpty(), ObserverContestsEmptyError)
        val data = observerData {
            this.community = communityId
            name = observerName
            this.contests = contestIds.toMutableList()
            accessToken(UUID.randomUUID().toString(), algorithm = HashAlgorithm.Identity)
        }
        return observerRepository.save(data).asSuccess()
    }

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
    ): OperationResult<Page<Pair<User<*>, Instant?>>, ViewUsersError> = operation<Page<Pair<User<*>, Instant?>>, ViewUsersError> {
        ensure(user.hasRole<Administrator>(), MissedAdministratorRoleError)
        val users = userRepository.findAvailableToAdministrator(administratorId = user.id, pagination = pagination, filter = filter)
        val lastLogins = userRepository.findLastLogins(users.content.map { found -> found.id })
        val content = users.content.map { found -> found to lastLogins[found.id] }
        return Page(content = content, pagination = users.pagination, totalElements = users.totalElements).asSuccess()
    }

    /**
     * Returns [userId] with all its stored data and its last login, or `null` without one, if it is available to [user],
     * preserving stored state. Missing role, user and access are expected failures; storage exceptions propagate.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.viewUser")
    @RawAccessTokenDependency(
        reason = "The returned user carries the stored access code that the administrator sees as the original code only with Identity.",
    )
    fun viewUser(user: MultipleRoleUser, userId: UserId): OperationResult<Pair<User<*>, Instant?>, ViewUserError> =
        operation<Pair<User<*>, Instant?>, ViewUserError> {
            ensure(user.hasRole<Administrator>(), MissedAdministratorRoleError)
            val viewed = userRepository.findAvailableToAdministratorById(administratorId = user.id, userId = userId)
            if (viewed == null) {
                ensure(userRepository.existsById(userId)) { UserNotExistsError(userId) }
                UserAccessDeniedError(userId).raise()
            }
            val lastLogin = userRepository.findLastLogins(listOf(viewed.id))[viewed.id]
            return (viewed to lastLogin).asSuccess()
        }

    /**
     * Returns the communities created by [user] in ascending id order, each with the number of users available to [user]
     * through it, the administrator included. Missing role is an expected failure; nothing is written.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.viewCommunities")
    fun viewCommunities(user: MultipleRoleUser): OperationResult<List<Pair<Community, Long>>, ViewCommunitiesError> =
        operation<List<Pair<Community, Long>>, ViewCommunitiesError> {
            ensure(user.hasRole<Administrator>(), MissedAdministratorRoleError)
            // ponytail: one count query per community; a grouped count query if administrators own many communities.
            val communities = communityRepository.findByOwner(user.id).map { community ->
                community to userRepository.countAvailableToAdministrator(
                    administratorId = user.id,
                    filter = UserFilter(communityId = community.id),
                )
            }
            return communities.asSuccess()
        }

    /**
     * Creates a community owned by [user] with unchanged [communityName] and [description], no members and new manager and
     * developer invite codes. The name must be nonblank and contain at most 255 Unicode code points; storage exceptions,
     * including an invite code collision, propagate to the caller.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.createCommunity")
    fun createCommunity(
        user: MultipleRoleUser,
        communityName: String,
        description: String = "",
    ): OperationResult<Community, CreateCommunityError> = operation<Community, CreateCommunityError> {
        ensure(user.hasRole<Administrator>(), MissedAdministratorRoleError)
        ensure(communityName.isNotBlank(), CommunityNameBlankError)
        ensure(communityName.codePointCount(0, communityName.length) <= MAX_COMMUNITY_NAME_CODE_POINTS) {
            CommunityNameTooLongError(communityName)
        }

        val expiresAt = inviteExpiresAt(now = clock.instant(), ttl = communityInviteConfig.ttl)
        val managerInviteData = newInvite(expiresAt)
        val developerInviteData = newInvite(expiresAt)
        val community = communityRepository.saveWithInvites(
            managerInvite = managerInviteData,
            developerInvite = developerInviteData,
        ) { managerInviteId, developerInviteId ->
            communityData {
                owner = user.id
                name = communityName
                this.description = description
                managerInvite = managerInviteId
                developerInvite = developerInviteId
            }
        }
        return community.asSuccess()
    }

    /**
     * Replaces the name and description of [communityId] owned by [user] with unchanged [communityName] and [description],
     * keeping its owner, invite codes and members. Name rules match [createCommunity]; missing role, community and access
     * are expected failures.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.editCommunity")
    fun editCommunity(
        user: MultipleRoleUser,
        communityId: CommunityId,
        communityName: String,
        description: String,
    ): OperationResult<Community, EditCommunityError> = operation<Community, EditCommunityError> {
        ensure(user.hasRole<Administrator>(), MissedAdministratorRoleError)
        val community = communityRepository.findById(communityId)
        ensure(community != null) { CommunityNotExistsError(communityId) }
        ensure(community.data.owner.id == user.id) { CommunityAccessDeniedError(communityId) }
        ensure(communityName.isNotBlank(), CommunityNameBlankError)
        ensure(communityName.codePointCount(0, communityName.length) <= MAX_COMMUNITY_NAME_CODE_POINTS) {
            CommunityNameTooLongError(communityName)
        }

        val edited = community.withData {
            name = communityName
            this.description = description
        }
        return communityRepository.update(edited).asSuccess()
    }

    /**
     * Makes [userId], available to [user] and holding non-fixed roles, a member of [communityId] owned by [user] in [role];
     * a member already in that role is returned unchanged. Roles in other communities are kept; missing role, user,
     * community, access and a fixed role of the user are expected failures.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.grantRole")
    fun grantRole(
        user: MultipleRoleUser,
        userId: UserId,
        communityId: CommunityId,
        role: CommunityRole,
    ): OperationResult<MultipleRoleUser, GrantRoleError> = operation<MultipleRoleUser, GrantRoleError> {
        ensure(user.hasRole<Administrator>(), MissedAdministratorRoleError)
        ensure(userRepository.existsById(userId)) { UserNotExistsError(userId) }
        val community = communityRepository.findById(communityId)
        ensure(community != null) { CommunityNotExistsError(communityId) }
        val granted = userRepository.findAvailableToAdministratorById(administratorId = user.id, userId = userId)
        ensure(granted != null) { UserAccessDeniedError(userId) }
        ensure(community.data.owner.id == user.id) { CommunityAccessDeniedError(communityId) }
        ensure(granted is MultipleRoleUser) { UserHasFixedRoleError(userId) }

        // The port keeps an existing membership, so a user already in the role is returned unchanged.
        val member = multipleRoleUserRepository.addCommunityMembership(userId = granted.id, communityId = communityId, role = role)
        return member.asSuccess()
    }

    /**
     * Returns a [pagination] page of the contests shared to [communityId] owned by [user] whose names contain [name],
     * from which an observer of the community is assigned. Missing role, community and access are expected failures.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.createObserver")
    fun viewCommunityContests(
        user: MultipleRoleUser,
        communityId: CommunityId,
        pagination: Pagination,
        name: String? = null,
    ): OperationResult<Page<Contest>, ViewCommunityContestsError> = operation<Page<Contest>, ViewCommunityContestsError> {
        ensure(user.hasRole<Administrator>(), MissedAdministratorRoleError)
        val community = communityRepository.findById(communityId)
        ensure(community != null) { CommunityNotExistsError(communityId) }
        ensure(community.data.owner.id == user.id) { CommunityAccessDeniedError(communityId) }

        val contests = contestRepository.findAvailableToDeveloper(
            ownerId = user.id,
            communityIds = setOf(communityId),
            pagination = pagination,
            filter = ContestFilter(name = name, communityId = communityId),
        )
        return contests.asSuccess()
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

    private fun newInvite(expiresAt: Instant): CommunityInviteData = communityInviteData {
        code(generateInviteCode(random = random, previous = null), HashAlgorithm.Identity)
        this.expiresAt = expiresAt
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
