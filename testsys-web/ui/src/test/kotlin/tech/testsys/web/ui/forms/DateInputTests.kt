package tech.testsys.web.ui.forms

import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._fireDomEvent
import com.github.mvysny.kaributesting.v10._setValue
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.datetimepicker.DateTimePicker
import com.vaadin.flow.component.timepicker.TimePicker
import com.vaadin.flow.data.binder.Binder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.web.ui.MockVaadinTests
import tech.testsys.web.ui.buildTestRow
import tech.testsys.web.ui.control
import tech.testsys.web.ui.testTexts
import java.time.Duration
import java.time.LocalDate

class DateInputTests : MockVaadinTests() {
    private val start = LocalDate.of(2026, 10, 1)
    private val end = LocalDate.of(2026, 10, 10)

    @Nested
    inner class DateTests {
        @Test
        fun `should localize the calendar`() {
            buildTestRow { dateInput("Начало", labelSize = 4, size = 20) }

            val i18n = control<DatePicker>("Начало").i18n
            assertEquals("Январь", i18n.monthNames.first())
            assertEquals(listOf("dd.MM.yyyy"), i18n.dateFormats)
            assertEquals(1, i18n.firstDayOfWeek)
            assertEquals(testTexts.fieldErrors.badInput, i18n.badInputErrorMessage)
        }

        @Test
        fun `should report chosen date through the handle`() {
            lateinit var input: ValueInput<LocalDate?>
            buildTestRow { input = dateInput("Начало", labelSize = 4, size = 20) }

            control<DatePicker>("Начало")._setValue(start)

            assertEquals(start, input.value)
        }
    }

    @Nested
    inner class TimeInputTests {
        @Test
        fun `should use the given step`() {
            buildTestRow { timeInput("Время", labelSize = 4, size = 20, step = Duration.ofMinutes(30)) }

            assertEquals(Duration.ofMinutes(30), control<TimePicker>("Время").step)
        }
    }

    @Nested
    inner class DateTimeInputTests {
        @Test
        fun `should name the control through its accessible name property`() {
            buildTestRow { dateTimeInput("Начало", labelSize = 4, size = 20) }

            assertEquals("Начало", control<DateTimePicker>("Начало").element.getProperty("accessibleName"))
        }
    }

    @Nested
    inner class DateRangeInputTests {
        private inner class Form(var period: DateRange = DateRange())

        @Test
        fun `should name the control through its accessible name property`() {
            buildTestRow { dateRangeInput("Период", labelSize = 4, size = 20) }

            assertEquals("Период", control<DateRangeField>("Период").element.getProperty("accessibleName"))
        }

        @Test
        fun `should limit end by the chosen start and start by the chosen end`() {
            buildTestRow { dateRangeInput("Период", labelSize = 4, size = 20) }
            val (from, to) = _find<DatePicker>()

            from._setValue(start)
            to._setValue(end)

            assertEquals(start, to.min)
            assertEquals(end, from.max)
        }

        @Test
        fun `should report both ends through the handle`() {
            lateinit var input: ValueInput<DateRange>
            buildTestRow { input = dateRangeInput("Период", labelSize = 4, size = 20) }
            val (from, to) = _find<DatePicker>()

            from._setValue(start)
            to._setValue(end)

            assertEquals(DateRange(start, end), input.value)
        }

        @Test
        fun `should reject a range that ends before it starts`() {
            lateinit var input: ValueInput<DateRange>
            buildTestRow { input = dateRangeInput("Период", labelSize = 4, size = 20) }
            val binder = Binder<Form>().apply {
                forField(input).bind({ form -> form.period }, { form, value -> form.period = value })
            }

            input.value = DateRange(from = end, to = start)
            val status = binder.validate()

            assertFalse(status.isOk)
            assertEquals(testTexts.dateRangeReversed, status.fieldValidationErrors.single().message.orElseThrow())
            assertTrue(input.isInvalid)
            assertEquals(testTexts.dateRangeReversed, input.errorMessage)
        }

        @Test
        fun `should reject unparsable text in a picker`() {
            lateinit var input: ValueInput<DateRange>
            buildTestRow { input = dateRangeInput("Период", labelSize = 4, size = 20) }
            val binder = bind(input)
            val (from, to) = _find<DatePicker>()
            from._setValue(start)

            typeUnparsable(to, "32.13.2026")

            assertTrue(input.isInvalid)
            val status = binder.validate()
            assertFalse(status.isOk)
            assertEquals(testTexts.fieldErrors.badInput, status.fieldValidationErrors.single().message.orElseThrow())
        }

        @Test
        fun `should clear errors once the moved end makes the range valid`() {
            lateinit var input: ValueInput<DateRange>
            buildTestRow { input = dateRangeInput("Период", labelSize = 4, size = 20) }
            val binder = bind(input)
            val (from, to) = _find<DatePicker>()
            to._setValue(end)
            from._setValue(end.plusDays(5))

            to._setValue(end.plusDays(10))

            assertTrue(binder.validate().isOk)
            assertFalse(input.isInvalid)
            assertFalse(from.isInvalid)
            assertFalse(to.isInvalid)
        }

        private fun bind(input: ValueInput<DateRange>): Binder<Form> = Binder<Form>().apply {
            forField(input).bind({ form -> form.period }, { form, value -> form.period = value })
        }

        /**
         * Reproduces what the browser sends when the user types text the picker cannot parse: the synchronized
         * `_inputElementValue` property and the `unparsable-change` event, with the value staying empty.
         */
        private fun typeUnparsable(picker: DatePicker, text: String) {
            picker.element.setProperty("_inputElementValue", text)
            picker._fireDomEvent("unparsable-change")
        }
    }
}
