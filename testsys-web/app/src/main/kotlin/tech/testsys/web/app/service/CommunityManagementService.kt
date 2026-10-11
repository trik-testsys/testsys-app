package tech.testsys.web.app.service

import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.task.ContestId
import tech.testsys.web.app.service.administrator.CommunityInviteVo

/**
 * Community data and actions shared by administrator and supervisor pages under their own access rules.
 *
 * @since %CURRENT_VERSION%
 */
interface CommunityManagementService : UserListingService {
    /**
     * Lists visible communities with user counts.
     *
     * @since %CURRENT_VERSION%
     */
    fun viewCommunities(): List<Pair<CommunityVo, Long>>

    /**
     * Edits a visible community.
     *
     * @since %CURRENT_VERSION%
     */
    fun editCommunity(communityId: CommunityId, communityName: String, description: String): CommunityVo

    /**
     * Reads both invite codes.
     *
     * @since %CURRENT_VERSION%
     */
    fun viewCommunityInvites(communityId: CommunityId): List<CommunityInviteVo>

    /**
     * Replaces an invite code.
     *
     * @since %CURRENT_VERSION%
     */
    fun createCommunityInvite(communityId: CommunityId, kind: CommunityInvite.Kind): CommunityInviteVo

    /**
     * Extends an invite code.
     *
     * @since %CURRENT_VERSION%
     */
    fun extendCommunityInvite(communityId: CommunityId, kind: CommunityInvite.Kind): CommunityInviteVo

    /**
     * Finds contests shared to the community.
     *
     * @since %CURRENT_VERSION%
     */
    fun viewCommunityContests(communityId: CommunityId, pagination: Pagination, name: String? = null): Page<ContestVo>

    /**
     * Creates an observer.
     *
     * @since %CURRENT_VERSION%
     */
    fun createObserver(communityId: CommunityId, observerName: String, contestIds: Set<ContestId>): ObserverVo
}
