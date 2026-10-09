package tech.testsys.web.app.service.administrator

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
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.user.AdministratorOperations
import tech.testsys.web.app.service.AdminUserVo
import tech.testsys.web.app.service.CommunityVo
import tech.testsys.web.app.service.CompetitionVo
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.CurrentUser
import tech.testsys.web.app.service.MultipleRoleUserVo
import tech.testsys.web.app.service.ObserverVo
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.developer.TaskValidationRequestVo
import tech.testsys.web.app.service.developer.toVo
import tech.testsys.web.app.service.judge.JudgmentOrderVo
import tech.testsys.web.app.service.judge.toVo
import tech.testsys.web.app.service.map
import tech.testsys.web.app.service.student.ClassVo
import tech.testsys.web.app.service.student.toVo
import tech.testsys.web.app.service.toAdminVo
import tech.testsys.web.app.service.toVo
import java.time.Instant

/**
 * Runs [AdministratorOperations] for the current user in one transaction per call.
 * A failed operation throws `OperationException`, which rolls the transaction back.
 *
 * @since %CURRENT_VERSION%
 */
@Service
@Transactional
class AdministratorService(private val operations: AdministratorOperations, private val currentUser: CurrentUser) {
    /**
     * Runs [AdministratorOperations.createObserver].
     *
     * @since %CURRENT_VERSION%
     */
    fun createObserver(communityId: CommunityId, observerName: String, contestIds: Set<ContestId>): ObserverVo =
        operations.createObserver(currentUser.multipleRoleUser(), communityId, observerName, contestIds).getOrThrow().toVo()

    /**
     * Runs [AdministratorOperations.viewUsers].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewUsers(pagination: Pagination, filter: UserFilter = UserFilter()): Page<Pair<AdminUserVo, Instant?>> =
        operations.viewUsers(currentUser.multipleRoleUser(), pagination, filter)
            .getOrThrow().map { (user, lastLogin) -> user.toAdminVo() to lastLogin }

    /**
     * Runs [AdministratorOperations.viewUser].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewUser(userId: UserId): Pair<AdminUserVo, Instant?> {
        val (user, lastLogin) = operations.viewUser(currentUser.multipleRoleUser(), userId).getOrThrow()
        return user.toAdminVo() to lastLogin
    }

    /**
     * Runs [AdministratorOperations.viewUserTasks].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewUserTasks(userId: UserId): List<Triple<TaskVo, TaskValidationRequestVo?, List<CommunityVo>>> =
        operations.viewUserTasks(currentUser.multipleRoleUser(), userId).getOrThrow().map { (task, request, communities) ->
            Triple(task.toVo(), request?.toVo(), communities.map { community -> community.toVo() })
        }

    /**
     * Runs [AdministratorOperations.viewUserContests].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewUserContests(userId: UserId): List<Pair<ContestVo, List<Pair<TaskVo, Long>>>> =
        operations.viewUserContests(currentUser.multipleRoleUser(), userId).getOrThrow().map { (contest, tasks) ->
            contest.toVo() to tasks.map { (task, submissions) -> task.toVo() to submissions }
        }

    /**
     * Runs [AdministratorOperations.viewUserClasses].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewUserClasses(userId: UserId): List<Pair<ClassVo, SubmissionCount>> =
        operations.viewUserClasses(currentUser.multipleRoleUser(), userId).getOrThrow().map { (group, count) -> group.toVo() to count }

    /**
     * Runs [AdministratorOperations.viewUserCompetitions].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewUserCompetitions(userId: UserId): List<Pair<CompetitionVo, SubmissionCount>> =
        operations.viewUserCompetitions(currentUser.multipleRoleUser(), userId).getOrThrow()
            .map { (competition, count) -> competition.toVo() to count }

    /**
     * Runs [AdministratorOperations.viewUserJudgments].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewUserJudgments(userId: UserId): List<Triple<JudgmentOrderVo, SolutionId, Long?>> =
        operations.viewUserJudgments(currentUser.multipleRoleUser(), userId).getOrThrow()
            .map { (order, solution, previous) -> Triple(order.toVo(), solution, previous) }

    /**
     * Runs [AdministratorOperations.viewUserAssignedContests].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewUserAssignedContests(userId: UserId): List<Pair<ContestVo, List<CompetitionVo>>> =
        operations.viewUserAssignedContests(currentUser.multipleRoleUser(), userId).getOrThrow().map { (contest, competitions) ->
            contest.toVo() to competitions.map { competition -> competition.toVo() }
        }

    /**
     * Runs [AdministratorOperations.createCommunityInvite].
     *
     * @since %CURRENT_VERSION%
     */
    fun createCommunityInvite(communityId: CommunityId, kind: CommunityInvite.Kind): CommunityInviteVo =
        operations.createCommunityInvite(currentUser.multipleRoleUser(), communityId, kind).getOrThrow().toVo()

    /**
     * Runs [AdministratorOperations.extendCommunityInvite].
     *
     * @since %CURRENT_VERSION%
     */
    fun extendCommunityInvite(communityId: CommunityId, kind: CommunityInvite.Kind): CommunityInviteVo =
        operations.extendCommunityInvite(currentUser.multipleRoleUser(), communityId, kind).getOrThrow().toVo()

    /**
     * Runs [AdministratorOperations.viewCommunityInvites].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewCommunityInvites(communityId: CommunityId): List<CommunityInviteVo> =
        operations.viewCommunityInvites(currentUser.multipleRoleUser(), communityId).getOrThrow().map { invite -> invite.toVo() }

    /**
     * Runs [AdministratorOperations.viewCommunities].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewCommunities(): List<Pair<CommunityVo, Long>> =
        operations.viewCommunities(currentUser.multipleRoleUser()).getOrThrow().map { (community, users) -> community.toVo() to users }

    /**
     * Runs [AdministratorOperations.createCommunity].
     *
     * @since %CURRENT_VERSION%
     */
    fun createCommunity(communityName: String, description: String = ""): CommunityVo = operations.createCommunity(
        user = currentUser.multipleRoleUser(),
        communityName = communityName,
        description = description,
    ).getOrThrow().toVo()

    /**
     * Runs [AdministratorOperations.editCommunity].
     *
     * @since %CURRENT_VERSION%
     */
    fun editCommunity(communityId: CommunityId, communityName: String, description: String): CommunityVo = operations.editCommunity(
        user = currentUser.multipleRoleUser(),
        communityId = communityId,
        communityName = communityName,
        description = description,
    ).getOrThrow().toVo()

    /**
     * Runs [AdministratorOperations.grantRole].
     *
     * @since %CURRENT_VERSION%
     */
    fun grantRole(userId: UserId, communityId: CommunityId, role: CommunityRole): MultipleRoleUserVo =
        operations.grantRole(currentUser.multipleRoleUser(), userId, communityId, role).getOrThrow().toVo()

    /**
     * Runs [AdministratorOperations.removeFromCommunity].
     *
     * @since %CURRENT_VERSION%
     */
    fun removeFromCommunity(userId: MultipleRoleUserId, communityId: CommunityId, role: CommunityRole): MultipleRoleUserVo =
        operations.removeFromCommunity(currentUser.multipleRoleUser(), userId, communityId, role).getOrThrow().toVo()

    /**
     * Runs [AdministratorOperations.deleteObserver].
     *
     * @since %CURRENT_VERSION%
     */
    fun deleteObserver(observerId: SingleRoleUserId): ObserverVo =
        operations.deleteObserver(currentUser.multipleRoleUser(), observerId).getOrThrow().toVo()

    /**
     * Runs [AdministratorOperations.viewCommunityContests].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewCommunityContests(communityId: CommunityId, pagination: Pagination, name: String? = null): Page<ContestVo> =
        operations.viewCommunityContests(currentUser.multipleRoleUser(), communityId, pagination, name)
            .getOrThrow().map { contest -> contest.toVo() }
}
