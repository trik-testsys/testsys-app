package tech.testsys.domain.contract.persistence

import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId

/**
 * Read-only selection of every user without the supervisor role for the Supervisor cabinet.
 *
 * @since 1.0.0-beta.1
 */
interface SupervisorUserReadPort {

    /**
     * Synchronously finds users matching [filter] before paging and counting, ordered by [pagination] with id as a tie-breaker.
     * An unknown community matches nothing. Storage failures propagate.
     *
     * @param pagination requested page and order.
     * @param filter optional name, role and community conditions.
     * @return users of their concrete kinds, with an exact filtered total; an absent page is empty.
     * @since 1.0.0-beta.1
     */
    fun findUsers(pagination: Pagination, filter: UserFilter = UserFilter()): Page<User<*>>

    /**
     * Synchronously reads [userId] without the supervisor role, without changing it; storage failures propagate.
     *
     * @param userId identifier of the requested user.
     * @return its concrete kind or `null` when absent or holding the supervisor role.
     * @since 1.0.0-beta.1
     */
    fun findUserById(userId: UserId): User<*>?

    /**
     * Synchronously counts distinct users without the supervisor role belonging to [communityId]. Storage failures propagate.
     *
     * @param communityId community to count, including its owner and observers.
     * @return exact count, or zero for an unknown community.
     * @since 1.0.0-beta.1
     */
    fun countUsersInCommunity(communityId: CommunityId): Long
}
