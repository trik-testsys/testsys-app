package tech.testsys.web.service.security

import org.springframework.stereotype.Component
import tech.testsys.bootstrap.ServiceWebSettings

/**
 * Checks the configured code only after the shared IP limiter allows the attempt.
 *
 * @since %CURRENT_VERSION%
 */
@Component
class ServiceAuthenticator(private val settings: ServiceWebSettings, private val limiter: ServiceLoginAttemptLimiter) {
    /**
     * Authenticates [code] from the container-verified client [address] without retaining it in the result.
     *
     * @since %CURRENT_VERSION%
     */
    fun authenticate(address: String, code: String): ServiceSignInResult = limiter.attempt(address) { settings.matches(code) }
}
