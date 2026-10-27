package tech.testsys.web.app.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.env.MockEnvironment
import java.time.format.DateTimeParseException

class SignInLimitConfigurationTests {
    private val clock = MutableClock()

    @Test
    fun `should block on the configured number of failures`() {
        val limiter = loginAttemptLimiter(environment(failures = "2"))
        limiter.attempt("client") { false }

        val result = limiter.attempt("client") { false }

        assertEquals(LoginAttemptResult.BLOCKED, result)
    }

    @Test
    fun `should block for the configured duration`() {
        val limiter = loginAttemptLimiter(environment(failures = "1", duration = "PT1M"))
        assertEquals(LoginAttemptResult.BLOCKED, limiter.attempt("client") { false })
        clock.now = clock.now.plusSeconds(59)
        assertEquals(LoginAttemptResult.BLOCKED, limiter.attempt("client") { true })
        clock.now = clock.now.plusSeconds(1)

        val result = limiter.attempt("client") { true }

        assertEquals(LoginAttemptResult.SUCCESS, result)
    }

    @ParameterizedTest
    @ValueSource(strings = ["0", "-1"])
    fun `should reject failures that are not positive`(failures: String) {
        assertThrows(IllegalArgumentException::class.java) { loginAttemptLimiter(environment(failures = failures)) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["PT0S", "PT-1S", "PT0.5S", "PT2147483648S"])
    fun `should reject duration that is not a positive whole number of seconds within Int range`(duration: String) {
        assertThrows(IllegalArgumentException::class.java) { loginAttemptLimiter(environment(duration = duration)) }
    }

    @Test
    fun `should reject duration not in ISO-8601 format`() {
        assertThrows(DateTimeParseException::class.java) { loginAttemptLimiter(environment(duration = "5m")) }
    }

    private fun loginAttemptLimiter(environment: MockEnvironment): LoginAttemptLimiter =
        SignInLimitConfiguration().loginAttemptLimiter(environment, clock)

    private fun environment(failures: String = "5", duration: String = "PT5M") = MockEnvironment()
        .withProperty("testsys.web.sign-in-limit.failures", failures)
        .withProperty("testsys.web.sign-in-limit.duration", duration)
}
