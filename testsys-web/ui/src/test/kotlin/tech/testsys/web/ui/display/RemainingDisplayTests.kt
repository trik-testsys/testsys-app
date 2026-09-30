package tech.testsys.web.ui.display

import com.vaadin.flow.component.html.NativeButton
import com.github.mvysny.kaributesting.v10._click
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.ui.ElementHandle
import tech.testsys.web.ui.feedback.SkeletonShape
import tech.testsys.web.ui.feedback.skeleton
import tech.testsys.web.ui.MockVaadinTests
import tech.testsys.web.ui.buildTestContent
import tech.testsys.web.ui.find
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class RemainingDisplayTests : MockVaadinTests() {
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

    @Test
    fun `should separate card and CTA callbacks`() {
        var cardClicks = 0
        var actionClicks = 0
        val root = buildTestContent {
            contestCard(
                ContestCardData(
                    "Title",
                    "Format",
                    "Status",
                    Tone.Info,
                    "Today",
                    actionLabel = "Join",
                ),
            ) {
                onClick { cardClicks++ }
                onAction { actionClicks++ }
            }
        }
        val button = requireNotNull(root.find("ts-btn") as? NativeButton)

        button._click()

        assertEquals(1, actionClicks)
        assertEquals(0, cardClicks)
    }

    @Test
    fun `should reject invalid progress and render indeterminate without numeric value`() {
        assertThrows(IllegalArgumentException::class.java) { ProgressValue.Determinate(Double.NaN) }
        val root = buildTestContent { progressBar("Progress", ProgressValue.Indeterminate) }

        assertFalse(root.find("ts-progress").element.hasAttribute("aria-valuenow"))
    }
    @Test
    fun `should retain positive skeleton dimensions in intrinsic horizontal flows`() {
        val placeholders = mutableListOf<ElementHandle>()
        buildTestContent {
            horizontal { SkeletonShape.entries.forEach { shape -> placeholders += skeleton(shape) } }
        }

        assertEquals(SkeletonShape.entries.size, placeholders.size)
        placeholders.forEach { handle ->
            assertTrue(handle.component.element.style.get("min-width").removeSuffix("px").toInt() > 0)
        }
    }

    @Test
    fun `should reject zero explicit skeleton dimensions`() {
        assertThrows(IllegalArgumentException::class.java) {
            buildTestContent { skeleton(SkeletonShape.Rectangle, width = 0) }
        }
    }

}
