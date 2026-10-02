package tech.testsys.web.components.forms

import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.child
import tech.testsys.web.components.control

class TextInputTests : MockVaadinTests() {
    private class Form(var name: String = "")

    @Test
    fun `should show hint under the control`() {
        buildTestRow { textInput("Название", labelSize = 4, size = 20, hint = "Видно участникам") }

        val field = control<TextField>("Название")
        assertEquals("Видно участникам", field.helperText)
    }

    @Test
    fun `should mark code input as monospace`() {
        buildTestRow { codeInput("Идентификатор", labelSize = 4, size = 20) }

        assertTrue(control<TextField>("Идентификатор").element.hasAttribute("data-ts-mono"))
    }

    @Test
    fun `should add multi-line text area`() {
        buildTestRow { textArea("Описание", labelSize = 4, size = 20) }

        control<TextArea>("Описание")
    }

    @Test
    fun `should grow text area without limit by default`() {
        buildTestRow { textArea("Описание", labelSize = 4, size = 20) }

        assertNull(control<TextArea>("Описание").maxRows)
    }

    @Test
    fun `should scroll text area after the given number of lines`() {
        buildTestRow { textArea("Описание", labelSize = 4, size = 20, maxLines = 6) }

        assertEquals(6, control<TextArea>("Описание").maxRows)
    }

    @Test
    fun `should keep text area at least the given number of lines high`() {
        buildTestRow { textArea("Описание", labelSize = 4, size = 20, minLines = 5) }

        assertEquals(5, control<TextArea>("Описание").minRows)
    }

    @Test
    fun `should fix text area height when min and max lines are equal`() {
        buildTestRow { textArea("Описание", labelSize = 4, size = 20, minLines = 4, maxLines = 4) }

        val area = control<TextArea>("Описание")
        assertEquals(4, area.minRows)
        assertEquals(4, area.maxRows)
    }

    @Test
    fun `should reject text area whose min lines exceed max lines`() {
        assertThrows<IllegalArgumentException> {
            buildTestRow { textArea("Описание", labelSize = 4, size = 20, minLines = 5, maxLines = 3) }
        }
    }

    @Test
    fun `should reject text area at least no lines high`() {
        assertThrows<IllegalArgumentException> { buildTestRow { textArea("Описание", labelSize = 4, size = 20, minLines = 0) } }
    }

    @Test
    fun `should reject text area limited to no lines`() {
        assertThrows<IllegalArgumentException> { buildTestRow { textArea("Описание", labelSize = 4, size = 20, maxLines = 0) } }
    }

    @Test
    fun `should bind value through Binder`() {
        lateinit var input: ValueInput<String>
        buildTestRow { input = textInput("Название", labelSize = 4, size = 20) }
        val binder = Binder<Form>().apply { forField(input).bind({ form -> form.name }, { form, value -> form.name = value }) }
        val form = Form()

        binder.readBean(Form(name = "Весенний тур"))
        assertEquals("Весенний тур", control<TextField>("Название").value)
        binder.writeBean(form)
        assertEquals("Весенний тур", form.name)
    }

    @Test
    fun `should show Binder error on the field`() {
        lateinit var input: ValueInput<String>
        buildTestRow { input = textInput("Название", labelSize = 4, size = 20) }
        val binder = Binder<Form>().apply {
            forField(input).asRequired("Заполните поле").bind({ form -> form.name }, { form, value -> form.name = value })
        }

        binder.validate()

        val field = control<TextField>("Название")
        assertTrue(field.isInvalid)
        assertEquals("Заполните поле", field.errorMessage)
    }

    @Test
    fun `should disable input through its handle`() {
        buildTestRow { textInput("Название", labelSize = 4, size = 20) { isEnabled = false } }

        assertFalse(control<TextField>("Название").isEnabled)
    }

    @Test
    fun `should hide input through its handle`() {
        lateinit var input: ValueInput<String>
        val field = buildTestRow { input = textInput("Название", labelSize = 4, size = 20) }.child(0)

        input.isVisible = false

        assertFalse(field.isVisible)
    }
}
