package tech.testsys.web.app.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.context.properties.bind.BindException
import org.springframework.mock.env.MockEnvironment

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
    fun `should block for a duration in Spring units`() {
        val limiter = loginAttemptLimiter(environment(failures = "1", duration = "1m"))
        assertEquals(LoginAttemptResult.BLOCKED, limiter.attempt("client") { false })
        clock.now = clock.now.plusSeconds(59)
        assertEquals(LoginAttemptResult.BLOCKED, limiter.attempt("client") { true })
        clock.now = clock.now.plusSeconds(1)

        val result = limiter.attempt("client") { true }

        assertEquals(LoginAttemptResult.SUCCESS, result)
    }

    @Test
    fun `should reject duration that is not a duration`() {
        assertThrows(BindException::class.java) { loginAttemptLimiter(environment(duration = "five minutes")) }
    }

    private fun loginAttemptLimiter(environment: MockEnvironment): LoginAttemptLimiter =
        SignInLimitConfiguration().loginAttemptLimiter(environment, clock)

    private fun environment(failures: String = "5", duration: String = "PT5M") = MockEnvironment()
        .withProperty("testsys.web.sign-in-limit.failures", failures)
        .withProperty("testsys.web.sign-in-limit.duration", duration)
}
