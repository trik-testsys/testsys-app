package tech.testsys.web.components.forms

import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.data.converter.StringToIntegerConverter
import com.vaadin.flow.dom.Element
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.layout.BlockHandle

class BindingTests : MockVaadinTests() {
    private class Form(var count: Int = 7)

    @Test
    fun `should skip hidden converted field in general validation and explicit write`() {
        lateinit var input: ValueInput<String>
        buildTestRow { input = textInput("Количество", labelSize = 4, size = 20) }
        val binder = Binder<Form>()
        val binding = binder.forField(input).withConverter(StringToIntegerConverter("Число"))
            .bind({ form -> form.count }, { form, value -> form.count = value })
        val form = Form()
        binder.readBean(form)
        assertSame(binding, binding.skipWhenHidden())
        input.isVisible = false
        input.value = "не число"

        val status = binder.validate()
        binder.writeBean(form)

        assertTrue(status.isOk)
        assertEquals(7, form.count)
    }

    @Test
    fun `should preserve bean while ancestor is hidden and resume writing after showing`() {
        lateinit var input: ValueInput<String>
        lateinit var block: BlockHandle
        buildTestPage {
            block = block { row { input = textInput("Количество", labelSize = 4, size = 20) } }
        }
        val binder = bind(input)
        val form = Form()
        binder.readBean(form)
        block.isVisible = false
        input.value = "12"

        assertTrue(binder.writeBeanIfValid(form))
        assertEquals(7, form.count)
        block.isVisible = true
        assertTrue(binder.writeBeanIfValid(form))
        assertEquals(12, form.count)
    }

    @Test
    fun `should resume validation after showing a hidden field`() {
        lateinit var input: ValueInput<String>
        buildTestRow { input = textInput("Количество", labelSize = 4, size = 20) }
        val binder = bind(input)
        input.isVisible = false
        input.value = "не число"
        assertTrue(binder.validate().isOk)

        input.isVisible = true

        assertFalse(binder.validate().isOk)
    }

    @Test
    fun `should keep ordinary hidden bindings in general validation`() {
        lateinit var input: ValueInput<String>
        buildTestRow { input = textInput("Количество", labelSize = 4, size = 20) }
        val binder = Binder<Form>()
        binder.forField(input).withConverter(StringToIntegerConverter("Число"))
            .bind({ form -> form.count }, { form, value -> form.count = value })
        input.isVisible = false
        input.value = "не число"

        assertFalse(binder.validate().isOk)
    }

    @Test
    fun `should still populate a hidden field through readBean`() {
        lateinit var input: ValueInput<String>
        buildTestRow { input = textInput("Количество", labelSize = 4, size = 20) }
        val binder = bind(input)
        input.isVisible = false

        binder.readBean(Form(19))

        assertEquals("19", input.value)
    }

    @Test
    fun `should reject a binding of a raw Vaadin field with context`() {
        val binding = Binder<Form>().forField(TextField()).withConverter(StringToIntegerConverter("Число"))
            .bind({ form -> form.count }, { form, value -> form.count = value })

        val error = assertThrows<IllegalArgumentException> { binding.skipWhenHidden() }

        assertTrue(error.message.orEmpty().contains("ValueInput"))
        assertTrue(error.message.orEmpty().contains("TextField"))
    }

    @Test
    fun `should skip a field hidden by an element without a component`() {
        lateinit var input: ValueInput<String>
        buildTestRow { input = textInput("Количество", labelSize = 4, size = 20) }
        val field = input.component.element
        val parent = field.parent
        val wrapper = Element("div")
        parent.appendChild(wrapper)
        wrapper.appendChild(field)
        wrapper.isVisible = false
        val binder = bind(input)
        input.value = "не число"

        assertTrue(binder.validate().isOk)
    }

    private fun bind(input: ValueInput<String>): Binder<Form> = Binder<Form>().apply {
        forField(input).withConverter(StringToIntegerConverter("Число"))
            .bind({ form -> form.count }, { form, value -> form.count = value }).skipWhenHidden()
    }
}
