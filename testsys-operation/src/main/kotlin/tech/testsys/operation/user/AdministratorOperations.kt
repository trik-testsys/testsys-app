package tech.testsys.operation.user

import tech.testsys.domain.builder.api.communityData
import tech.testsys.domain.builder.api.communityInviteData
import tech.testsys.domain.builder.api.observerData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.builder.group.CommunityInviteDataBuilder
import tech.testsys.domain.contract.persistence.ContestFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.SubmissionCount
import tech.testsys.domain.contract.persistence.UserFilter
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.JudgmentOrderRepository
import tech.testsys.domain.contract.persistence.repository.ManagerCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.TaskValidationRequestRepository
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.CommunityInviteData
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.RawInviteCodeDependency
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskValidationRequest
import tech.testsys.domain.model.user.Administrator
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.CompatibleUserRole
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.Judge
import tech.testsys.domain.model.user.Manager
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.RawAccessTokenDependency
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.config.CommunityConfig
import tech.testsys.operation.config.CommunityInviteConfig
import tech.testsys.operation.error.CommunityAccessDeniedError
import tech.testsys.operation.error.CommunityIsPublicError
import tech.testsys.operation.error.CommunityNameBlankError
import tech.testsys.operation.error.CommunityNameTooLongError
import tech.testsys.operation.error.CommunityNotExistsError
import tech.testsys.operation.error.ContestAccessDeniedError
import tech.testsys.operation.error.ContestNotExistsError
import tech.testsys.operation.error.CreateCommunityError
import tech.testsys.operation.error.CreateCommunityInviteError
import tech.testsys.operation.error.CreateObserverError
import tech.testsys.operation.error.DeleteObserverError
import tech.testsys.operation.error.EditCommunityError
import tech.testsys.operation.error.ExtendCommunityInviteError
import tech.testsys.operation.error.GrantRoleError
import tech.testsys.operation.error.MissedAdministratorRoleError
import tech.testsys.operation.error.ObserverContestsEmptyError
import tech.testsys.operation.error.ObserverNameBlankError
import tech.testsys.operation.error.ObserverNameTooLongError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.RefreshCommunityInviteError
import tech.testsys.operation.error.RemoveFromCommunityError
import tech.testsys.operation.error.RoleNotGrantableError
import tech.testsys.operation.error.RoleNotRemovableError
import tech.testsys.operation.error.UserAccessDeniedError
import tech.testsys.operation.error.UserHasFixedRoleError
import tech.testsys.operation.error.UserNotCommunityMemberError
import tech.testsys.operation.error.UserNotExistsError
import tech.testsys.operation.error.ViewCommunitiesError
import tech.testsys.operation.error.ViewCommunityContestsError
import tech.testsys.operation.error.ViewCommunityInvitesError
import tech.testsys.operation.error.ViewUserAssignedContestsError
import tech.testsys.operation.error.ViewUserClassesError
import tech.testsys.operation.error.ViewUserCompetitionsError
import tech.testsys.operation.error.ViewUserContestsError
import tech.testsys.operation.error.ViewUserError
import tech.testsys.operation.error.ViewUserJudgmentsError
import tech.testsys.operation.error.ViewUserTasksError
import tech.testsys.operation.error.ViewUsersError
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.getOrRaise
import tech.testsys.operation.error.operation
import tech.testsys.operation.error.raise
import tech.testsys.operation.util.findByIdsAsMap
import tech.testsys.operation.util.generateInviteCode
import tech.testsys.operation.util.hasRole
import tech.testsys.operation.util.heldRole
import tech.testsys.operation.util.inviteExpiresAt
import tech.testsys.operation.util.joinCommunity
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
    private val communityConfig: CommunityConfig,
    private val taskRepository: TaskRepository,
    private val taskValidationRequestRepository: TaskValidationRequestRepository,
    private val classRepository: ClassRepository,
    private val competitionRepository: CompetitionRepository,
    private val submissionRepository: SubmissionRepository,
    private val judgmentOrderRepository: JudgmentOrderRepository,
    private val verdictRepository: VerdictRepository,
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
     * Returns the tasks created by [userId] viewed as in [viewUser], each with its latest validation request, or `null`
     * if never validated, and the communities it is shared to; empty if the viewed user is not a developer.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.viewUser")
    fun viewUserTasks(
        user: MultipleRoleUser,
        userId: UserId,
    ): OperationResult<List<Triple<Task, TaskValidationRequest?, List<Community>>>, ViewUserTasksError> =
        operation<List<Triple<Task, TaskValidationRequest?, List<Community>>>, ViewUserTasksError> {
            val developer = viewedRole<Developer>(user, userId).getOrRaise { error ->
                when (error) {
                    MissedAdministratorRoleError -> MissedAdministratorRoleError
                    is UserNotExistsError -> error
                    is UserAccessDeniedError -> error
                }
            } ?: return emptyList<Nothing>().asSuccess()
            val tasks = taskRepository.findByIds(developer.data.tasks.ids)
            val communities = communityRepository.findByIds(tasks.flatMap { task -> task.data.sharedTo.ids }.distinct())
                .associateBy { community -> community.id }

            // ponytail: one history query per task; a query of the latest requests if developers own many tasks.
            return tasks.map { task ->
                Triple(
                    task,
                    taskValidationRequestRepository.findHistory(task.id).lastOrNull(),
                    task.data.sharedTo.ids.mapNotNull(communities::get),
                )
            }.asSuccess()
        }

    /**
     * Returns the contests created by [userId] viewed as in [viewUser], each with its tasks and the number of grading
     * submissions of every task in the contest; empty if the viewed user is not a developer.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.viewUser")
    fun viewUserContests(
        user: MultipleRoleUser,
        userId: UserId,
    ): OperationResult<List<Pair<Contest, List<Pair<Task, Long>>>>, ViewUserContestsError> =
        operation<List<Pair<Contest, List<Pair<Task, Long>>>>, ViewUserContestsError> {
            val developer = viewedRole<Developer>(user, userId).getOrRaise { error ->
                when (error) {
                    MissedAdministratorRoleError -> MissedAdministratorRoleError
                    is UserNotExistsError -> error
                    is UserAccessDeniedError -> error
                }
            } ?: return emptyList<Nothing>().asSuccess()
            val contests = contestRepository.findByIds(developer.data.contests.ids)
            val tasks = taskRepository.findByIds(contests.flatMap { contest -> contest.data.tasks.ids }.distinct())
                .associateBy { task -> task.id }

            // ponytail: one count query per contest; a grouped query over all contests if developers own many contests.
            return contests.map { contest ->
                val counts = submissionRepository.countGradingByTask(contest.id)
                contest to contest.data.tasks.ids.map { taskId -> tasks.getValue(taskId) to (counts[taskId] ?: 0L) }
            }.asSuccess()
        }

    /**
     * Returns the classes created by [userId] viewed as in [viewUser], each with the grading submissions of its current
     * students in its current contests; empty if the viewed user is not a manager.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.viewUser")
    fun viewUserClasses(
        user: MultipleRoleUser,
        userId: UserId,
    ): OperationResult<List<Pair<Class, SubmissionCount>>, ViewUserClassesError> =
        operation<List<Pair<Class, SubmissionCount>>, ViewUserClassesError> {
            val manager = viewedRole<Manager>(user, userId).getOrRaise { error ->
                when (error) {
                    MissedAdministratorRoleError -> MissedAdministratorRoleError
                    is UserNotExistsError -> error
                    is UserAccessDeniedError -> error
                }
            } ?: return emptyList<Nothing>().asSuccess()

            // ponytail: one count query per class; a grouped query if managers own many classes.
            return classRepository.findByIds(manager.data.classes.ids).map { group ->
                group to submissionRepository.countGrading(group.data.students.ids.toSet(), group.data.contests.ids.toSet())
            }.asSuccess()
        }

    /**
     * Returns the competitions created by [userId] viewed as in [viewUser], each with the grading submissions of its
     * current participants in its contests; empty if the viewed user is not a manager.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.viewUser")
    fun viewUserCompetitions(
        user: MultipleRoleUser,
        userId: UserId,
    ): OperationResult<List<Pair<Competition, SubmissionCount>>, ViewUserCompetitionsError> =
        operation<List<Pair<Competition, SubmissionCount>>, ViewUserCompetitionsError> {
            val manager = viewedRole<Manager>(user, userId).getOrRaise { error ->
                when (error) {
                    MissedAdministratorRoleError -> MissedAdministratorRoleError
                    is UserNotExistsError -> error
                    is UserAccessDeniedError -> error
                }
            } ?: return emptyList<Nothing>().asSuccess()

            // ponytail: one count query per competition; a grouped query if managers own many competitions.
            return competitionRepository.findByIds(manager.data.competitions.ids).map { competition ->
                competition to submissionRepository.countGrading(
                    competition.data.participants.ids.toSet(),
                    competition.data.contests.ids.toSet(),
                )
            }.asSuccess()
        }

    /**
     * Returns the judgment orders issued by [userId] viewed as in [viewUser], each with the solution of its submission and
     * the previous result of the submission, or `null` without one; empty if the viewed user is not a judge.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.viewUser")
    fun viewUserJudgments(
        user: MultipleRoleUser,
        userId: UserId,
    ): OperationResult<List<Triple<JudgmentOrder, SolutionId, Long?>>, ViewUserJudgmentsError> =
        operation<List<Triple<JudgmentOrder, SolutionId, Long?>>, ViewUserJudgmentsError> {
            val judge = viewedRole<Judge>(user, userId).getOrRaise { error ->
                when (error) {
                    MissedAdministratorRoleError -> MissedAdministratorRoleError
                    is UserNotExistsError -> error
                    is UserAccessDeniedError -> error
                }
            } ?: return emptyList<Nothing>().asSuccess()

            // ponytail: reads the submission and its judgment orders per order; batch them if judges issue many orders.
            return judgmentOrderRepository.findByIds(judge.data.judgmentOrders.ids).map { order ->
                val submission = submissionOf(order)
                Triple(order, submission.data.solution.id, previousScore(order, submission))
            }.asSuccess()
        }

    /**
     * Returns the contests assigned to the observer [userId] viewed as in [viewUser], each with the competitions it is
     * currently part of; empty if the viewed user is not an observer.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.viewUser")
    fun viewUserAssignedContests(
        user: MultipleRoleUser,
        userId: UserId,
    ): OperationResult<List<Pair<Contest, List<Competition>>>, ViewUserAssignedContestsError> =
        operation<List<Pair<Contest, List<Competition>>>, ViewUserAssignedContestsError> {
            val (viewed, _) = viewUser(user, userId).getOrRaise { error ->
                when (error) {
                    MissedAdministratorRoleError -> MissedAdministratorRoleError
                    is UserNotExistsError -> error
                    is UserAccessDeniedError -> error
                }
            }
            val observer = viewed as? Observer ?: return emptyList<Nothing>().asSuccess()
            val contests = contestRepository.findByIds(observer.data.contests.ids)
            val competitions = competitionRepository.findByContestIds(contests.map { contest -> contest.id }.toSet())

            return contests.map { contest ->
                contest to competitions.filter { competition -> contest.id in competition.data.contests.ids }
            }.asSuccess()
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
     * Creates a community owned by [user] with unchanged [communityName] and [description], [user] as its only member in the
     * administrator role and new manager and developer invite codes. The name must be nonblank and contain at most 255
     * Unicode code points; storage exceptions, including an invite code collision, propagate to the caller.
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
        multipleRoleUserRepository.addCommunityMembership(
            userId = user.id,
            communityId = community.id,
            role = CommunityRole.Administrator,
        )
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
     * Makes [userId], available to [user] and holding non-fixed roles, a member of [communityId] owned by [user] in [role],
     * and of the public community if the role is new; a member already in that role is returned unchanged. Missing role,
     * user, community, access, a fixed role of the user and the judge role are expected failures.
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
        ensure(role != CommunityRole.Judge && role != CommunityRole.Administrator) { RoleNotGrantableError(role) }

        // The port keeps an existing membership, so a user already in the role is returned unchanged.
        val member = multipleRoleUserRepository.joinCommunity(
            user = granted,
            communityId = communityId,
            role = role,
            publicCommunityId = communityConfig.publicCommunityId,
        )
        return member.asSuccess()
    }

    /**
     * Removes the membership of [userId] in [communityId] owned by [user] in [role], keeping the role and its data.
     * Missing role, user, community, access, the public community and a missing membership are expected failures.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.removeFromCommunity")
    fun removeFromCommunity(
        user: MultipleRoleUser,
        userId: MultipleRoleUserId,
        communityId: CommunityId,
        role: CommunityRole,
    ): OperationResult<MultipleRoleUser, RemoveFromCommunityError> = operation<MultipleRoleUser, RemoveFromCommunityError> {
        ensure(user.hasRole<Administrator>(), MissedAdministratorRoleError)
        val member = multipleRoleUserRepository.findById(userId)
        ensure(member != null) { UserNotExistsError(userId) }
        val community = communityRepository.findById(communityId)
        ensure(community != null) { CommunityNotExistsError(communityId) }
        ensure(community.data.owner.id == user.id) { CommunityAccessDeniedError(communityId) }
        ensure(communityId != communityConfig.publicCommunityId) { CommunityIsPublicError(communityId) }
        ensure(role != CommunityRole.Administrator) { RoleNotRemovableError(role) }
        ensure(communityId in member.heldRole(role)?.memberOf?.ids.orEmpty()) {
            UserNotCommunityMemberError(userId = userId, communityId = communityId, role = role)
        }

        return multipleRoleUserRepository.removeCommunityMembership(userId = userId, communityId = communityId, role = role)
            .asSuccess()
    }

    /**
     * Deletes [observerId] of a community owned by [user], so that its access code stops working, and returns the observer
     * as it was before the deletion. Missing role, observer and access are expected failures.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.admin.deleteObserver")
    fun deleteObserver(user: MultipleRoleUser, observerId: SingleRoleUserId): OperationResult<Observer, DeleteObserverError> =
        operation<Observer, DeleteObserverError> {
            ensure(user.hasRole<Administrator>(), MissedAdministratorRoleError)
            val observer = observerRepository.findById(observerId)
            ensure(observer != null) { UserNotExistsError(observerId) }
            val community = checkNotNull(communityRepository.findById(observer.data.community.id)) {
                "Community ${observer.data.community.id.value} of observer ${observerId.value} does not exist"
            }
            ensure(community.data.owner.id == user.id) { UserAccessDeniedError(observerId) }

            observerRepository.removeById(observerId)
            return observer.asSuccess()
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

    /** Returns the role [R] of [userId] viewed by [user] as in [viewUser], or `null` if the viewed user does not hold it. */
    private inline fun <reified R : CompatibleUserRole> viewedRole(
        user: MultipleRoleUser,
        userId: UserId,
    ): OperationResult<R?, ViewUserError> = operation<R?, ViewUserError> {
        val (viewed, _) = viewUser(user, userId).getOrRaise()
        (viewed as? MultipleRoleUser)?.data?.roles?.filterIsInstance<R>()?.singleOrNull()
    }

    /**
     * Returns the score of the judgment order of the same submission issued just before [order], otherwise the verdict
     * total of the submission, or `null` without a successful verdict.
     */
    private fun submissionOf(order: JudgmentOrder): Submission {
        val submissionId = order.data.submission.id
        return checkNotNull(submissionRepository.findById(submissionId)) {
            "Submission ${submissionId.value} of judgment order ${order.id.value} does not exist"
        }
    }

    private fun previousScore(order: JudgmentOrder, submission: Submission): Long? {
        val issueOrder = compareBy<JudgmentOrder> { other -> other.createdAt }.thenBy { other -> other.id.value }
        val previous = judgmentOrderRepository.findByIds(submission.data.judgmentOrders.ids)
            .filter { other -> issueOrder.compare(other, order) < 0 }
            .maxWithOrNull(issueOrder)
        if (previous != null) return previous.data.score.value.toLong()

        val verdict = when (val status = submission.data.status) {
            SubmissionStatus.Queued, SubmissionStatus.InProgress -> return null
            is SubmissionStatus.Graded -> when (val grade = status.grade) {
                is GradingResult.Success -> grade.verdict
                is GradingResult.GradingError, GradingResult.Timeout -> return null
            }
        }
        val testVerdicts = checkNotNull(verdictRepository.findById(verdict.id)) {
            "Verdict ${verdict.id.value} of submission ${submission.id.value} does not exist"
        }.data.testVerdicts
        return testVerdicts.sumOf { testVerdict -> testVerdict.score.value.toLong() }
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
