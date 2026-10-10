package tech.testsys.web.app.service.user

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.model.user.RegistrationRequestId
import tech.testsys.domain.model.user.RegistrationRole
import tech.testsys.operation.error.OperationException
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.user.UserOperations
import tech.testsys.web.app.security.CabinetPrincipal

/**
 * Runs [UserOperations] before the user is known, in one transaction per call.
 * A failed operation throws `OperationException`, which rolls the transaction back, except in [confirmRegistration].
 *
 * @since %CURRENT_VERSION%
 */
@Service
@Transactional
class UserService(private val operations: UserOperations) {
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
    fun confirmRegistration(
        registrationRequestId: RegistrationRequestId,
        confirmationCode: String,
        name: String,
        role: RegistrationRole,
    ): String = operations.confirmRegistration(
        registrationRequestId = registrationRequestId,
        confirmationCode = confirmationCode,
        name = name,
        role = role,
    ).getOrThrow().second
}
