package tech.testsys.operation.user

import tech.testsys.domain.builder.api.observerData
import tech.testsys.domain.contract.persistence.ContestFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.SubmissionCount
import tech.testsys.domain.contract.persistence.SupervisorCommunityReadPort
import tech.testsys.domain.contract.persistence.SupervisorUserReadPort
import tech.testsys.domain.contract.persistence.UserFilter
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.RawInviteCodeDependency
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskValidationRequest
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.RawAccessTokenDependency
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.config.CommunityConfig
import tech.testsys.operation.error.AddToCommunityError
import tech.testsys.operation.error.AdministratorInPublicCommunityError
import tech.testsys.operation.error.CommunityIsPublicError
import tech.testsys.operation.error.CommunityNameBlankError
import tech.testsys.operation.error.CommunityNameTooLongError
import tech.testsys.operation.error.CommunityNotExistsError
import tech.testsys.operation.error.ContestAccessDeniedError
import tech.testsys.operation.error.ContestNotExistsError
import tech.testsys.operation.error.CreateSupervisorCommunityInviteError
import tech.testsys.operation.error.CreateSupervisorObserverError
import tech.testsys.operation.error.EditSupervisorCommunityError
import tech.testsys.operation.error.ExtendSupervisorCommunityInviteError
import tech.testsys.operation.error.GrantAdministratorError
import tech.testsys.operation.error.MissedSupervisorRoleError
import tech.testsys.operation.error.ObserverContestsEmptyError
import tech.testsys.operation.error.ObserverNameBlankError
import tech.testsys.operation.error.ObserverNameTooLongError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.RefreshSupervisorCommunityInviteError
import tech.testsys.operation.error.RemoveSupervisorMemberError
import tech.testsys.operation.error.RoleNotRemovableError
import tech.testsys.operation.error.UserHasFixedRoleError
import tech.testsys.operation.error.UserNotCommunityMemberError
import tech.testsys.operation.error.UserNotExistsError
import tech.testsys.operation.error.ViewSupervisorCommunitiesError
import tech.testsys.operation.error.ViewSupervisorCommunityContestsError
import tech.testsys.operation.error.ViewSupervisorCommunityError
import tech.testsys.operation.error.ViewSupervisorCommunityInvitesError
import tech.testsys.operation.error.ViewSupervisorUserAssignedContestsError
import tech.testsys.operation.error.ViewSupervisorUserClassesError
import tech.testsys.operation.error.ViewSupervisorUserCompetitionsError
import tech.testsys.operation.error.ViewSupervisorUserContestsError
import tech.testsys.operation.error.ViewSupervisorUserError
import tech.testsys.operation.error.ViewSupervisorUserJudgmentsError
import tech.testsys.operation.error.ViewSupervisorUserTasksError
import tech.testsys.operation.error.ViewSupervisorUsersError
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.getOrRaise
import tech.testsys.operation.error.operation
import tech.testsys.operation.util.findByIdsAsMap
import tech.testsys.operation.util.heldRole
import tech.testsys.operation.util.joinCommunity
import java.time.Instant
import java.util.UUID

private const val MAX_NAME_CODE_POINTS = 255

/**
 * Operations of a supervisor across all communities and non-supervisor users.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalOperationsApi::class)
class SupervisorOperations(
    private val users: SupervisorUserReadPort,
    private val communities: SupervisorCommunityReadPort,
    private val communityRepository: CommunityRepository,
    private val multipleRoleUsers: MultipleRoleUserRepository,
    private val userRepository: UserRepository,
    private val communityConfig: CommunityConfig,
    private val administratorOperations: AdministratorOperations,
    private val contests: ContestRepository,
    private val observers: ObserverRepository,
) {
    /**
     * Returns a page of all non-supervisor users with last logins.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.single.supervisor.authorization")
    fun viewUsers(
        user: User<*>,
        pagination: Pagination,
        filter: UserFilter = UserFilter(),
    ): OperationResult<Page<Pair<User<*>, Instant?>>, ViewSupervisorUsersError> =
        operation<Page<Pair<User<*>, Instant?>>, ViewSupervisorUsersError> {
            ensure(user is Supervisor, MissedSupervisorRoleError)
            val page = users.findUsers(pagination, filter)
            val logins = userRepository.findLastLogins(page.content.map { it.id })
            Page(page.content.map { found -> found to logins[found.id] }, page.pagination, page.totalElements)
        }

    /**
     * Returns one non-supervisor user and their last login.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.single.supervisor.authorization")
    fun viewUser(user: User<*>, userId: UserId): OperationResult<Pair<User<*>, Instant?>, ViewSupervisorUserError> =
        operation<Pair<User<*>, Instant?>, ViewSupervisorUserError> {
            ensure(user is Supervisor, MissedSupervisorRoleError)
            val found = users.findUserById(userId)
            ensure(found != null) { UserNotExistsError(userId) }
            found to userRepository.findLastLogins(listOf(userId))[userId]
        }

    /**
     * Reads the developer tasks of a non-supervisor user.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.single.supervisor.authorization")
    fun viewUserTasks(
        user: User<*>,
        userId: UserId,
    ): OperationResult<List<Triple<Task, TaskValidationRequest?, List<Community>>>, ViewSupervisorUserTasksError> =
        operation<List<Triple<Task, TaskValidationRequest?, List<Community>>>, ViewSupervisorUserTasksError> {
            administratorOperations.userTasks(viewUser(user, userId).getOrRaise().first)
        }

    /**
     * Reads the developer contests of a non-supervisor user.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.single.supervisor.authorization")
    fun viewUserContests(
        user: User<*>,
        userId: UserId,
    ): OperationResult<List<Pair<Contest, List<Pair<Task, Long>>>>, ViewSupervisorUserContestsError> =
        operation<List<Pair<Contest, List<Pair<Task, Long>>>>, ViewSupervisorUserContestsError> {
            administratorOperations.userContests(viewUser(user, userId).getOrRaise().first)
        }

    /**
     * Reads the manager classes of a non-supervisor user.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.single.supervisor.authorization")
    fun viewUserClasses(
        user: User<*>,
        userId: UserId,
    ): OperationResult<List<Pair<Class, SubmissionCount>>, ViewSupervisorUserClassesError> =
        operation<List<Pair<Class, SubmissionCount>>, ViewSupervisorUserClassesError> {
            administratorOperations.userClasses(viewUser(user, userId).getOrRaise().first)
        }

    /**
     * Reads the manager competitions of a non-supervisor user.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.single.supervisor.authorization")
    fun viewUserCompetitions(
        user: User<*>,
        userId: UserId,
    ): OperationResult<List<Pair<Competition, SubmissionCount>>, ViewSupervisorUserCompetitionsError> =
        operation<List<Pair<Competition, SubmissionCount>>, ViewSupervisorUserCompetitionsError> {
            administratorOperations.userCompetitions(viewUser(user, userId).getOrRaise().first)
        }

    /**
     * Reads the judge orders of a non-supervisor user.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.single.supervisor.authorization")
    fun viewUserJudgments(
        user: User<*>,
        userId: UserId,
    ): OperationResult<List<Triple<JudgmentOrder, SolutionId, Long?>>, ViewSupervisorUserJudgmentsError> =
        operation<List<Triple<JudgmentOrder, SolutionId, Long?>>, ViewSupervisorUserJudgmentsError> {
            administratorOperations.userJudgments(viewUser(user, userId).getOrRaise().first)
        }

    /**
     * Reads observer assignments of a non-supervisor user.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.single.supervisor.authorization")
    fun viewUserAssignedContests(
        user: User<*>,
        userId: UserId,
    ): OperationResult<List<Pair<Contest, List<Competition>>>, ViewSupervisorUserAssignedContestsError> =
        operation<List<Pair<Contest, List<Competition>>>, ViewSupervisorUserAssignedContestsError> {
            administratorOperations.userAssignedContests(viewUser(user, userId).getOrRaise().first)
        }

    /**
     * Returns all communities in ascending id order with their distinct user counts.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.single.supervisor.authorization")
    fun viewCommunities(user: User<*>): OperationResult<List<Pair<Community, Long>>, ViewSupervisorCommunitiesError> =
        operation<List<Pair<Community, Long>>, ViewSupervisorCommunitiesError> {
            ensure(user is Supervisor, MissedSupervisorRoleError)
            communities.findAllCommunities().map { community ->
                community to users.countUsersInCommunity(community.id)
            }
        }

    /**
     * Returns one community available to a supervisor.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.single.supervisor.authorization")
    fun viewCommunity(user: User<*>, communityId: CommunityId): OperationResult<Community, ViewSupervisorCommunityError> =
        operation<Community, ViewSupervisorCommunityError> {
            ensure(user is Supervisor, MissedSupervisorRoleError)
            val community = communityRepository.findById(communityId)
            ensure(community != null) { CommunityNotExistsError(communityId) }
            community
        }

    /**
     * Adds a non-fixed-role [userId] to [communityId] in [role], preserving every other membership.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.single.supervisor.addToCommunity")
    fun addToCommunity(
        user: User<*>,
        userId: UserId,
        communityId: CommunityId,
        role: CommunityRole,
    ): OperationResult<MultipleRoleUser, AddToCommunityError> = operation<MultipleRoleUser, AddToCommunityError> {
        ensure(user is Supervisor, MissedSupervisorRoleError)
        val member = users.findUserById(userId)
        ensure(member != null) { UserNotExistsError(userId) }
        val community = communityRepository.findById(communityId)
        ensure(community != null) { CommunityNotExistsError(communityId) }
        ensure(member is MultipleRoleUser) { UserHasFixedRoleError(userId) }
        ensure(role != CommunityRole.Administrator || communityId != communityConfig.publicCommunityId) {
            AdministratorInPublicCommunityError(communityId)
        }
        if (communityId in member.heldRole(role)?.memberOf?.ids.orEmpty()) return@operation member

        val isFirstAdministratorRole = role == CommunityRole.Administrator && member.heldRole(role) == null
        val joined = multipleRoleUsers.joinCommunity(member, communityId, role, communityConfig.publicCommunityId)
        if (isFirstAdministratorRole) {
            administratorOperations.createFirstCommunity(joined)
        }
        checkNotNull(multipleRoleUsers.findById(joined.id))
    }

    /**
     * Grants the administrator role to a non-fixed-role [userId] by creating their first community, or returns them
     * unchanged if they already hold the role.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.single.supervisor.grantAdministrator")
    fun grantAdministrator(user: User<*>, userId: UserId): OperationResult<MultipleRoleUser, GrantAdministratorError> =
        operation<MultipleRoleUser, GrantAdministratorError> {
            ensure(user is Supervisor, MissedSupervisorRoleError)
            val member = users.findUserById(userId)
            ensure(member != null) { UserNotExistsError(userId) }
            ensure(member is MultipleRoleUser) { UserHasFixedRoleError(userId) }
            if (member.heldRole(CommunityRole.Administrator) != null) return@operation member

            administratorOperations.createFirstCommunity(member)
            checkNotNull(multipleRoleUsers.findById(member.id))
        }

    /**
     * Edits the name and description of any community.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.editCommunity")
    fun editCommunity(
        user: User<*>,
        communityId: CommunityId,
        name: String,
        description: String,
    ): OperationResult<Community, EditSupervisorCommunityError> = operation<Community, EditSupervisorCommunityError> {
        ensure(user is Supervisor, MissedSupervisorRoleError)
        val community = communityRepository.findById(communityId)
        ensure(community != null) { CommunityNotExistsError(communityId) }
        ensure(name.isNotBlank(), CommunityNameBlankError)
        ensure(name.codePointCount(0, name.length) <= MAX_NAME_CODE_POINTS) { CommunityNameTooLongError(name) }
        administratorOperations.updateCommunity(community, name, description)
    }

    /**
     * Removes a non-administrator membership outside the public community.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.removeFromCommunity")
    fun removeFromCommunity(
        user: User<*>,
        userId: MultipleRoleUserId,
        communityId: CommunityId,
        role: CommunityRole,
    ): OperationResult<MultipleRoleUser, RemoveSupervisorMemberError> = operation<MultipleRoleUser, RemoveSupervisorMemberError> {
        ensure(user is Supervisor, MissedSupervisorRoleError)
        val member = multipleRoleUsers.findById(userId)
        ensure(member != null) { UserNotExistsError(userId) }
        val community = communityRepository.findById(communityId)
        ensure(community != null) { CommunityNotExistsError(communityId) }
        ensure(communityId != communityConfig.publicCommunityId) { CommunityIsPublicError(communityId) }
        ensure(role != CommunityRole.Administrator) { RoleNotRemovableError(role) }
        ensure(communityId in member.heldRole(role)?.memberOf?.ids.orEmpty()) {
            UserNotCommunityMemberError(userId, communityId, role)
        }
        multipleRoleUsers.removeCommunityMembership(userId, communityId, role)
    }

    /**
     * Creates an observer in any community for contests shared to it.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.createObserver")
    @RawAccessTokenDependency(reason = "Returns the issued access code while Identity hashing is in use.")
    fun createObserver(
        user: User<*>,
        communityId: CommunityId,
        name: String,
        contestIds: Set<ContestId>,
    ): OperationResult<Observer, CreateSupervisorObserverError> = operation<Observer, CreateSupervisorObserverError> {
        ensure(user is Supervisor, MissedSupervisorRoleError)
        val community = communityRepository.findById(communityId)
        ensure(community != null) { CommunityNotExistsError(communityId) }
        val byId = contests.findByIdsAsMap(contestIds)
        val selected = contestIds.map { id ->
            val contest = byId[id]
            ensure(contest != null) { ContestNotExistsError(id) }
            contest
        }
        selected.forEach { contest ->
            ensure(communityId in contest.data.sharedTo.ids) { ContestAccessDeniedError(contest.id) }
        }
        ensure(name.isNotBlank(), ObserverNameBlankError)
        ensure(name.codePointCount(0, name.length) <= MAX_NAME_CODE_POINTS) { ObserverNameTooLongError(name) }
        ensure(contestIds.isNotEmpty(), ObserverContestsEmptyError)
        val data = observerData {
            this.community = communityId
            this.name = name
            this.contests = contestIds.toMutableList()
            accessToken(UUID.randomUUID().toString(), HashAlgorithm.Identity)
        }
        observers.save(data)
    }

    /**
     * Lists contests shared to any community for observer assignment.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.createObserver")
    fun viewCommunityContests(
        user: User<*>,
        communityId: CommunityId,
        pagination: Pagination,
        name: String? = null,
    ): OperationResult<Page<Contest>, ViewSupervisorCommunityContestsError> =
        operation<Page<Contest>, ViewSupervisorCommunityContestsError> {
            ensure(user is Supervisor, MissedSupervisorRoleError)
            ensure(communityRepository.findById(communityId) != null) { CommunityNotExistsError(communityId) }
            contests.findSharedTo(setOf(communityId), pagination, ContestFilter(name = name, communityId = communityId))
        }

    /**
     * Returns both invite codes of any community.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.viewInvites")
    @RawInviteCodeDependency(reason = "Returns issued invite codes while Identity hashing is in use.")
    fun viewCommunityInvites(
        user: User<*>,
        communityId: CommunityId,
    ): OperationResult<List<CommunityInvite>, ViewSupervisorCommunityInvitesError> =
        operation<List<CommunityInvite>, ViewSupervisorCommunityInvitesError> {
            ensure(user is Supervisor, MissedSupervisorRoleError)
            val community = communityRepository.findById(communityId)
            ensure(community != null) { CommunityNotExistsError(communityId) }
            administratorOperations.communityInvites(community)
        }

    /**
     * Replaces an invite code in any community.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.inviteUser")
    @RawInviteCodeDependency(reason = "Returns the issued invite code while Identity hashing is in use.")
    fun createCommunityInvite(
        user: User<*>,
        communityId: CommunityId,
        kind: CommunityInvite.Kind,
    ): OperationResult<CommunityInvite, CreateSupervisorCommunityInviteError> =
        operation<CommunityInvite, CreateSupervisorCommunityInviteError> {
            ensure(user is Supervisor, MissedSupervisorRoleError)
            val community = communityRepository.findById(communityId)
            ensure(community != null) { CommunityNotExistsError(communityId) }
            administratorOperations.replaceCommunityInvite(community, kind)
        }

    /**
     * Extends an invite code in any community.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.extendInvite")
    @RawInviteCodeDependency(reason = "Returns the issued invite code while Identity hashing is in use.")
    fun extendCommunityInvite(
        user: User<*>,
        communityId: CommunityId,
        kind: CommunityInvite.Kind,
    ): OperationResult<CommunityInvite, ExtendSupervisorCommunityInviteError> =
        operation<CommunityInvite, ExtendSupervisorCommunityInviteError> {
            ensure(user is Supervisor, MissedSupervisorRoleError)
            val community = communityRepository.findById(communityId)
            ensure(community != null) { CommunityNotExistsError(communityId) }
            administratorOperations.extendCommunityInvite(community, kind)
        }

    /**
     * Refreshes an expired invite code in any community.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.inviteUser")
    fun refreshCommunityInvite(
        user: User<*>,
        communityId: CommunityId,
        kind: CommunityInvite.Kind,
    ): OperationResult<CommunityInvite, RefreshSupervisorCommunityInviteError> =
        operation<CommunityInvite, RefreshSupervisorCommunityInviteError> {
            ensure(user is Supervisor, MissedSupervisorRoleError)
            val community = communityRepository.findById(communityId)
            ensure(community != null) { CommunityNotExistsError(communityId) }
            administratorOperations.refreshCommunityInvite(community, kind)
        }
}
