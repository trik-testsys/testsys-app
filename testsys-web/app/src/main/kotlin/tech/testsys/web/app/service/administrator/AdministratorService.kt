package tech.testsys.web.app.service.administrator

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.UserFilter
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.UserId
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.user.AdministratorOperations
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.CurrentUser
import tech.testsys.web.app.service.map
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
     * Runs [AdministratorOperations.viewCommunityContests].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewCommunityContests(communityId: CommunityId, pagination: Pagination, name: String? = null): Page<ContestVo> =
        operations.viewCommunityContests(currentUser.multipleRoleUser(), communityId, pagination, name)
            .getOrThrow().map { contest -> contest.toVo() }
}
