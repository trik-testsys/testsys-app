package tech.testsys.web.ui.forms

import com.github.mvysny.kaributesting.v10._find
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.web.ui.MockVaadinTests
import tech.testsys.web.ui.buildTestPage
import tech.testsys.web.ui.buildTestRow
import tech.testsys.web.ui.control
import tech.testsys.web.ui.layout.BlockHandle

class EditableTests : MockVaadinTests() {
    private class Form(var login: String = "")

    @ParameterizedTest
    @CsvSource("true, true, false", "true, false, true", "false, true, true", "false, false, true")
    fun `should make a field read-only unless both it and its block are editable`(
        isFieldEditable: Boolean,
        isBlockEditable: Boolean,
        isReadOnly: Boolean,
    ) {
        lateinit var handle: BlockHandle
        buildTestPage { handle = block { row { textInput("Логин", labelSize = 4, size = 20) { isEditable = isFieldEditable } } } }

        handle.isEditable = isBlockEditable

        assertEquals(isReadOnly, control<TextField>("Логин").isReadOnly)
    }

    @Test
    fun `should make a field editable again with its block`() {
        lateinit var handle: BlockHandle
        buildTestPage { handle = block { row { textInput("Логин", labelSize = 4, size = 20) } } }

        handle.isEditable = false
        handle.isEditable = true

        assertFalse(control<TextField>("Логин").isReadOnly)
    }

    @Test
    fun `should let code write a read-only field`() {
        lateinit var input: ValueInput<String>
        buildTestRow { input = textInput("Логин", labelSize = 4, size = 20) { isEditable = false } }

        input.value = "anna"

        assertEquals("anna", control<TextField>("Логин").value)
    }

    @Test
    fun `should take Binder read-only as not editable`() {
        lateinit var input: ValueInput<String>
        buildTestRow { input = textInput("Логин", labelSize = 4, size = 20) }
        val binder = Binder<Form>().apply { forField(input).bind({ form -> form.login }, { form, value -> form.login = value }) }

        binder.setReadOnly(true)

        assertFalse(input.isEditable)
        assertTrue(control<TextField>("Логин").isReadOnly)
    }

    @Test
    fun `should make both pickers of a date range read-only`() {
        buildTestRow { dateRangeInput("Период", labelSize = 4, size = 20) { isEditable = false } }

        assertTrue(_find<DatePicker>().all { picker -> picker.isReadOnly })
    }
}
