package tech.testsys.web.app.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class LoginAttemptLimiterTests {
    private val clock = MutableClock()
    private val limiter = LoginAttemptLimiter(clock)

    @Test
    fun `should block the fifth failure and reject even a correct code without extending the block`() {
        repeat(4) { assertEquals(LoginAttemptResult.FAILURE, limiter.attempt("client") { false }) }
        assertEquals(LoginAttemptResult.BLOCKED, limiter.attempt("client") { false })
        clock.now = clock.now.plusSeconds(299)
        var wasChecked = false

        val result = limiter.attempt("client") {
            wasChecked = true
            true
        }
        assertEquals(LoginAttemptResult.BLOCKED, result)
        assertFalse(wasChecked)
        clock.now = clock.now.plusSeconds(1)
        assertEquals(LoginAttemptResult.SUCCESS, limiter.attempt("client") { true })
    }

    @Test
    fun `should preserve failures after successful authentication`() {
        repeat(4) { limiter.attempt("client") { false } }

        assertEquals(LoginAttemptResult.SUCCESS, limiter.attempt("client") { true })
        assertEquals(LoginAttemptResult.BLOCKED, limiter.attempt("client") { false })
    }

    @Test
    fun `should drop failures exactly five minutes old`() {
        repeat(4) { limiter.attempt("client") { false } }
        clock.now = clock.now.plusSeconds(300)

        val result = limiter.attempt("client") { false }

        assertEquals(LoginAttemptResult.FAILURE, result)
    }

    @Test
    fun `should authenticate another address while the first address is blocked`() {
        repeat(5) { limiter.attempt("client") { false } }

        val result = limiter.attempt("another") { true }

        assertEquals(LoginAttemptResult.SUCCESS, result)
    }

    @Test
    fun `should drop the oldest failure when only it is five minutes old`() {
        limiter.attempt("client") { false }
        clock.now = clock.now.plusSeconds(299)
        repeat(3) { limiter.attempt("client") { false } }
        clock.now = clock.now.plusSeconds(1)

        val result = limiter.attempt("client") { false }

        assertEquals(LoginAttemptResult.FAILURE, result)
    }

    @Test
    fun `should preserve recent failures after the oldest failure expires`() {
        limiter.attempt("client") { false }
        clock.now = clock.now.plusSeconds(299)
        repeat(3) { limiter.attempt("client") { false } }
        clock.now = clock.now.plusSeconds(1)
        limiter.attempt("client") { false }
        clock.now = clock.now.plusSeconds(1)

        val result = limiter.attempt("client") { false }

        assertEquals(LoginAttemptResult.BLOCKED, result)
    }

    @Test
    fun `should count concurrent failures atomically`() {
        val results = Executors.newFixedThreadPool(10).use { executor ->
            executor.invokeAll(List(20) { Callable { limiter.attempt("client") { false } } }).map { it.get() }
        }

        assertEquals(4, results.count { it == LoginAttemptResult.FAILURE })
        assertEquals(16, results.count { it == LoginAttemptResult.BLOCKED })
    }

    private class MutableClock : Clock() {
        var now: Instant = Instant.parse("2026-01-01T00:00:00Z")

        override fun instant(): Instant = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
    }
}
