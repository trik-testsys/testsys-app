package tech.testsys.web.components.forms

import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._fireDomEvent
import com.github.mvysny.kaributesting.v10._setValue
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.datetimepicker.DateTimePicker
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.popover.Popover
import com.vaadin.flow.component.timepicker.TimePicker
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.child
import tech.testsys.web.components.control
import tech.testsys.web.components.pendingJavaScript
import tech.testsys.web.components.testTexts
import tools.jackson.databind.ObjectMapper
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
            assertNull(control<DatePicker>("Начало").prefixComponent)
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
        fun `should use localized suffixes for date and time parts`() {
            buildTestRow { dateTimeInput("Начало", labelSize = 4, size = 20) }

            assertEquals("дата", control<DateTimePicker>("Начало").dateAriaLabel.orElse(null))
            assertEquals("время", control<DateTimePicker>("Начало").timeAriaLabel.orElse(null))
        }

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
        fun `should name both range ends in the field context`() {
            buildTestRow { dateRangeInput("Период", labelSize = 4, size = 20) }

            assertEquals(listOf("Период: с", "Период: до"), _find<DatePicker>().map { picker -> picker.ariaLabel.orElse(null) })
            val prefixes = _find<DatePicker>().map { picker -> picker.prefixComponent as Span }
            assertEquals(listOf("С", "До"), prefixes.map { prefix -> prefix.text })
            assertTrue(prefixes.all { prefix -> prefix.element.getAttribute("slot") == "prefix" })
            assertTrue(prefixes.all { prefix -> prefix.element.getAttribute("aria-hidden") == "true" })
        }

        @Test
        fun `should retain boundary prefixes after setting a filled range`() {
            lateinit var input: ValueInput<DateRange>
            buildTestRow { input = dateRangeInput("Период", labelSize = 4, size = 20) }
            val pickers = _find<DatePicker>()
            val prefixes = pickers.map(DatePicker::getPrefixComponent)

            input.value = DateRange(start, end)

            prefixes.forEachIndexed { index, prefix -> assertSame(prefix, pickers[index].prefixComponent) }
            assertEquals(listOf("С", "До"), prefixes.map { prefix -> (prefix as Span).text })
        }

        @Test
        fun `should describe required range without requiring either end`() {
            lateinit var input: ValueInput<DateRange>
            buildTestRow { input = dateRangeInput("Период", labelSize = 4, size = 20) }

            input.isRequiredIndicatorVisible = true

            val pickers = _find<DatePicker>()
            assertTrue(pickers.all { picker -> picker.ariaDescribedBy.isPresent })
            assertTrue(pickers.none { picker -> picker.isRequiredIndicatorVisible })
            assertEquals(pickers[0].ariaDescribedBy, pickers[1].ariaDescribedBy)
        }

        @Test
        fun `should remove required description when signal clears the indicator`() {
            lateinit var input: ValueInput<DateRange>
            buildTestRow { input = dateRangeInput("Период", labelSize = 4, size = 20) }
            val required = ValueSignal(true)
            input.bindRequiredIndicatorVisible(required)
            assertTrue(_find<DatePicker>().all { picker -> picker.ariaDescribedBy.isPresent })

            required.set(false)

            assertTrue(_find<DatePicker>().none { picker -> picker.ariaDescribedBy.isPresent })
        }

        @Test
        fun `should reject an empty required range`() {
            lateinit var input: ValueInput<DateRange>
            buildTestRow { input = dateRangeInput("Период", labelSize = 4, size = 20) }
            val binder = bindRequired(input)

            val status = binder.validate()

            assertFalse(status.isOk)
        }

        @ParameterizedTest
        @CsvSource("2026-10-01,", ",2026-10-10", "2026-10-01,2026-10-10")
        fun `should accept a required range with at least one boundary`(from: LocalDate?, to: LocalDate?) {
            lateinit var input: ValueInput<DateRange>
            buildTestRow { input = dateRangeInput("Период", labelSize = 4, size = 20) }
            val binder = bindRequired(input)
            input.value = DateRange(from = from, to = to)

            val status = binder.validate()

            assertTrue(status.isOk)
        }

        @Test
        fun `should choose the range picked in the calendar and close it`() {
            lateinit var input: ValueInput<DateRange>
            buildTestRow { input = dateRangeInput("Период", labelSize = 4, size = 20) }
            val popup = _find<Popover>().single().apply { open() }
            val pick = ObjectMapper().createObjectNode().put("event.detail.start", "$start").put("event.detail.end", "$end")

            _find<DateRangeCalendarAdapter>().single()._fireDomEvent("range-pick", pick)

            assertEquals(DateRange(start, end), input.value)
            assertFalse(popup.isOpened)
        }

        @Test
        fun `should keep the calendar open while only the start is picked`() {
            lateinit var input: ValueInput<DateRange>
            buildTestRow { input = dateRangeInput("Период", labelSize = 4, size = 20) }
            val popup = _find<Popover>().single().apply { open() }
            val pick = ObjectMapper().createObjectNode().put("event.detail.start", "$start").putNull("event.detail.end")

            _find<DateRangeCalendarAdapter>().single()._fireDomEvent("range-pick", pick)

            assertEquals(DateRange(from = start), input.value)
            assertTrue(popup.isOpened)
        }

        @Test
        fun `should install the shared calendar again when the field is attached again`() {
            val row = buildTestRow { dateRangeInput("Период", labelSize = 4, size = 20) }
            val field = row.child(0)
            pendingJavaScript()

            field.element.removeFromParent()
            row.element.appendChild(field.element)

            val installed = pendingJavaScript().filter { call -> "__tsRangeCalendar" in call.invocation.expression }
            assertEquals(_find<DatePicker>().map { picker -> picker.element.node }.toSet(), installed.map { call -> call.owner }.toSet())
        }

        @Test
        fun `should allow a separate validator to require both ends`() {
            lateinit var input: ValueInput<DateRange>
            buildTestRow { input = dateRangeInput("Период", labelSize = 4, size = 20) }
            val binder = Binder<Form>().apply {
                forField(input).withValidator({ value -> value.from != null && value.to != null }, "Обе границы")
                    .bind({ form -> form.period }, { form, value -> form.period = value })
            }

            input.value = DateRange(from = start)

            assertFalse(binder.validate().isOk)
        }

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

        private fun bindRequired(input: ValueInput<DateRange>): Binder<Form> = Binder<Form>().apply {
            forField(input).asRequired("Период обязателен").bind({ form -> form.period }, { form, value -> form.period = value })
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
