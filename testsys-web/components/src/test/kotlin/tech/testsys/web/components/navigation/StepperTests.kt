package tech.testsys.web.components.navigation

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.NativeButton
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.SelectionHandle
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.findAll

class StepperTests : MockVaadinTests() {
    private val steps = listOf(StepData("First"), StepData("Second"), StepData("Locked", isSelectable = false))
    private var choices = 0

    @Test
    fun `should mark the steps before the current one as done`() {
        val root = buildStepper()

        assertTrue("ts-step--done" in buttons(root)[0].element.classList)
    }

    @Test
    fun `should name the current step and mark it current`() {
        val root = buildStepper()

        assertEquals("step", buttons(root)[1].element.getAttribute("aria-current"))
        assertEquals("Second", buttons(root)[1].element.getAttribute("aria-label"))
    }

    @Test
    fun `should disable a step the application marks unavailable`() {
        val root = buildStepper()

        assertFalse(buttons(root)[2].isEnabled)
    }

    @Test
    fun `should reject a click on an unavailable step without a callback`() {
        val root = buildStepper()

        assertThrows(IllegalStateException::class.java) { buttons(root)[2]._click() }

        assertEquals(0, choices)
    }

    @Test
    fun `should choose a clicked step and report it`() {
        lateinit var handle: SelectionHandle<StepperData>
        val root = buildStepper { handle = this }

        buttons(root)[0]._click()

        assertEquals(0, handle.data.current)
        assertEquals(1, choices)
    }

    @Test
    fun `should show programmatic data without a callback`() {
        lateinit var handle: SelectionHandle<StepperData>
        val root = buildStepper { handle = this }

        handle.data = StepperData(steps, 2)

        assertEquals("step", root.findAll("ts-step")[2].element.getAttribute("aria-current"))
        assertEquals(0, choices)
    }

    @ParameterizedTest
    @ValueSource(ints = [-1, 3])
    fun `should reject a current index outside the steps`(current: Int) {
        assertThrows(IllegalArgumentException::class.java) { StepperData(steps, current) }
    }

    private fun buildStepper(configure: SelectionHandle<StepperData>.() -> Unit = {}): Component = buildTestContent {
        stepper(StepperData(steps, 1)) {
            onChange { choices++ }
            configure()
        }
    }

    private fun buttons(root: Component): List<NativeButton> = root.findAll("ts-step").filterIsInstance<NativeButton>()
}
