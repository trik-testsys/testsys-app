package tech.testsys.web.app.service.multi

import com.vaadin.flow.server.VaadinServletRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.testsys.operation.error.OperationException
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.user.MultipleRoleUserOperations
import tech.testsys.web.app.security.CabinetSessions
import tech.testsys.web.app.security.CabinetSignIn
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
class MultipleRoleUserService(
    private val operations: MultipleRoleUserOperations,
    private val currentUser: CurrentUser,
    private val sessions: CabinetSessions,
) {
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
     * Runs [MultipleRoleUserOperations.requestEmailChange]; throws `IllegalStateException` while a supervisor is signed in
     * as the user.
     *
     * @since %CURRENT_VERSION%
     */
    fun requestEmailChange(email: String) {
        checkNotImpersonated()
        operations.requestEmailChange(currentUser.multipleRoleUser(), email).getOrThrow()
    }

    /**
     * Runs [MultipleRoleUserOperations.confirmEmailChange]; throws `IllegalStateException` while a supervisor is signed in
     * as the user. The transaction commits even if the operation fails, so that a spent attempt to enter the code is kept.
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(noRollbackFor = [OperationException::class])
    fun confirmEmailChange(confirmationCode: String): MultipleRoleUserVo {
        checkNotImpersonated()
        return operations.confirmEmailChange(currentUser.multipleRoleUser(), confirmationCode).getOrThrow().toVo()
    }

    /**
     * Runs [MultipleRoleUserOperations.changePassword] and ends the other sessions of the user, keeping the current one;
     * throws `IllegalStateException` while a supervisor is signed in as the user.
     *
     * @since %CURRENT_VERSION%
     */
    fun changePassword(currentPassword: String, newPassword: String) {
        checkNotImpersonated()
        val user = operations.changePassword(currentUser.multipleRoleUser(), currentPassword = currentPassword, newPassword = newPassword)
            .getOrThrow()
        sessions.expire(user.id.value, except = VaadinServletRequest.getCurrent()?.httpServletRequest?.getSession(false)?.id)
    }

    /**
     * Runs [MultipleRoleUserOperations.joinCommunity].
     *
     * @since %CURRENT_VERSION%
     */
    fun joinCommunity(inviteCode: String): MultipleRoleUserVo =
        operations.joinCommunity(currentUser.multipleRoleUser(), inviteCode).getOrThrow().toVo()

    private fun checkNotImpersonated() {
        val supervisorId = CabinetSignIn.principal()?.impersonatedBy
        check(supervisorId == null) { "Supervisor $supervisorId signed in as the user cannot change their e-mail address or password" }
    }
}
