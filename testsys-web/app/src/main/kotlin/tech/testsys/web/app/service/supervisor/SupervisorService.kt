package tech.testsys.web.app.service.supervisor

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.SubmissionCount
import tech.testsys.domain.contract.persistence.UserFilter
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.UserId
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.user.SupervisorOperations
import tech.testsys.web.app.service.AdminUserVo
import tech.testsys.web.app.service.CommunityManagementService
import tech.testsys.web.app.service.CommunityVo
import tech.testsys.web.app.service.CompetitionVo
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.CurrentUser
import tech.testsys.web.app.service.MultipleRoleUserVo
import tech.testsys.web.app.service.ObserverVo
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.UserRoleReadService
import tech.testsys.web.app.service.administrator.CommunityInviteVo
import tech.testsys.web.app.service.administrator.toVo
import tech.testsys.web.app.service.developer.TaskValidationRequestVo
import tech.testsys.web.app.service.developer.toVo
import tech.testsys.web.app.service.judge.JudgmentOrderVo
import tech.testsys.web.app.service.judge.toVo
import tech.testsys.web.app.service.map
import tech.testsys.web.app.service.student.ClassVo
import tech.testsys.web.app.service.student.toVo
import tech.testsys.web.app.service.toSupervisorVo
import tech.testsys.web.app.service.toVo
import java.time.Instant

/**
 * Runs supervisor operations for the signed-in user in one transaction per call.
 *
 * @since %CURRENT_VERSION%
 */
@Service
@Transactional
class SupervisorService(private val operations: SupervisorOperations, private val currentUser: CurrentUser) :
    CommunityManagementService,
    UserRoleReadService {
    @Transactional(readOnly = true)
    override fun viewUsers(pagination: Pagination, filter: UserFilter): Page<Pair<AdminUserVo, Instant?>> =
        operations.viewUsers(currentUser.user(), pagination, filter).getOrThrow()
            .map { (user, login) -> user.toSupervisorVo() to login }

    /**
     * Reads one non-supervisor user.
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewUser(userId: UserId): Pair<AdminUserVo, Instant?> = operations.viewUser(currentUser.user(), userId)
        .getOrThrow().let { (user, login) -> user.toSupervisorVo() to login }

    /**
     * Resolves a route id to either a multiple-role or a fixed-role user.
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewUserByIdValue(id: Long): Pair<AdminUserVo, Instant?> {
        val multiple = operations.viewUser(currentUser.user(), MultipleRoleUserId(id))
        return when (multiple) {
            is OperationResult.Success -> multiple.value.let { (user, login) -> user.toSupervisorVo() to login }
            is OperationResult.Error -> viewUser(SingleRoleUserId(id))
        }
    }

    @Transactional(readOnly = true)
    override fun viewUserTasks(userId: UserId): List<Triple<TaskVo, TaskValidationRequestVo?, List<CommunityVo>>> =
        operations.viewUserTasks(currentUser.user(), userId).getOrThrow().map { (task, request, communities) ->
            Triple(task.toVo(), request?.toVo(), communities.map { it.toVo() })
        }

    @Transactional(readOnly = true)
    override fun viewUserContests(userId: UserId): List<Pair<ContestVo, List<Pair<TaskVo, Long>>>> =
        operations.viewUserContests(currentUser.user(), userId).getOrThrow().map { (contest, tasks) ->
            contest.toVo() to tasks.map { (task, count) -> task.toVo() to count }
        }

    @Transactional(readOnly = true)
    override fun viewUserClasses(userId: UserId): List<Pair<ClassVo, SubmissionCount>> =
        operations.viewUserClasses(currentUser.user(), userId).getOrThrow().map { (group, count) -> group.toVo() to count }

    @Transactional(readOnly = true)
    override fun viewUserCompetitions(userId: UserId): List<Pair<CompetitionVo, SubmissionCount>> =
        operations.viewUserCompetitions(currentUser.user(), userId).getOrThrow().map { (competition, count) ->
            competition.toVo() to count
        }

    @Transactional(readOnly = true)
    override fun viewUserJudgments(userId: UserId): List<Triple<JudgmentOrderVo, SolutionId, Long?>> =
        operations.viewUserJudgments(currentUser.user(), userId).getOrThrow().map { (order, solution, previous) ->
            Triple(order.toVo(), solution, previous)
        }

    @Transactional(readOnly = true)
    override fun viewUserAssignedContests(userId: UserId): List<Pair<ContestVo, List<CompetitionVo>>> =
        operations.viewUserAssignedContests(currentUser.user(), userId).getOrThrow().map { (contest, competitions) ->
            contest.toVo() to competitions.map { it.toVo() }
        }

    @Transactional(readOnly = true)
    override fun viewCommunities(): List<Pair<CommunityVo, Long>> {
        return operations.viewCommunities(currentUser.user()).getOrThrow().map { (community, count) ->
            community.toVo() to count
        }
    }

    /**
     * Reads one community.
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewCommunity(communityId: CommunityId): CommunityVo {
        return operations.viewCommunity(currentUser.user(), communityId).getOrThrow().toVo()
    }

    /**
     * Adds a non-fixed-role user to any community.
     *
     * @since %CURRENT_VERSION%
     */
    fun addToCommunity(userId: UserId, communityId: CommunityId, role: CommunityRole): MultipleRoleUserVo =
        operations.addToCommunity(currentUser.user(), userId, communityId, role).getOrThrow().toVo()

    /**
     * Grants the administrator role to a non-fixed-role user without choosing a community.
     *
     * @since %CURRENT_VERSION%
     */
    fun grantAdministrator(userId: UserId): MultipleRoleUserVo =
        operations.grantAdministrator(currentUser.user(), userId).getOrThrow().toVo()

    override fun editCommunity(communityId: CommunityId, communityName: String, description: String): CommunityVo =
        operations.editCommunity(currentUser.user(), communityId, communityName, description).getOrThrow().toVo()

    /**
     * Removes one membership from a community.
     *
     * @since %CURRENT_VERSION%
     */
    fun removeFromCommunity(userId: MultipleRoleUserId, communityId: CommunityId, role: CommunityRole): MultipleRoleUserVo =
        operations.removeFromCommunity(currentUser.user(), userId, communityId, role).getOrThrow().toVo()

    override fun createObserver(communityId: CommunityId, observerName: String, contestIds: Set<ContestId>): ObserverVo =
        operations.createObserver(currentUser.user(), communityId, observerName, contestIds).getOrThrow().toVo()

    @Transactional(readOnly = true)
    override fun viewCommunityContests(communityId: CommunityId, pagination: Pagination, name: String?): Page<ContestVo> =
        operations.viewCommunityContests(currentUser.user(), communityId, pagination, name).getOrThrow().map { it.toVo() }

    @Transactional(readOnly = true)
    override fun viewCommunityInvites(communityId: CommunityId): List<CommunityInviteVo> =
        operations.viewCommunityInvites(currentUser.user(), communityId).getOrThrow().map { it.toVo() }

    override fun createCommunityInvite(communityId: CommunityId, kind: CommunityInvite.Kind): CommunityInviteVo =
        operations.createCommunityInvite(currentUser.user(), communityId, kind).getOrThrow().toVo()

    override fun extendCommunityInvite(communityId: CommunityId, kind: CommunityInvite.Kind): CommunityInviteVo =
        operations.extendCommunityInvite(currentUser.user(), communityId, kind).getOrThrow().toVo()

    /**
     * Refreshes an expired invite code.
     *
     * @since %CURRENT_VERSION%
     */
    fun refreshCommunityInvite(communityId: CommunityId, kind: CommunityInvite.Kind): CommunityInviteVo =
        operations.refreshCommunityInvite(currentUser.user(), communityId, kind).getOrThrow().toVo()
}
