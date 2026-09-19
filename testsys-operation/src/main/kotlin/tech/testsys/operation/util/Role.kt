package tech.testsys.operation.util

import tech.testsys.domain.model.user.CompatibleUserRole
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.operation.annotation.InternalOperationsApi

@InternalOperationsApi
inline fun <reified R : CompatibleUserRole> MultipleRoleUser.hasRole(): Boolean {
    return this.data.roles.filterIsInstance<R>().size == 1
}
