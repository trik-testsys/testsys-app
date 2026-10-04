package tech.testsys.web.components.navigation

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.html.NativeButton
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.SelectionHandle
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.findAll

class StepperTests : MockVaadinTests() {
    @Test
    fun `should expose step semantics update states and block unavailable choices`() {
        lateinit var handle: SelectionHandle<StepperData>
        var choices = 0
        val steps = listOf(StepData("First"), StepData("Second"), StepData("Locked", isSelectable = false))
        val root = buildTestContent { handle = stepper(StepperData(steps, 1)) { onChange { choices++ } } }
        val buttons = root.findAll("ts-step").filterIsInstance<NativeButton>()
        assertTrue("ts-step--done" in buttons[0].element.classList)
        assertEquals("step", buttons[1].element.getAttribute("aria-current"))
        assertEquals("Second", buttons[1].element.getAttribute("aria-label"))
        assertFalse(buttons[2].isEnabled)
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException::class.java) { buttons[2]._click() }
        assertEquals(0, choices)
        buttons[0]._click()
        assertEquals(0, handle.data.current)
        assertEquals(1, choices)
        handle.data = StepperData(steps, 2)
        assertEquals("step", root.findAll("ts-step")[2].element.getAttribute("aria-current"))
        assertEquals(1, choices)
    }
}
