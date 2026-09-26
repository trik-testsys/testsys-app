package tech.testsys.web.ui.forms

import com.vaadin.flow.component.UI
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.ui.MockVaadinTests
import tech.testsys.web.ui.buildTestRow
import tech.testsys.web.ui.child
import tech.testsys.web.ui.classes
import tech.testsys.web.ui.control
import tech.testsys.web.ui.display.field
import tech.testsys.web.ui.display.tag
import tech.testsys.web.ui.find

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

        assertTrue(error.message!!.contains("16+12 = 28"))
    }

    @Test
    fun `should show any display content as the value of a field`() {
        val field = buildTestRow { field("Теги", labelSize = 4, size = 20) { tag("графы") } }.child(0)

        assertEquals("графы", field.find("ts-field__content").find("ts-tag").element.textRecursively)
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
