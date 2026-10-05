package tech.testsys.web.components.display

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

internal class TimerTests : MockVaadinTests() {
    private val originalSchedule = TimerRuntime.schedule
    private val originalClock = TimerRuntime.clock

    @AfterEach
    fun restoreTime() {
        TimerRuntime.schedule = originalSchedule
        TimerRuntime.clock = originalClock
    }

    @Test
    fun `should update dynamic timer and ignore old tick after detach`() {
        val now = Instant.parse("2026-09-30T00:00:00Z")
        TimerRuntime.clock = Clock.fixed(now, ZoneOffset.UTC)
        var tick: () -> Unit = {}
        var closes = 0
        TimerRuntime.schedule = { action ->
            tick = action
            AutoCloseable { closes++ }
        }
        lateinit var handle: TimerHandle
        buildTestContent { handle = timer("Deadline", TimerValue.Until(now.plusSeconds(2))) }
        val oldTick = tick

        TimerRuntime.clock = Clock.fixed(now.plusSeconds(3), ZoneOffset.UTC)
        tick()

        assertEquals(0L, handle.remainingSeconds.peek())
        assertTrue(closes > 0)
        handle.component.element.removeFromParent()
        handle.data = TimerValue.Static(Duration.ofSeconds(12))
        oldTick()
        assertEquals(12L, handle.remainingSeconds.peek())
    }

    @Test
    fun `should never schedule static timer and round negative durations to zero`() {
        var schedules = 0
        TimerRuntime.schedule = {
            schedules++
            AutoCloseable {}
        }
        lateinit var handle: TimerHandle

        buildTestContent { handle = timer("Static", TimerValue.Static(Duration.ofSeconds(-1))) }

        assertEquals(0, schedules)
        assertEquals(0L, handle.remainingSeconds.peek())
    }
}
