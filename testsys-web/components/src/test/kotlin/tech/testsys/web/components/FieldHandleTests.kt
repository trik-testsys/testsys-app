package tech.testsys.web.components

import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.display.field
import tech.testsys.web.components.display.text
import tech.testsys.web.components.forms.FileDropHandle
import tech.testsys.web.components.forms.UploadLimits
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.dateRangeInput
import tech.testsys.web.components.forms.fileDrop
import tech.testsys.web.components.forms.textInput

class FieldHandleTests : MockVaadinTests() {
    private class Draft(var code: String = "")

    @Test
    fun `should keep values unobscured by default`() {
        lateinit var input: ValueInput<String>
        buildTestRow { input = textInput("Code", labelSize = 4, size = 8) }

        assertFalse(input.isObscured)
        assertFalse(input.component.find("ts-field__value").element.hasAttribute("data-ts-obscured"))
    }

    @Test
    fun `should obscure only the value area`() {
        lateinit var input: ValueInput<String>
        buildTestRow { input = textInput("Code", labelSize = 4, size = 8, hint = "Hint") { value = "secret" } }

        input.isObscured = true

        assertTrue(input.isObscured)
        assertTrue(input.component.find("ts-field__value").element.hasAttribute("data-ts-obscured"))
        assertFalse(input.component.find("ts-field__label").element.hasAttribute("data-ts-obscured"))
        assertEquals("Hint", control<TextField>("Code").helperText)
        assertTrue(input.isVisible)
        assertTrue(input.isEditable)
    }

    @Test
    fun `should write an obscured value through Binder`() {
        lateinit var input: ValueInput<String>
        buildTestRow { input = textInput("Code", labelSize = 4, size = 8) { value = "secret" } }
        val binder = Binder<Draft>()
        val draft = Draft()
        binder.forField(input).bind({ bean -> bean.code }, { bean, code -> bean.code = code })
        input.isObscured = true

        binder.writeBean(draft)

        assertEquals("secret", draft.code)
    }

    @Test
    fun `should reveal a disabled value through an independently focusable area`() {
        lateinit var input: ValueInput<String>
        buildTestRow {
            input = textInput("Code", labelSize = 4, size = 8) {
                isEnabled = false
                isEditable = false
            }
        }

        input.isObscured = true

        assertEquals("0", input.component.find("ts-field__value").element.getAttribute("tabindex"))
        assertFalse(input.isEnabled)
        assertFalse(input.isEditable)
    }

    @Test
    fun `should restore the tab order of a disabled value when no longer obscured`() {
        lateinit var input: ValueInput<String>
        buildTestRow {
            input = textInput("Code", labelSize = 4, size = 8) {
                isEnabled = false
                isEditable = false
                isObscured = true
            }
        }

        input.isObscured = false

        assertFalse(input.component.find("ts-field__value").element.hasAttribute("tabindex"))
    }

    @Test
    fun `should obscure the value while the bound signal is true`() {
        lateinit var input: ValueInput<String>
        buildTestRow { input = textInput("Code", labelSize = 4, size = 8) }

        input.bindObscured(ValueSignal(true))

        assertTrue(input.isObscured)
    }

    @Test
    fun `should follow obscuring signals without changing visibility`() {
        lateinit var input: ValueInput<String>
        buildTestRow { input = textInput("Code", labelSize = 4, size = 8) }
        val source = ValueSignal(true)
        input.bindObscured(source)

        source.set(false)

        assertFalse(input.isObscured)
        assertTrue(input.isVisible)
        assertFalse(input.component.find("ts-field__value").element.hasAttribute("data-ts-obscured"))
    }

    @Test
    fun `should reject manual changes while obscuring is bound`() {
        lateinit var input: ValueInput<String>
        buildTestRow { input = textInput("Code", labelSize = 4, size = 8) }
        input.bindObscured(ValueSignal(true))

        assertThrows<BindingActiveException> { input.isObscured = false }
    }

    @Test
    fun `should reject a second obscuring binding`() {
        lateinit var input: ValueInput<String>
        buildTestRow { input = textInput("Code", labelSize = 4, size = 8) }
        input.bindObscured(ValueSignal(true))

        assertThrows<BindingActiveException> { input.bindObscured(ValueSignal(false)) }
    }

    @Test
    fun `should return an obscurable information field`() {
        lateinit var info: FieldHandle
        buildTestRow { info = field("Access", labelSize = 4, size = 8) { text("secret") } }

        info.isObscured = true

        assertTrue(info.component.find("ts-field__value").element.hasAttribute("data-ts-obscured"))
        assertEquals("secret", info.component.find("ts-field__content").element.textRecursively)
        assertEquals("0", info.component.find("ts-field__value").element.getAttribute("tabindex"))
    }

    @Test
    fun `should obscure a composite field through its single value area`() {
        lateinit var input: ValueInput<tech.testsys.web.components.forms.DateRange>
        buildTestRow { input = dateRangeInput("Period", labelSize = 4, size = 8) }

        input.isObscured = true

        assertTrue(input.component.find("ts-field__value").element.hasAttribute("data-ts-obscured"))
        assertTrue(input.component.find("ts-date-range").isVisible)
    }

    @Test
    fun `should obscure upload values without disabling file reception`() {
        lateinit var input: FileDropHandle
        buildTestRow {
            input = fileDrop("File", limits = UploadLimits(maxFiles = 1, maxFileBytes = 4, maxMemoryBytes = 4), consume = {})
        }

        input.isObscured = true

        assertTrue(input.isEnabled)
        assertTrue(input.isEditable)
        assertTrue(input.component.element.hasAttribute("data-ts-obscured"))
        assertEquals("File", input.component.element.getAttribute("aria-label"))
    }
}
