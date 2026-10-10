package tech.testsys.web.app.service.multi

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.testsys.operation.error.OperationException
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.user.MultipleRoleUserOperations
import tech.testsys.web.app.service.CurrentUser
import tech.testsys.web.app.service.MultipleRoleUserVo
import tech.testsys.web.app.service.toVo

/**
 * Runs [MultipleRoleUserOperations] for the current user in one transaction per call.
 * A failed operation throws `OperationException`, which rolls the transaction back, except in [confirmEmailChange].
 *
 * @since %CURRENT_VERSION%
 */
@Service
@Transactional
class MultipleRoleUserService(private val operations: MultipleRoleUserOperations, private val currentUser: CurrentUser) {
    /**
     * Runs [MultipleRoleUserOperations.viewProfile] and adds the nickname and the e-mail address of the current user.
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewProfile(): ProfileVo {
        val user = currentUser.multipleRoleUser()
        val roles = operations.viewProfile(user).getOrThrow()
            .map { (role, communities) -> role.toVo() to communities.map { community -> community.toVo() } }
        return ProfileVo(name = user.data.name, email = user.data.email, roles = roles)
    }

    /**
     * Runs [MultipleRoleUserOperations.requestEmailChange].
     *
     * @since %CURRENT_VERSION%
     */
    fun requestEmailChange(email: String) {
        operations.requestEmailChange(currentUser.multipleRoleUser(), email).getOrThrow()
    }

    /**
     * Runs [MultipleRoleUserOperations.confirmEmailChange]. The transaction commits even if the operation fails, so that
     * a spent attempt to enter the code is kept.
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(noRollbackFor = [OperationException::class])
    fun confirmEmailChange(confirmationCode: String): MultipleRoleUserVo =
        operations.confirmEmailChange(currentUser.multipleRoleUser(), confirmationCode).getOrThrow().toVo()

    /**
     * Runs [MultipleRoleUserOperations.joinCommunity].
     *
     * @since %CURRENT_VERSION%
     */
    fun joinCommunity(inviteCode: String): MultipleRoleUserVo =
        operations.joinCommunity(currentUser.multipleRoleUser(), inviteCode).getOrThrow().toVo()
}
