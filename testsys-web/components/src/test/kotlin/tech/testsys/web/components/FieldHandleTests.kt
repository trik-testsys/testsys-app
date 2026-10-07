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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import tech.testsys.web.components.display.field
import tech.testsys.web.components.display.text
import tech.testsys.web.components.forms.FileDropHandle
import tech.testsys.web.components.forms.UploadLimits
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.dateRangeInput
import tech.testsys.web.components.forms.fileDrop
import tech.testsys.web.components.forms.multiSelect
import tech.testsys.web.components.forms.segmentedControl
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.forms.skipWhenHidden
import tech.testsys.web.components.forms.textInput

class FieldHandleTests : MockVaadinTests() {
    private class Draft(var code: String = "")

    enum class LabelLessKind(val expected: Any) {
        Single("a"),
        Segments("a"),
        Multiple(setOf("a")),
    }

    @Test
    fun `should keep values unobscured by default`() {
        lateinit var input: ValueInput<String>
        buildTestRow { input = textInput("Code", labelSize = 4, size = 8) }

        assertFalse(input.isObscured)
        assertFalse(input.component.find("ts-field__value").element.hasAttribute("data-ts-obscured"))
    }

    @Test
    fun `should obscure only the value area and retain Binder writes`() {
        lateinit var input: ValueInput<String>
        buildTestRow { input = textInput("Code", labelSize = 4, size = 8, hint = "Hint") { value = "secret" } }
        val binder = Binder<Draft>()
        val draft = Draft()
        binder.forField(input).bind({ bean -> bean.code }, { bean, code -> bean.code = code })

        input.isObscured = true
        binder.writeBean(draft)

        assertTrue(input.isObscured)
        assertTrue(input.component.find("ts-field__value").element.hasAttribute("data-ts-obscured"))
        assertFalse(input.component.find("ts-field__label").element.hasAttribute("data-ts-obscured"))
        assertEquals("secret", draft.code)
        assertEquals("Hint", control<TextField>("Code").helperText)
        assertTrue(input.isVisible)
        assertTrue(input.isEditable)
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
    fun `should follow obscuring signals without changing visibility`() {
        lateinit var input: ValueInput<String>
        buildTestRow { input = textInput("Code", labelSize = 4, size = 8) }
        val source = ValueSignal(true)
        input.bindObscured(source)
        assertTrue(input.isObscured)

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
    fun `should preserve a label less control tab position when obscuring is disabled`() {
        lateinit var input: ValueInput<String?>
        buildTestPage { block { footer { input = select("Mode", items = listOf("A"), itemLabel = { value -> value }) } } }

        input.isObscured = true
        input.isObscured = false

        assertFalse(input.component.element.hasAttribute("data-ts-obscured"))
        assertFalse(input.component.element.hasAttribute("tabindex"))
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

    @ParameterizedTest
    @EnumSource(LabelLessKind::class)
    fun `should provide an independent obscured area around a disabled label less control`(kind: LabelLessKind) {
        val input = labelLessInput(kind)
        input.isEnabled = false

        input.isObscured = true

        assertEquals("div", input.component.element.tag)
        assertEquals("Mode", input.component.element.getAttribute("aria-label"))
        assertEquals("0", input.component.element.getAttribute("tabindex"))
        assertTrue(input.component.element.isEnabled)
        assertFalse(input.component.child(0).element.isEnabled)
        assertFalse(input.isEnabled)
        assertEquals(kind.expected, input.value)
    }

    @ParameterizedTest
    @EnumSource(LabelLessKind::class)
    fun `should reveal a read only label less control without enabling edits`(kind: LabelLessKind) {
        val input = labelLessInput(kind)
        input.isEditable = false

        input.isObscured = true

        assertEquals("div", input.component.element.tag)
        assertEquals("0", input.component.element.getAttribute("tabindex"))
        assertTrue(input.isReadOnly)
        assertFalse(input.isEditable)
        assertEquals(kind.expected, input.value)
    }

    @Test
    fun `should skip a hidden obscured label less control when Binder writes`() {
        lateinit var input: ValueInput<String?>
        buildTestContent { input = select("Mode", items = listOf("a", "b"), itemLabel = { item -> item }) { value = "a" } }
        val binder = Binder<Draft>()
        val draft = Draft("a")
        binder.forField(input).bind({ bean -> bean.code }, { bean, code -> bean.code = code.orEmpty() }).skipWhenHidden()
        input.isObscured = true
        input.isVisible = false
        input.value = "b"

        binder.writeBean(draft)

        assertEquals("a", draft.code)
        assertFalse(input.component.isVisible)
    }

    @Test
    fun `should write an obscured label less control again once visible`() {
        lateinit var input: ValueInput<String?>
        buildTestContent { input = select("Mode", items = listOf("a", "b"), itemLabel = { item -> item }) { value = "a" } }
        val binder = Binder<Draft>()
        val draft = Draft("a")
        binder.forField(input).bind({ bean -> bean.code }, { bean, code -> bean.code = code.orEmpty() }).skipWhenHidden()
        input.isObscured = true
        input.isVisible = false
        input.value = "b"
        input.isVisible = true

        binder.writeBean(draft)

        assertEquals("b", draft.code)
    }

    private fun labelLessInput(kind: LabelLessKind): ValueInput<*> {
        lateinit var input: ValueInput<*>
        buildTestContent {
            input = when (kind) {
                LabelLessKind.Single -> select("Mode", items = listOf("a", "b"), itemLabel = { item -> item }) { value = "a" }
                LabelLessKind.Segments -> segmentedControl("Mode", items = listOf("a", "b"), itemLabel = { item -> item }) { value = "a" }
                LabelLessKind.Multiple -> multiSelect("Mode", items = listOf("a", "b"), itemLabel = { item -> item }) { value = setOf("a") }
            }
        }
        return input
    }
}
