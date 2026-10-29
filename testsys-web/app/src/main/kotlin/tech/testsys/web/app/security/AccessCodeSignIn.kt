package tech.testsys.web.app.security

import com.vaadin.flow.server.VaadinServletRequest
import org.springframework.stereotype.Component
import tech.testsys.operation.error.AuthenticateError
import tech.testsys.operation.error.OperationException
import tech.testsys.web.app.service.user.UserService

/**
 * Signs a fixed-role user in by the access code through [UserService] outside its transaction, limiting failed
 * attempts per client address of the request with a [LoginAttemptLimiter].
 *
 * @since 1.0.0-beta.1
 */
@Component
class AccessCodeSignIn(private val userService: UserService, private val limiter: LoginAttemptLimiter) {
    /**
     * Outcome of [signIn], without the entered access code.
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
         * The access code is assigned to no fixed-role user.
         *
         * @since 1.0.0-beta.1
         */
        data object InvalidAccessCode : Result

        /**
         * Signing in by access code from this client address is temporarily closed, including the failure that
         * closes it.
         *
         * @since 1.0.0-beta.1
         */
        data object Blocked : Result
    }

    /**
     * Checks [accessToken] unless signing in by access code from the client address of the current Vaadin request
     * is closed and on success signs the user in with [CabinetSignIn.signIn].
     *
     * @since 1.0.0-beta.1
     */
    fun signIn(accessToken: String): Result {
        val address = checkNotNull(VaadinServletRequest.getCurrent()) {
            "Signing in by access code needs a Vaadin servlet request"
        }.remoteAddr
        var principal: CabinetPrincipal? = null
        val attempt = limiter.attempt(ADDRESS_KEY_PREFIX + address) {
            principal = authenticate(accessToken)
            principal != null
        }

        return when (attempt) {
            LoginAttemptResult.SUCCESS -> checkNotNull(principal).let { user -> Result.Success(user, CabinetSignIn.signIn(user)) }
            LoginAttemptResult.FAILURE -> Result.InvalidAccessCode
            LoginAttemptResult.BLOCKED -> Result.Blocked
        }
    }

    private fun authenticate(accessToken: String): CabinetPrincipal? = try {
        userService.authenticate(accessToken)
    } catch (failure: OperationException) {
        if (failure.error !is AuthenticateError) throw failure
        null
    }

    private companion object {
        // Keys of the limiter shared with signing in by e-mail address, where they are e-mail addresses.
        const val ADDRESS_KEY_PREFIX = "address:"
    }
}
