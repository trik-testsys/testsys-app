package tech.testsys.domain.contract.persistence

import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId

/**
 * Read-only selection of every non-supervisor user for the Supervisor cabinet.
 *
 * @since %CURRENT_VERSION%
 */
interface SupervisorUserReadPort {

    /**
     * Synchronously finds users matching [filter] before paging and counting, ordered by [pagination] with id as a tie-breaker.
     * An unknown community matches nothing. Storage failures propagate.
     *
     * @param pagination requested page and order.
     * @param filter optional name, role and community conditions.
     * @return users of their concrete kinds, with an exact filtered total; an absent page is empty.
     * @since %CURRENT_VERSION%
     */
    fun findUsers(pagination: Pagination, filter: UserFilter = UserFilter()): Page<User<*>>

    /**
     * Synchronously reads a non-supervisor [userId], without changing it; storage failures propagate.
     *
     * @param userId identifier of the requested user.
     * @return its concrete kind or `null` when absent or a supervisor.
     * @since %CURRENT_VERSION%
     */
    fun findUserById(userId: UserId): User<*>?

    /**
     * Synchronously counts distinct non-supervisor users belonging to [communityId]. Storage failures propagate.
     *
     * @param communityId community to count, including its owner and observers.
     * @return exact count, or zero for an unknown community.
     * @since %CURRENT_VERSION%
     */
    fun countUsersInCommunity(communityId: CommunityId): Long
}
