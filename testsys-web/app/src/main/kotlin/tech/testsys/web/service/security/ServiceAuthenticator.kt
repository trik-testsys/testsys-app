package tech.testsys.web.service.security

import org.springframework.stereotype.Component
import tech.testsys.bootstrap.ServiceWebSettings
import tech.testsys.web.app.security.LoginAttemptLimiter
import tech.testsys.web.app.security.LoginAttemptResult
import java.time.Clock

/**
 * Checks the configured code only after the limiter of client addresses allows the attempt.
 *
 * @since %CURRENT_VERSION%
 */
@Component
class ServiceAuthenticator(private val settings: ServiceWebSettings, clock: Clock) {
    private val limiter = LoginAttemptLimiter(clock)

    /**
     * Authenticates [code] from the container-verified client [address] without retaining it in the result.
     *
     * @since %CURRENT_VERSION%
     */
    fun authenticate(address: String, code: String): ServiceSignInResult = when (limiter.attempt(address) { settings.matches(code) }) {
        LoginAttemptResult.SUCCESS -> ServiceSignInResult.Success
        LoginAttemptResult.FAILURE -> ServiceSignInResult.InvalidCode
        LoginAttemptResult.BLOCKED -> ServiceSignInResult.Blocked
    }
}
