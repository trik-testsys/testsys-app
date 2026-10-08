package tech.testsys.web.app.service

import org.springframework.stereotype.Component
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.SingleRoleUser

/**
 * User of the current request on whose behalf the services run operations.
 *
 * @since %CURRENT_VERSION%
 */
interface CurrentUser {
    /**
     * Returns the current user, who must be a [MultipleRoleUser].
     *
     * @since %CURRENT_VERSION%
     */
    fun multipleRoleUser(): MultipleRoleUser

    /**
     * Returns the current user, who must be a [SingleRoleUser].
     *
     * @since %CURRENT_VERSION%
     */
    fun singleRoleUser(): SingleRoleUser
}

/**
 * [CurrentUser] of an application without authentication: every call throws [IllegalStateException].
 *
 * @since %CURRENT_VERSION%
 */
@Component
class NoCurrentUser : CurrentUser {
    override fun multipleRoleUser(): MultipleRoleUser = error(NO_USER)

    override fun singleRoleUser(): SingleRoleUser = error(NO_USER)

    private companion object {
        const val NO_USER = "No authenticated user: security is not implemented"
    }
}
