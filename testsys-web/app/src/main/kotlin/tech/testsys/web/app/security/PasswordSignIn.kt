package tech.testsys.web.app.security

import org.springframework.stereotype.Component
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.AuthenticateByPasswordError
import tech.testsys.operation.error.OperationException
import tech.testsys.operation.util.normalizeEmail
import tech.testsys.web.app.service.user.UserService

/**
 * Signs a user with non-fixed roles in by the e-mail address and the password through [UserService] outside its
 * transaction, limiting failed attempts per normalized e-mail address with a [LoginAttemptLimiter].
 *
 * @since 1.0.0-beta.1
 */
@Component
@OptIn(InternalOperationsApi::class)
class PasswordSignIn(private val userService: UserService, private val limiter: LoginAttemptLimiter) {
    /**
     * Outcome of [signIn], without the entered password.
     *
     * @since 1.0.0-beta.1
     */
    sealed interface Result {
        /**
         * The user is signed in.
         *
         * @property principal the signed-in user.
         * @property returnAddress the page saved before signing in and open to the user, or `null`.
         * @since 1.0.0-beta.1
         */
        data class Success(val principal: CabinetPrincipal, val returnAddress: String?) : Result

        /**
         * The e-mail address or the password is wrong.
         *
         * @since 1.0.0-beta.1
         */
        data object InvalidCredentials : Result

        /**
         * Signing in by this e-mail address is temporarily closed, including the failure that closes it.
         *
         * @since 1.0.0-beta.1
         */
        data object Blocked : Result
    }

    /**
     * Checks [email] and [password] unless signing in by [email] is closed and on success signs the user in with
     * [CabinetSignIn.signIn].
     *
     * @since 1.0.0-beta.1
     */
    fun signIn(email: String, password: String): Result {
        var principal: CabinetPrincipal? = null
        val attempt = limiter.attempt(EMAIL_KEY_PREFIX + normalizeEmail(email)) {
            principal = authenticate(email, password)
            principal != null
        }

        return when (attempt) {
            LoginAttemptResult.SUCCESS -> checkNotNull(principal).let { user -> Result.Success(user, CabinetSignIn.signIn(user)) }
            LoginAttemptResult.FAILURE -> Result.InvalidCredentials
            LoginAttemptResult.BLOCKED -> Result.Blocked
        }
    }

    private fun authenticate(email: String, password: String): CabinetPrincipal? = try {
        userService.authenticateByPassword(email, password)
    } catch (failure: OperationException) {
        if (failure.error !is AuthenticateByPasswordError) throw failure
        null
    }

    private companion object {
        // Keys of the limiter shared with signing in by access code, where they are client addresses.
        const val EMAIL_KEY_PREFIX = "email:"
    }
}
