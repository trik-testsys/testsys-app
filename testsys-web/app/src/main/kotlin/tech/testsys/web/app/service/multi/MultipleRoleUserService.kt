package tech.testsys.web.app.service.multi

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.user.MultipleRoleUserOperations
import tech.testsys.web.app.service.CommunityVo
import tech.testsys.web.app.service.CurrentUser
import tech.testsys.web.app.service.MultipleRoleUserVo
import tech.testsys.web.app.service.RoleVo
import tech.testsys.web.app.service.toVo

/**
 * Runs [MultipleRoleUserOperations] for the current user in one transaction per call.
 * A failed operation throws `OperationException`, which rolls the transaction back.
 *
 * @since %CURRENT_VERSION%
 */
@Service
@Transactional
class MultipleRoleUserService(private val operations: MultipleRoleUserOperations, private val currentUser: CurrentUser) {
    /**
     * Runs [MultipleRoleUserOperations.viewProfile].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewProfile(): List<Pair<RoleVo, List<CommunityVo>>> = operations.viewProfile(currentUser.multipleRoleUser()).getOrThrow()
        .map { (role, communities) -> role.toVo() to communities.map { community -> community.toVo() } }

    /**
     * Runs [MultipleRoleUserOperations.joinCommunity].
     *
     * @since %CURRENT_VERSION%
     */
    fun joinCommunity(inviteCode: String): MultipleRoleUserVo =
        operations.joinCommunity(currentUser.multipleRoleUser(), inviteCode).getOrThrow().toVo()
}
