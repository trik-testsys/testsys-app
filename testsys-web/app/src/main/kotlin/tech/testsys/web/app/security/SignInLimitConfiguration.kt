package tech.testsys.web.app.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.time.Clock
import java.time.Duration

/**
 * Configures the [LoginAttemptLimiter] shared by the sign-in ways of the main interface.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration(proxyBeanMethods = false)
class SignInLimitConfiguration {
    /**
     * Limiter closing a key for `testsys.web.sign-in-limit.duration` once its failures within that time reach
     * `testsys.web.sign-in-limit.failures`.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun loginAttemptLimiter(environment: Environment, clock: Clock): LoginAttemptLimiter {
        val failures = environment.getRequiredProperty("$PREFIX.failures", Int::class.java)
        require(failures > 0) { "$PREFIX.failures must be positive, got $failures" }

        val rawDuration = environment.getRequiredProperty("$PREFIX.duration")
        val duration = Duration.parse(rawDuration)
        require(duration.isPositive && duration.nano == 0 && duration.seconds <= Int.MAX_VALUE) {
            "$PREFIX.duration must be a positive whole number of seconds up to ${Int.MAX_VALUE}, got $rawDuration"
        }

        return LoginAttemptLimiter(clock = clock, maximumFailures = failures, window = duration)
    }

    private companion object {
        const val PREFIX = "testsys.web.sign-in-limit"
    }
}
