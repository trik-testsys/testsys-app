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
 * Serializes sign-in attempts by a string key and blocks the key for [window] on the failure that reaches
 * [maximumFailures] within [window]; a blocked attempt never authenticates or extends the block, and the state lives
 * in instance memory. Keys are spread over stripes: attempts of keys in different stripes run in parallel.
 *
 * @since %CURRENT_VERSION%
 */
class LoginAttemptLimiter(
    private val clock: Clock,
    private val maximumFailures: Int = MAXIMUM_FAILURES,
    private val window: Duration = WINDOW,
) {
    // ponytail: keys of one stripe wait for each other; more stripes if sign-in bursts still queue up.
    private val stripes = Array(STRIPES) { mutableMapOf<String, Attempts>() }

    /**
     * Checks [authenticate] for [key] atomically with failure accounting.
     *
     * @since %CURRENT_VERSION%
     */
    fun attempt(key: String, authenticate: () -> Boolean): LoginAttemptResult {
        val attempts = stripes[key.hashCode().mod(STRIPES)]
        synchronized(attempts) {
            val now = clock.instant()
            attempts.entries.removeIf { (_, state) -> state.expired(now, window) }
            val state = attempts.getOrPut(key) { Attempts() }
            if (state.blockedUntil?.isAfter(now) == true) return LoginAttemptResult.BLOCKED

            state.failures.removeIf { failure -> !failure.isAfter(now.minus(window)) }
            if (authenticate()) {
                if (state.failures.isEmpty()) attempts.remove(key)
                return LoginAttemptResult.SUCCESS
            }

            state.failures.add(now)
            return if (state.failures.size >= maximumFailures) {
                state.blockedUntil = now.plus(window)
                LoginAttemptResult.BLOCKED
            } else {
                LoginAttemptResult.FAILURE
            }
        }
    }

    private class Attempts {
        val failures = mutableListOf<Instant>()
        var blockedUntil: Instant? = null

        fun expired(now: Instant, window: Duration): Boolean = blockedUntil?.let { until -> !until.isAfter(now) }
            ?: failures.none { failure -> failure.isAfter(now.minus(window)) }
    }

    private companion object {
        const val MAXIMUM_FAILURES = 5
        const val STRIPES = 64
        val WINDOW: Duration = Duration.ofMinutes(5)
    }
}
