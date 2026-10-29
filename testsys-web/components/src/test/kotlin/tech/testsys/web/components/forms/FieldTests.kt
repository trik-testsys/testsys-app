package tech.testsys.web.components.forms

import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._setValue
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.child
import tech.testsys.web.components.classes
import tech.testsys.web.components.control
import tech.testsys.web.components.display.field
import tech.testsys.web.components.display.tag
import tech.testsys.web.components.find
import java.time.LocalDate

private const val DURATION = 90
private const val UPDATED_DURATION = 120

class FieldTests : MockVaadinTests() {
    private class Form(var login: String = "")

    @Test
    fun `should place label and value on their columns`() {
        val field = buildTestRow { textInput("Логин", labelSize = 4, size = 8) }.child(0)

        assertTrue(setOf("ts-field", "ts-field--grid").all { cssClass -> cssClass in field.classes() })
        assertEquals("span 12", field.element.style.get("grid-column"))
        assertEquals("span 4", field.find("ts-field__label").element.style.get("grid-column"))
        assertEquals("span 8", field.find("ts-field__value").element.style.get("grid-column"))
    }

    @Test
    fun `should name the control after its label instead of its own label`() {
        buildTestRow { textInput("Логин", labelSize = 4, size = 8) }

        val control = control<TextField>("Логин")
        assertEquals("Логин", control.ariaLabel.orElseThrow())
        assertNull(control.label)
    }

    @Test
    fun `should show the required mark when Binder requires the field`() {
        lateinit var input: ValueInput<String>
        val field = buildTestRow { input = textInput("Логин", labelSize = 4, size = 8) }.child(0)
        val mark = field.find("ts-field__required")
        assertFalse(mark.isVisible)

        Binder<Form>().forField(input).asRequired("Заполните").bind({ form -> form.login }, { form, value -> form.login = value })

        assertTrue(mark.isVisible)
    }

    @Test
    fun `should focus the control on a label click on the client`() {
        val field = buildTestRow { textInput("Логин", labelSize = 4, size = 8) }.child(0)

        val calls = pendingJavaScript()

        val caption = field.find("ts-field__label")
        assertTrue(calls.any { call -> call.owner == caption.element.node && "focus()" in call.invocation.expression })
    }

    @Test
    fun `should toggle a checkbox on a label click on the client`() {
        val field = buildTestRow { checkbox("Открытый тур", labelSize = 4, size = 8) }.child(0)

        val calls = pendingJavaScript()

        val caption = field.find("ts-field__label")
        assertTrue(calls.any { call -> call.owner == caption.element.node && "click()" in call.invocation.expression })
    }

    @Test
    fun `should install the label action once per browser element when the field is attached again`() {
        val row = buildTestRow { checkbox("Открытый тур", labelSize = 4, size = 8) }
        val field = row.child(0)
        pendingJavaScript()

        field.element.removeFromParent()
        row.element.appendChild(field.element)

        // A second attach may reuse the browser element; the guard keeps one listener, so a click toggles the box once.
        val caption = field.find("ts-field__label")
        val calls = pendingJavaScript().filter { call -> call.owner == caption.element.node }
        assertTrue(calls.isNotEmpty())
        assertTrue(calls.all { call -> "if (this.__tsLabelAction) return;" in call.invocation.expression })
    }

    @Test
    fun `should caption a form control with a label element`() {
        val field = buildTestRow { textInput("Логин", labelSize = 4, size = 8) }.child(0)

        assertEquals("label", field.find("ts-field__label").element.tag)
    }

    @Test
    fun `should caption display content without a label element`() {
        val field = buildTestRow { field("Теги", labelSize = 4, size = 20) { tag("графы") } }.child(0)

        assertEquals("div", field.find("ts-field__label").element.tag)
    }

    @Test
    fun `should dim the label of a disabled field`() {
        val field = buildTestRow { textInput("Логин", labelSize = 4, size = 8) { isEnabled = false } }.child(0)

        assertTrue("ts-field--disabled" in field.classes())
    }

    @Test
    fun `should restore the label when the field is enabled again`() {
        lateinit var input: ValueInput<String>
        val field = buildTestRow { input = textInput("Логин", labelSize = 4, size = 8) { isEnabled = false } }.child(0)

        input.isEnabled = true

        assertFalse("ts-field--disabled" in field.classes())
    }

    @Test
    fun `should reject a field without a column for its label`() {
        assertThrows<IllegalArgumentException> { buildTestRow { textInput("Логин", labelSize = 0, size = 8) } }
    }

    @Test
    fun `should reject a field without a column for its value`() {
        assertThrows<IllegalArgumentException> { buildTestRow { textInput("Логин", labelSize = 4, size = 0) } }
    }

    @Test
    fun `should reject fields that overflow the row`() {
        val error = assertThrows<IllegalStateException> {
            buildTestRow {
                textInput("Логин", labelSize = 4, size = 12)
                textInput("Почта", labelSize = 4, size = 8)
            }
        }

        assertTrue(requireNotNull(error.message).contains("16+12 = 28"))
    }

    @Test
    fun `should show any display content as the value of a field`() {
        val field = buildTestRow { field("Теги", labelSize = 4, size = 20) { tag("графы") } }.child(0)

        assertEquals("графы", field.find("ts-field__content").find("ts-tag").element.textRecursively)
    }

    @Nested
    inner class BindEnabledTests {
        @Test
        fun `should disable and dim the field from a false signal`() {
            lateinit var input: ValueInput<String>
            val field = buildTestRow { input = textInput("Логин", labelSize = 4, size = 8) }.child(0)

            input.bindEnabled(ValueSignal(false))

            assertFalse(input.isEnabled)
            assertTrue("ts-field--disabled" in field.classes())
        }

        @Test
        fun `should enable and dim the field after the signal`() {
            lateinit var input: ValueInput<String>
            val field = buildTestRow { input = textInput("Логин", labelSize = 4, size = 8) }.child(0)
            val signal = ValueSignal(false)
            input.bindEnabled(signal)

            signal.set(true)

            assertTrue(input.isEnabled)
            assertTrue(control<TextField>("Логин").isEnabled)
            assertFalse("ts-field--disabled" in field.classes())
        }

        @Test
        fun `should reject a manual value while bound`() {
            lateinit var input: ValueInput<String>
            buildTestRow { input = textInput("Логин", labelSize = 4, size = 8) }
            input.bindEnabled(ValueSignal(true))

            assertThrows<BindingActiveException> { input.isEnabled = false }
        }

        @Test
        fun `should reject a second binding`() {
            lateinit var input: ValueInput<String>
            buildTestRow { input = textInput("Логин", labelSize = 4, size = 8) }
            input.bindEnabled(ValueSignal(true))

            assertThrows<BindingActiveException> { input.bindEnabled(ValueSignal(false)) }
        }
    }

    @Nested
    inner class BindRequiredIndicatorVisibleTests {
        @Test
        fun `should show the required mark from a true signal`() {
            lateinit var input: ValueInput<String>
            val field = buildTestRow { input = textInput("Логин", labelSize = 4, size = 8) }.child(0)

            input.bindRequiredIndicatorVisible(ValueSignal(true))

            assertTrue(field.find("ts-field__required").isVisible)
        }

        @Test
        fun `should hide the indicator and the required mark after the signal`() {
            lateinit var input: ValueInput<String>
            val field = buildTestRow { input = textInput("Логин", labelSize = 4, size = 8) }.child(0)
            val signal = ValueSignal(true)
            input.bindRequiredIndicatorVisible(signal)

            signal.set(false)

            assertFalse(input.isRequiredIndicatorVisible)
            assertFalse(control<TextField>("Логин").isRequiredIndicatorVisible)
            assertFalse(field.find("ts-field__required").isVisible)
        }

        @Test
        fun `should reject a manual value while bound`() {
            lateinit var input: ValueInput<String>
            buildTestRow { input = textInput("Логин", labelSize = 4, size = 8) }
            input.bindRequiredIndicatorVisible(ValueSignal(true))

            assertThrows<BindingActiveException> { input.isRequiredIndicatorVisible = false }
        }
    }

    @Nested
    inner class BindValueTests {
        private val start = LocalDate.of(2026, 10, 1)
        private val end = LocalDate.of(2026, 10, 10)

        @Test
        fun `should show the value of a read-only binding`() {
            lateinit var input: ValueInput<String>
            buildTestRow { input = textInput("Логин", labelSize = 4, size = 8) }
            val signal = ValueSignal("anna")
            input.bindValue(signal, null)

            signal.set("boris")

            assertEquals("boris", control<TextField>("Логин").value)
            assertEquals("boris", input.value)
        }

        @Test
        fun `should reject a manual value of a read-only binding`() {
            lateinit var input: ValueInput<String>
            buildTestRow { input = textInput("Логин", labelSize = 4, size = 8) }
            input.bindValue(ValueSignal("anna"), null)

            assertThrows<IllegalStateException> { input.value = "boris" }
        }

        @Test
        fun `should write a user edit to the signal through the callback`() {
            lateinit var input: ValueInput<String>
            buildTestRow { input = textInput("Логин", labelSize = 4, size = 8) }
            val signal = ValueSignal("anna")
            input.bindValue(signal) { value -> signal.set(value) }

            control<TextField>("Логин")._setValue("boris")

            assertEquals("boris", signal.peek())
            assertEquals("boris", input.value)
        }

        @Test
        fun `should keep the signal value when the callback does not take a user edit`() {
            lateinit var input: ValueInput<String>
            buildTestRow { input = textInput("Логин", labelSize = 4, size = 8) }
            val signal = ValueSignal("anna")
            input.bindValue(signal) { _ -> }

            control<TextField>("Логин")._setValue("boris")

            assertEquals("anna", control<TextField>("Логин").value)
            assertEquals("anna", signal.peek())
        }

        @Test
        fun `should bind the value of an integer field`() {
            lateinit var input: ValueInput<Int?>
            buildTestRow { input = integerInput("Длительность", labelSize = 4, size = 8) }
            val signal = ValueSignal<Int?>(DURATION)
            input.bindValue(signal) { value -> signal.set(value) }
            assertEquals(DURATION, control<IntegerField>("Длительность").value)

            control<IntegerField>("Длительность")._setValue(UPDATED_DURATION)

            assertEquals(UPDATED_DURATION, signal.peek())
        }

        @Test
        fun `should bind the value of a date field`() {
            lateinit var input: ValueInput<LocalDate?>
            buildTestRow { input = dateInput("Начало", labelSize = 4, size = 8) }
            val signal = ValueSignal<LocalDate?>(start)
            input.bindValue(signal) { value -> signal.set(value) }
            assertEquals(start, control<DatePicker>("Начало").value)

            control<DatePicker>("Начало")._setValue(end)

            assertEquals(end, signal.peek())
        }

        @Test
        fun `should show the bound range on both pickers of a date range`() {
            lateinit var input: ValueInput<DateRange>
            buildTestRow { input = dateRangeInput("Период", labelSize = 4, size = 8) }
            val signal = ValueSignal(DateRange())
            input.bindValue(signal, null)

            signal.set(DateRange(start, end))

            assertEquals(listOf(start, end), _find<DatePicker>().map { picker -> picker.value })
        }

        @Test
        fun `should write a user edit of a picker of a date range to the signal`() {
            lateinit var input: ValueInput<DateRange>
            buildTestRow { input = dateRangeInput("Период", labelSize = 4, size = 8) }
            val signal = ValueSignal(DateRange(from = start))
            input.bindValue(signal) { value -> signal.set(value) }

            _find<DatePicker>()[1]._setValue(end)

            assertEquals(DateRange(start, end), signal.peek())
        }
    }

    /**
     * Takes the JavaScript calls queued since the last call. Vaadin queues element calls until the response is written,
     * so the queue is flushed as the response would do before it is read; Karibu `_get`/`_find` between the steps of
     * a test would consume the queue.
     */
    private fun pendingJavaScript(): List<PendingJavaScriptInvocation> {
        val internals = UI.getCurrent().internals
        internals.stateTree.runExecutionsBeforeClientResponse()
        return internals.dumpPendingJavaScriptInvocations()
    }
}
