package tech.testsys.web.app.security

import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Outcome of a sign-in attempt checked by [LoginAttemptLimiter].
 *
 * @since %CURRENT_VERSION%
 */
enum class LoginAttemptResult {

    SUCCESS,
    FAILURE,
    BLOCKED,
}

/**
 * Serializes sign-in attempts by a string key and blocks the key for five minutes on its fifth failure within five
 * minutes; a blocked attempt never authenticates or extends the block, and the state lives in instance memory.
 *
 * @since %CURRENT_VERSION%
 */
class LoginAttemptLimiter(private val clock: Clock) {
    private val attempts = mutableMapOf<String, Attempts>()

    /**
     * Checks [authenticate] for [key] atomically with failure accounting.
     *
     * @since %CURRENT_VERSION%
     */
    @Synchronized
    fun attempt(key: String, authenticate: () -> Boolean): LoginAttemptResult {
        val now = clock.instant()
        attempts.entries.removeIf { (_, state) -> state.expired(now) }
        val state = attempts.getOrPut(key) { Attempts() }
        if (state.blockedUntil?.isAfter(now) == true) return LoginAttemptResult.BLOCKED

        state.failures.removeIf { failure -> !failure.isAfter(now.minus(WINDOW)) }
        if (authenticate()) {
            if (state.failures.isEmpty()) attempts.remove(key)
            return LoginAttemptResult.SUCCESS
        }

        state.failures.add(now)
        return if (state.failures.size >= MAXIMUM_FAILURES) {
            state.blockedUntil = now.plus(WINDOW)
            LoginAttemptResult.BLOCKED
        } else {
            LoginAttemptResult.FAILURE
        }
    }

    private class Attempts {
        val failures = mutableListOf<Instant>()
        var blockedUntil: Instant? = null

        fun expired(now: Instant): Boolean = blockedUntil?.let { until -> !until.isAfter(now) }
            ?: failures.none { failure -> failure.isAfter(now.minus(WINDOW)) }
    }

    private companion object {
        const val MAXIMUM_FAILURES = 5
        val WINDOW: Duration = Duration.ofMinutes(5)
    }
}
