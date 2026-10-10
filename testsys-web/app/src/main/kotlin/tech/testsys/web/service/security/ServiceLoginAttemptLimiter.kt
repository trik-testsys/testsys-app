package tech.testsys.web.service.security

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Serializes attempts across sessions by client address and expires in-memory failures after five minutes.
 * Success preserves recent failures; a blocked attempt never invokes authentication or extends its block.
 *
 * @since %CURRENT_VERSION%
 */
@Component
class ServiceLoginAttemptLimiter(private val clock: Clock) {
    private val attempts = mutableMapOf<String, Attempts>()

    /**
     * Checks [authenticate] for [address] atomically with failure accounting; the fifth failure blocks immediately.
     *
     * @since %CURRENT_VERSION%
     */
    @Synchronized
    fun attempt(address: String, authenticate: () -> Boolean): ServiceSignInResult {
        val now = clock.instant()
        attempts.entries.removeIf { (_, state) -> state.expired(now) }
        val state = attempts.getOrPut(address) { Attempts() }
        if (state.blockedUntil?.isAfter(now) == true) return ServiceSignInResult.Blocked

        state.failures.removeIf { failure -> !failure.isAfter(now.minus(WINDOW)) }
        if (authenticate()) {
            if (state.failures.isEmpty()) attempts.remove(address)
            return ServiceSignInResult.Success
        }

        state.failures.add(now)
        return if (state.failures.size >= MAXIMUM_FAILURES) {
            state.blockedUntil = now.plus(WINDOW)
            ServiceSignInResult.Blocked
        } else {
            ServiceSignInResult.InvalidCode
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
