package tech.testsys.web.app.service

import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.UserFilter
import java.time.Instant

/**
 * Reads users for a cabinet list with the actor's access rules.
 *
 * @since 1.0.0-beta.1
 */
interface UserListingService {
    /**
     * Returns a filtered page with last login times.
     *
     * @since 1.0.0-beta.1
     */
    fun viewUsers(pagination: Pagination, filter: UserFilter = UserFilter()): Page<Pair<AdminUserVo, Instant?>>
}
