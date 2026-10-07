package tech.testsys.web.components.forms

import com.github.mvysny.kaributesting.v10._find
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.control
import tech.testsys.web.components.layout.BlockHandle

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
        buildTestPage { row { handle = block { row { textInput("Логин", labelSize = 4, size = 20) { isEditable = isFieldEditable } } } } }

        handle.isEditable = isBlockEditable

        assertEquals(isReadOnly, control<TextField>("Логин").isReadOnly)
    }

    @Test
    fun `should make a field editable again with its block`() {
        lateinit var handle: BlockHandle
        buildTestPage { row { handle = block { row { textInput("Логин", labelSize = 4, size = 20) } } } }

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

    @Nested
    inner class BlockBindEditableTests {
        @Test
        fun `should make the fields of the block read-only from a false signal`() {
            lateinit var handle: BlockHandle
            buildTestPage { row { handle = block { row { textInput("Логин", labelSize = 4, size = 20) } } } }

            handle.bindEditable(ValueSignal(false))

            assertTrue(control<TextField>("Логин").isReadOnly)
        }

        @Test
        fun `should make the fields of the block follow the signal`() {
            lateinit var handle: BlockHandle
            buildTestPage { row { handle = block { row { textInput("Логин", labelSize = 4, size = 20) } } } }
            val signal = ValueSignal(false)
            handle.bindEditable(signal)

            signal.set(true)

            assertFalse(control<TextField>("Логин").isReadOnly)
            assertTrue(handle.isEditable)
        }

        @Test
        fun `should reject a manual value while bound`() {
            lateinit var handle: BlockHandle
            buildTestPage { row { handle = block { row { textInput("Логин", labelSize = 4, size = 20) } } } }
            handle.bindEditable(ValueSignal(true))

            assertThrows<BindingActiveException> { handle.isEditable = false }
        }

        @Test
        fun `should reject a second binding`() {
            lateinit var handle: BlockHandle
            buildTestPage { row { handle = block { row { textInput("Логин", labelSize = 4, size = 20) } } } }
            handle.bindEditable(ValueSignal(true))

            assertThrows<BindingActiveException> { handle.bindEditable(ValueSignal(false)) }
        }
    }

    @Nested
    inner class FieldBindEditableTests {
        @Test
        fun `should make the field read-only from a false editable signal`() {
            lateinit var input: ValueInput<String>
            buildTestRow { input = textInput("Логин", labelSize = 4, size = 20) }

            input.bindEditable(ValueSignal(false))

            assertTrue(control<TextField>("Логин").isReadOnly)
        }

        @Test
        fun `should make the field follow the signal`() {
            lateinit var input: ValueInput<String>
            buildTestRow { input = textInput("Логин", labelSize = 4, size = 20) }
            val signal = ValueSignal(false)
            input.bindEditable(signal)

            signal.set(true)

            assertFalse(control<TextField>("Логин").isReadOnly)
            assertTrue(input.isEditable)
        }

        @Test
        fun `should make the field read-only from a true read-only signal`() {
            lateinit var input: ValueInput<String>
            buildTestRow { input = textInput("Логин", labelSize = 4, size = 20) }

            input.bindReadOnly(ValueSignal(true))

            assertTrue(control<TextField>("Логин").isReadOnly)
        }

        @Test
        fun `should make the field follow the read-only signal`() {
            lateinit var input: ValueInput<String>
            buildTestRow { input = textInput("Логин", labelSize = 4, size = 20) }
            val signal = ValueSignal(true)
            input.bindReadOnly(signal)

            signal.set(false)

            assertFalse(control<TextField>("Логин").isReadOnly)
            assertTrue(input.isEditable)
        }

        @Test
        fun `should report read-only values of the bound signal to change callbacks`() {
            lateinit var input: ValueInput<String>
            buildTestRow { input = textInput("Логин", labelSize = 4, size = 20) }
            val signal = ValueSignal(true)
            val reported = mutableListOf<Pair<Boolean, Boolean>>()
            input.bindReadOnly(signal).onChange { context -> reported += context.oldValue to context.newValue }

            signal.set(false)

            assertEquals(listOf(true to true, true to false), reported)
        }

        @ParameterizedTest
        @CsvSource("true, true, false", "true, false, true", "false, true, true", "false, false, true")
        fun `should keep the field read-only unless both are editable with a bound field and a manual block`(
            isFieldEditable: Boolean,
            isBlockEditable: Boolean,
            isReadOnly: Boolean,
        ) {
            lateinit var handle: BlockHandle
            lateinit var input: ValueInput<String>
            buildTestPage { row { handle = block { row { input = textInput("Логин", labelSize = 4, size = 20) } } } }
            input.bindEditable(ValueSignal(isFieldEditable))

            handle.isEditable = isBlockEditable

            assertEquals(isReadOnly, control<TextField>("Логин").isReadOnly)
        }

        @ParameterizedTest
        @CsvSource("true, true, false", "true, false, true", "false, true, true", "false, false, true")
        fun `should keep the field read-only unless both are editable with a manual field and a bound block`(
            isFieldEditable: Boolean,
            isBlockEditable: Boolean,
            isReadOnly: Boolean,
        ) {
            lateinit var handle: BlockHandle
            lateinit var input: ValueInput<String>
            buildTestPage { row { handle = block { row { input = textInput("Логин", labelSize = 4, size = 20) } } } }
            val block = ValueSignal(!isBlockEditable)
            handle.bindEditable(block)
            input.isEditable = isFieldEditable

            block.set(isBlockEditable)

            assertEquals(isReadOnly, control<TextField>("Логин").isReadOnly)
        }

        @ParameterizedTest
        @CsvSource("true, true, false", "true, false, true", "false, true, true", "false, false, true")
        fun `should keep the field read-only unless both are editable with a bound field and a bound block`(
            isFieldEditable: Boolean,
            isBlockEditable: Boolean,
            isReadOnly: Boolean,
        ) {
            lateinit var handle: BlockHandle
            lateinit var input: ValueInput<String>
            buildTestPage { row { handle = block { row { input = textInput("Логин", labelSize = 4, size = 20) } } } }
            val field = ValueSignal(!isFieldEditable)
            val block = ValueSignal(!isBlockEditable)
            input.bindEditable(field)
            handle.bindEditable(block)

            field.set(isFieldEditable)
            block.set(isBlockEditable)

            assertEquals(isReadOnly, control<TextField>("Логин").isReadOnly)
        }

        @Test
        fun `should reject a read-only binding after an editable binding`() {
            lateinit var input: ValueInput<String>
            buildTestRow { input = textInput("Логин", labelSize = 4, size = 20) }
            input.bindEditable(ValueSignal(true))

            assertThrows<BindingActiveException> { input.bindReadOnly(ValueSignal(false)) }
        }

        @Test
        fun `should reject a second editable binding`() {
            lateinit var input: ValueInput<String>
            buildTestRow { input = textInput("Логин", labelSize = 4, size = 20) }
            input.bindReadOnly(ValueSignal(false))

            assertThrows<BindingActiveException> { input.bindEditable(ValueSignal(true)) }
        }

        @Test
        fun `should reject a manual editable state while bound`() {
            lateinit var input: ValueInput<String>
            buildTestRow { input = textInput("Логин", labelSize = 4, size = 20) }
            input.bindEditable(ValueSignal(true))

            assertThrows<BindingActiveException> { input.isEditable = false }
        }

        @Test
        fun `should reject a manual read-only state while bound`() {
            lateinit var input: ValueInput<String>
            buildTestRow { input = textInput("Логин", labelSize = 4, size = 20) }
            input.bindReadOnly(ValueSignal(false))

            assertThrows<BindingActiveException> { input.isReadOnly = true }
        }
    }
}
