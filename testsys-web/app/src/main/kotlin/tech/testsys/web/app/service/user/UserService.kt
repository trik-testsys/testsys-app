package tech.testsys.web.app.service.user

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.model.user.RegistrationRequestId
import tech.testsys.operation.error.OperationException
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.user.MultipleRoleUserOperations
import tech.testsys.operation.user.UserOperations
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.CabinetSessions
import tech.testsys.web.app.security.CabinetSignIn

/**
 * Runs [UserOperations] and the [MultipleRoleUserOperations] of access restoration before the user is known, in one
 * transaction per call. A failed operation throws `OperationException`, which rolls the transaction back, except in
 * [confirmRegistration] and [confirmAccessRestoration].
 *
 * @since %CURRENT_VERSION%
 */
@Service
@Transactional
class UserService(
    private val operations: UserOperations,
    private val multipleRoleUserOperations: MultipleRoleUserOperations,
    private val sessions: CabinetSessions,
) {
    /**
     * Runs [UserOperations.authenticate] and returns the principal of the found user instead of the user data.
     *
     * @since %CURRENT_VERSION%
     */
    fun authenticate(accessToken: String): CabinetPrincipal = CabinetPrincipal.of(operations.authenticate(accessToken).getOrThrow())

    /**
     * Runs [UserOperations.authenticateByPassword] and returns the principal of the found user instead of the user data.
     *
     * @since %CURRENT_VERSION%
     */
    fun authenticateByPassword(email: String, password: String): CabinetPrincipal =
        CabinetPrincipal.of(operations.authenticateByPassword(email = email, password = password).getOrThrow())

    /**
     * Runs [UserOperations.requestRegistration].
     *
     * @since %CURRENT_VERSION%
     */
    fun requestRegistration(email: String): RegistrationRequestId = operations.requestRegistration(email).getOrThrow()

    /**
     * Runs [UserOperations.confirmRegistration]. The transaction commits even if the operation fails, so that a spent
     * attempt to enter the code is kept.
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(noRollbackFor = [OperationException::class])
    fun confirmRegistration(registrationRequestId: RegistrationRequestId, confirmationCode: String, name: String, password: String) {
        operations.confirmRegistration(registrationRequestId, confirmationCode = confirmationCode, name = name, password = password)
            .getOrThrow()
    }

    /**
     * Runs [MultipleRoleUserOperations.requestAccessRestoration]; throws `IllegalStateException` while a supervisor is
     * signed in as some user.
     *
     * @since %CURRENT_VERSION%
     */
    fun requestAccessRestoration(email: String) {
        checkNotImpersonated()
        multipleRoleUserOperations.requestAccessRestoration(email).getOrThrow()
    }

    /**
     * Runs [MultipleRoleUserOperations.confirmAccessRestoration] and ends every session of the user; throws
     * `IllegalStateException` while a supervisor is signed in as some user. The transaction commits even if the
     * operation fails, so that a spent attempt to enter the code is kept.
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(noRollbackFor = [OperationException::class])
    fun confirmAccessRestoration(email: String, confirmationCode: String, password: String) {
        checkNotImpersonated()
        val user = multipleRoleUserOperations
            .confirmAccessRestoration(email = email, confirmationCode = confirmationCode, password = password)
            .getOrThrow()
        sessions.expire(user.id.value)
    }

    private fun checkNotImpersonated() {
        val supervisorId = CabinetSignIn.principal()?.impersonatedBy
        check(supervisorId == null) { "Supervisor $supervisorId signed in as a user cannot restore access" }
    }
}
