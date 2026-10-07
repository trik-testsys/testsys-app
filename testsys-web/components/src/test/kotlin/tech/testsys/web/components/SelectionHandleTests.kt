package tech.testsys.web.components

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.testsys.web.components.navigation.StepData
import tech.testsys.web.components.navigation.StepperData
import tech.testsys.web.components.navigation.stepper

class SelectionHandleTests : MockVaadinTests() {
    private val initial = StepperData(listOf(StepData("First"), StepData("Second")), current = 1)
    private val requested = mutableListOf<Int>()
    private lateinit var handle: SelectionHandle<StepperData>
    private lateinit var root: Component

    @Test
    fun `should request the clicked choice while the data stay with the bound signal`() {
        buildStepper()
        val signal = ValueSignal(initial)
        handle.bindData(signal)

        firstStep()._click()

        assertEquals(listOf(0), requested)
        assertEquals(1, handle.data.current)
        assertEquals(initial, signal.peek())
    }

    @Test
    fun `should show the value accepted by the bound signal`() {
        buildStepper()
        val signal = ValueSignal(initial)
        handle.bindData(signal)

        signal.set(initial.copy(current = 0))

        assertEquals(0, handle.data.current)
        assertEquals("step", firstStep().element.getAttribute("aria-current"))
    }

    @Test
    fun `should disable the choices while the selection is disabled`() {
        buildStepper()

        handle.isEnabled = false

        assertFalse(firstStep().isEnabled)
    }

    @Test
    fun `should ignore a click that reaches the server while disabled`() {
        buildStepper()
        handle.isEnabled = false
        handle.component.element.isEnabled = true

        firstStep()._click()

        assertEquals(emptyList<Int>(), requested)
        assertEquals(1, handle.data.current)
    }

    @Test
    fun `should ignore a click that reaches the server while a bound signal disables the selection`() {
        buildStepper()
        handle.bindEnabled(ValueSignal(false))
        handle.component.element.isEnabled = true

        firstStep()._click()

        assertEquals(emptyList<Int>(), requested)
    }

    @Test
    fun `should reject a manual enabled state while bound`() {
        buildStepper()
        handle.bindEnabled(ValueSignal(true))

        assertThrows(BindingActiveException::class.java) { handle.isEnabled = false }
    }

    @Test
    fun `should reject manual data while bound`() {
        buildStepper()
        handle.bindData(ValueSignal(initial))

        assertThrows(BindingActiveException::class.java) { handle.data = initial.copy(current = 0) }
    }

    private fun buildStepper() {
        root = buildTestContent { handle = stepper(initial) { onChange { data -> requested += data.current } } }
    }

    private fun firstStep(): NativeButton = root.findAll("ts-step").filterIsInstance<NativeButton>().first()
}
