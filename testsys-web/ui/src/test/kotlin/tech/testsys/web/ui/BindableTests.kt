package tech.testsys.web.ui

import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.dom.Element
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BindableTests : MockVaadinTests() {
    private val applied = mutableListOf<Boolean>()
    private lateinit var element: Element
    private lateinit var bindable: Bindable<Boolean>

    @BeforeEach
    fun setUpElement() {
        element = Div().also { div -> UI.getCurrent().add(div) }.element
        bindable = Bindable(element, initial = false) { value -> applied += value }
    }

    @Test
    fun `should apply a manual value`() {
        bindable.value = true

        assertEquals(listOf(true), applied)
        assertEquals(true, bindable.value)
    }

    @Test
    fun `should follow the bound signal`() {
        val signal = ValueSignal(true)
        bindable.bind(signal)

        signal.set(false)

        assertEquals(listOf(true, false), applied)
        assertEquals(false, bindable.value)
    }

    @Test
    fun `should show mapped values of the bound signal`() {
        val signal = ValueSignal(1)
        bindable.bind(signal) { count -> count > 0 }

        signal.set(0)

        assertEquals(listOf(true, false), applied)
        assertEquals(false, bindable.value)
    }

    @Test
    fun `should report values of the bound signal, not mapped ones, to change callbacks`() {
        val signal = ValueSignal(1)
        val reported = mutableListOf<Int>()
        bindable.bind(signal) { count -> count > 0 }.onChange { context -> reported += context.newValue }

        signal.set(0)

        assertEquals(listOf(1, 0), reported)
    }

    @Test
    fun `should reject a mapped binding after a binding`() {
        bindable.bind(ValueSignal(true))

        assertThrows<BindingActiveException> { bindable.bind(ValueSignal(1)) { count -> count > 0 } }
    }

    @Test
    fun `should not be bound before a binding`() {
        assertFalse(bindable.isBound)
    }

    @Test
    fun `should be bound after a binding`() {
        bindable.bind(ValueSignal(true))

        assertTrue(bindable.isBound)
    }

    @Test
    fun `should reject a manual value while bound`() {
        bindable.bind(ValueSignal(true))

        assertThrows<BindingActiveException> { bindable.value = false }
    }

    @Test
    fun `should reject a second binding`() {
        bindable.bind(ValueSignal(true))

        assertThrows<BindingActiveException> { bindable.bind(ValueSignal(false)) }
    }

    @Test
    fun `should keep the previous value when the new one is rejected`() {
        val count = Bindable<Int>(element, initial = 1) { value -> require(value >= 0) { "Negative count" } }

        assertThrows<IllegalArgumentException> { count.value = -1 }

        assertEquals(1, count.value)
    }
}
