package tech.testsys.operation.util

import tech.testsys.domain.model.user.CompatibleUserRole
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.operation.annotation.InternalOperationsApi

/**
 * Checks whether this user holds the role [R].
 *
 * @param R the role to look for.
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
inline fun <reified R : CompatibleUserRole> MultipleRoleUser.hasRole(): Boolean {
    return this.data.roles.filterIsInstance<R>().size == 1
}
