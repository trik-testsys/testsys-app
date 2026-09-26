package tech.testsys.web.ui.forms

import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.NumberField
import com.vaadin.flow.data.binder.Binder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import tech.testsys.web.ui.MockVaadinTests
import tech.testsys.web.ui.buildTestRow
import tech.testsys.web.ui.control
import tech.testsys.web.ui.testTexts

class NumberInputTests : MockVaadinTests() {
    private class Form(var minutes: Int? = null)

    @Test
    fun `should apply limits and step to integer field`() {
        buildTestRow { integerInput("Длительность", labelSize = 4, size = 20, min = 10, max = 300, step = 5) }

        val field = control<IntegerField>("Длительность")
        assertEquals(10, field.min)
        assertEquals(300, field.max)
        assertEquals(5, field.step)
    }

    @Test
    fun `should show unit after the value`() {
        buildTestRow { integerInput("Длительность", labelSize = 4, size = 20, unit = "минут") }

        assertEquals("минут", control<IntegerField>("Длительность").suffixComponent.element.textRecursively)
    }

    @Test
    fun `should reject integer above max with the localized message`() {
        lateinit var input: ValueInput<Int?>
        buildTestRow { input = integerInput("Длительность", labelSize = 4, size = 20, max = 300) }
        val binder = Binder<Form>().apply {
            forField(input).bind({ form -> form.minutes }, { form, value -> form.minutes = value })
        }

        input.value = 301
        val status = binder.validate()

        assertFalse(status.isOk)
        assertEquals(testTexts.fieldErrors.aboveMax, status.fieldValidationErrors.single().message.orElseThrow())
    }

    @Test
    fun `should report decimal value through the handle`() {
        lateinit var input: ValueInput<Double?>
        buildTestRow { input = decimalInput("Балл", labelSize = 4, size = 20, min = 0.0, max = 100.0, step = 0.5) }

        control<NumberField>("Балл").value = 87.5

        assertEquals(87.5, input.value)
    }
}
