package tech.testsys.web.components.display

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.findAll
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

internal class TimerTests : MockVaadinTests() {
    private val originalSchedule = TimerRuntime.schedule
    private val originalClock = TimerRuntime.clock
    private val now = Instant.parse("2026-09-30T00:00:00Z")
    private var tick: () -> Unit = {}
    private var schedules = 0
    private var closes = 0

    @BeforeEach
    fun controlTime() {
        TimerRuntime.clock = Clock.fixed(now, ZoneOffset.UTC)
        TimerRuntime.schedule = { action ->
            schedules++
            tick = action
            AutoCloseable { closes++ }
        }
    }

    @AfterEach
    fun restoreTime() {
        TimerRuntime.schedule = originalSchedule
        TimerRuntime.clock = originalClock
    }

    @Test
    fun `should count a live timer down on a tick`() {
        val handle = buildTimer(TimerValue.Until(now.plusSeconds(10)))
        moveClock(seconds = 3)

        tick()

        assertEquals(7L, handle.remainingSeconds.peek())
    }

    @Test
    fun `should stop the schedule when a live timer reaches zero`() {
        val handle = buildTimer(TimerValue.Until(now.plusSeconds(2)))
        moveClock(seconds = 3)

        tick()

        assertEquals(0L, handle.remainingSeconds.peek())
        assertEquals(1, closes)
    }

    @Test
    fun `should ignore an old tick after detach`() {
        val handle = buildTimer(TimerValue.Until(now.plusSeconds(10)))
        val oldTick = tick
        handle.component.element.removeFromParent()
        moveClock(seconds = 3)

        oldTick()

        assertEquals(10L, handle.remainingSeconds.peek())
    }

    @Test
    fun `should recount a live timer when attached again`() {
        val handle = buildTimer(TimerValue.Until(now.plusSeconds(10)))
        val parent = handle.component.element.parent
        handle.component.element.removeFromParent()
        moveClock(seconds = 4)

        parent.appendChild(handle.component.element)

        assertEquals(6L, handle.remainingSeconds.peek())
        assertEquals(2, schedules)
    }

    @Test
    fun `should never schedule a static timer`() {
        buildTimer(TimerValue.Static(Duration.ofSeconds(30)))

        assertEquals(0, schedules)
    }

    @Test
    fun `should round a negative static duration to zero`() {
        val handle = buildTimer(TimerValue.Static(Duration.ofSeconds(-1)))

        assertEquals(0L, handle.remainingSeconds.peek())
    }

    @Test
    fun `should show a chip with a clock and hours above the danger threshold`() {
        val timer = buildTimer(TimerValue.Static(Duration.ofSeconds(3725)), TimerVariant.Chip).component

        assertTrue("ts-timer--chip" in timer.element.classList)
        assertFalse("ts-timer--danger" in timer.element.classList)
        assertEquals("01:02:05", timer.element.textRecursively)
        assertEquals(2, timer.element.childCount)
    }

    @Test
    fun `should show a chip in danger with minutes only below the threshold`() {
        val timer = buildTimer(TimerValue.Static(Duration.ofSeconds(125)), TimerVariant.Chip, dangerBelow = Duration.ofMinutes(5)).component

        assertTrue("ts-timer--danger" in timer.element.classList)
        assertEquals("02:05", timer.element.textRecursively)
        assertEquals(1, timer.element.childCount)
    }

    @Test
    fun `should show plain text without the chip and danger marks`() {
        val timer = buildTimer(TimerValue.Static(Duration.ofSeconds(125)), TimerVariant.Text).component

        assertEquals(setOf("ts-timer"), timer.element.classList.toSet())
        assertEquals("00:02:05", timer.element.textRecursively)
    }

    @Test
    fun `should show hours, minutes and seconds as hero parts`() {
        val timer = buildTimer(TimerValue.Static(Duration.ofSeconds(3725)), TimerVariant.Hero).component

        assertTrue("ts-timer__parts" in timer.element.classList)
        assertEquals(listOf("01", "02", "05"), timer.findAll("ts-timer__num").map { number -> number.element.text })
    }

    @Test
    fun `should show days, hours, minutes and seconds as tiles`() {
        val timer = buildTimer(TimerValue.Static(Duration.ofSeconds(90_125)), TimerVariant.Tiles).component

        assertTrue("ts-timer__tiles" in timer.element.classList)
        val tiles = timer.findAll("ts-timer__tile")
        assertEquals(listOf("01", "01", "02", "05"), tiles.map { tile -> tile.element.getChild(0).getProperty("innerHTML") })
        assertEquals(listOf("дни", "часы", "минуты", "секунды"), timer.findAll("ts-timer__unit").map { unit -> unit.element.text })
    }

    @Test
    fun `should reject a negative danger threshold`() {
        assertThrows(IllegalArgumentException::class.java) {
            buildTimer(TimerValue.Static(Duration.ofSeconds(1)), dangerBelow = Duration.ofSeconds(-1))
        }
    }

    private fun buildTimer(
        value: TimerValue,
        variant: TimerVariant = TimerVariant.Chip,
        dangerBelow: Duration = Duration.ofMinutes(10),
    ): TimerHandle {
        lateinit var handle: TimerHandle
        buildTestContent { handle = timer("Deadline", value, variant = variant, dangerBelow = dangerBelow) }
        return handle
    }

    private fun moveClock(seconds: Long) {
        TimerRuntime.clock = Clock.fixed(now.plusSeconds(seconds), ZoneOffset.UTC)
    }
}
