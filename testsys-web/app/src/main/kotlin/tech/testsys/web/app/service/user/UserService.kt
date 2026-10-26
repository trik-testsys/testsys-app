package tech.testsys.web.app.service.user

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.model.user.RegistrationRequestId
import tech.testsys.operation.error.OperationException
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.user.MultipleRoleUserOperations
import tech.testsys.operation.user.UserOperations
import tech.testsys.web.app.security.CabinetPrincipal

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
) {
    /**
     * Runs [UserOperations.authenticate] and returns the principal of the found user instead of the user data.
     *
     * @since %CURRENT_VERSION%
     */
    fun authenticate(accessToken: String): CabinetPrincipal = CabinetPrincipal.of(operations.authenticate(accessToken).getOrThrow())

    /**
     * Runs [UserOperations.requestRegistration].
     *
     * @since %CURRENT_VERSION%
     */
    fun requestRegistration(email: String): RegistrationRequestId = operations.requestRegistration(email).getOrThrow()

    /**
     * Runs [UserOperations.confirmRegistration] and returns the original access code of the new user. The transaction
     * commits even if the operation fails, so that a spent attempt to enter the code is kept.
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(noRollbackFor = [OperationException::class])
    fun confirmRegistration(registrationRequestId: RegistrationRequestId, confirmationCode: String, name: String): String =
        operations.confirmRegistration(registrationRequestId, confirmationCode = confirmationCode, name = name).getOrThrow().second

    /**
     * Runs [MultipleRoleUserOperations.requestAccessRestoration].
     *
     * @since %CURRENT_VERSION%
     */
    fun requestAccessRestoration(email: String) {
        multipleRoleUserOperations.requestAccessRestoration(email).getOrThrow()
    }

    /**
     * Runs [MultipleRoleUserOperations.confirmAccessRestoration] and returns the new access code. The transaction
     * commits even if the operation fails, so that a spent attempt to enter the code is kept.
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(noRollbackFor = [OperationException::class])
    fun confirmAccessRestoration(email: String, confirmationCode: String): String =
        multipleRoleUserOperations.confirmAccessRestoration(email = email, confirmationCode = confirmationCode).getOrThrow()
}
