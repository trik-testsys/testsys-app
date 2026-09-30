package tech.testsys.web.ui.forms

import com.vaadin.flow.component.HasValue
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.datetimepicker.DateTimePicker
import com.vaadin.flow.component.timepicker.TimePicker
import tech.testsys.web.ui.UiTexts
import tech.testsys.web.ui.layout.BlockRowScope
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private val DAYS_IN_WEEK = DayOfWeek.entries.size

/**
 * Adds a date field with a localized calendar.
 * The label takes [labelSize] columns and the control [size] columns of the row.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.dateInput(
    label: String,
    labelSize: Int,
    size: Int,
    hint: String? = null,
    configure: ValueInput<LocalDate?>.() -> Unit = {},
): ValueInput<LocalDate?> = addInput<DatePicker, LocalDate?>(label, labelSize, size, datePicker(texts), hint, configure)

/**
 * Adds a time field; [step] sets the interval of the offered times, the picker default if `null`.
 * The label takes [labelSize] columns and the control [size] columns of the row.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.timeInput(
    label: String,
    labelSize: Int,
    size: Int,
    hint: String? = null,
    step: Duration? = null,
    configure: ValueInput<LocalTime?>.() -> Unit = {},
): ValueInput<LocalTime?> {
    val control = TimePicker().apply {
        locale = texts.locale
        if (step != null) setStep(step)
        i18n = TimePicker.TimePickerI18n()
            .setBadInputErrorMessage(texts.fieldErrors.badInput)
            .setMinErrorMessage(texts.fieldErrors.belowMin)
            .setMaxErrorMessage(texts.fieldErrors.aboveMax)
    }
    return addInput<TimePicker, LocalTime?>(label, labelSize, size, control, hint, configure)
}

/**
 * Adds a field of a date and a time, e.g. the start of a Contest.
 * The label takes [labelSize] columns and the control [size] columns of the row.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.dateTimeInput(
    label: String,
    labelSize: Int,
    size: Int,
    hint: String? = null,
    configure: ValueInput<LocalDateTime?>.() -> Unit = {},
): ValueInput<LocalDateTime?> {
    val control = DateTimePicker().apply {
        locale = texts.locale
        setDatePickerI18n(datePickerI18n(texts))
        setDateAriaLabel(texts.dateFields.date)
        setTimeAriaLabel(texts.dateFields.time)
        i18n = DateTimePicker.DateTimePickerI18n()
            .setBadInputErrorMessage(texts.fieldErrors.badInput)
            .setIncompleteInputErrorMessage(texts.fieldErrors.badInput)
            .setMinErrorMessage(texts.fieldErrors.belowMin)
            .setMaxErrorMessage(texts.fieldErrors.aboveMax)
    }
    return addInput<DateTimePicker, LocalDateTime?>(label, labelSize, size, control, hint, configure)
}

/**
 * Adds a field of a date range; a range that ends before it starts is rejected by the default validator.
 * The label takes [labelSize] columns and the control [size] columns of the row.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.dateRangeInput(
    label: String,
    labelSize: Int,
    size: Int,
    hint: String? = null,
    configure: ValueInput<DateRange>.() -> Unit = {},
): ValueInput<DateRange> {
    val control = DateRangeField(
        start = datePicker(texts).apply { setAriaLabel(texts.dateFields.rangeFrom(label)) },
        end = datePicker(texts).apply { setAriaLabel(texts.dateFields.rangeTo(label)) },
        reversedMessage = texts.dateRangeReversed,
        requiredMessage = texts.dateFields.rangeRequired,
        texts = texts,
        calendarName = texts.components.openCalendar(label),
    )
    val subscribe = { listener: HasValue.ValueChangeListener<in HasValue.ValueChangeEvent<DateRange>> ->
        control.addValueChangeListener { event -> listener.valueChanged(event) }
    }
    return placeInput(label, labelSize, size, control, hint, subscribe, configure)
}

private fun datePicker(texts: UiTexts): DatePicker = DatePicker().apply {
    locale = texts.locale
    i18n = datePickerI18n(texts)
}

private fun datePickerI18n(texts: UiTexts): DatePicker.DatePickerI18n = with(texts.calendar) {
    DatePicker.DatePickerI18n()
        .setMonthNames(monthNames)
        .setWeekdays(weekdays)
        .setWeekdaysShort(weekdaysShort)
        .setFirstDayOfWeek(firstDayOfWeek.value % DAYS_IN_WEEK)
        .setDateFormat(dateFormat)
        .setToday(today)
        .setCancel(cancel)
        .setBadInputErrorMessage(texts.fieldErrors.badInput)
        .setMinErrorMessage(texts.fieldErrors.belowMin)
        .setMaxErrorMessage(texts.fieldErrors.aboveMax)
}
