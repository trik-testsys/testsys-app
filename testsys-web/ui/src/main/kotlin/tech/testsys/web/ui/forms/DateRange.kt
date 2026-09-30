package tech.testsys.web.ui.forms

import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.data.binder.HasValidator
import com.vaadin.flow.data.binder.ValidationResult
import com.vaadin.flow.data.binder.ValidationStatusChangeEvent
import com.vaadin.flow.data.binder.ValidationStatusChangeListener
import com.vaadin.flow.data.binder.Validator
import com.vaadin.flow.data.binder.ValueContext
import com.vaadin.flow.shared.Registration
import java.time.LocalDate
import java.util.UUID

/**
 * Range of days with optional ends.
 *
 * @property from the first day, or `null` if the range has no start.
 * @property to the last day, or `null` if the range has no end.
 * @since %CURRENT_VERSION%
 */
data class DateRange(val from: LocalDate? = null, val to: LocalDate? = null)

/**
 * Field of two date pickers where the chosen start limits the end and the chosen end limits the start.
 * The pickers do not validate themselves: the field checks both of them, so an error never outlives a moved limit.
 */
internal class DateRangeField(
    private val start: DatePicker,
    private val end: DatePicker,
    private val reversedMessage: String,
    requiredMessage: String,
) : CustomField<DateRange>(DateRange()), HasValidator<DateRange> {
    private var isPresenting = false
    private val requiredDescription = Span(requiredMessage).apply {
        setId("ts-range-required-${UUID.randomUUID()}")
        element.setAttribute("hidden", true)
    }

    init {
        add(Div(start, end).apply { addClassName("ts-date-range") }, requiredDescription)
        start.setManualValidation(true)
        end.setManualValidation(true)
        start.addValueChangeListener { event ->
            end.min = event.value
            if (!isPresenting) updateValue()
        }
        end.addValueChangeListener { event ->
            start.max = event.value
            if (!isPresenting) updateValue()
        }
    }

    override fun setRequiredIndicatorVisible(requiredIndicatorVisible: Boolean) {
        super.setRequiredIndicatorVisible(requiredIndicatorVisible)
        val description = requiredDescription.id.orElseThrow().takeIf { requiredIndicatorVisible }
        start.setAriaDescribedBy(description)
        end.setAriaDescribedBy(description)
    }

    override fun generateModelValue(): DateRange = DateRange(from = start.value, to = end.value)

    override fun setPresentationValue(newPresentationValue: DateRange?) {
        isPresenting = true
        try {
            start.value = newPresentationValue?.from
            end.value = newPresentationValue?.to
        } finally {
            isPresenting = false
        }
    }

    /** Rejects a reversed range first, since the picker limits come from the other end and would only repeat it. */
    override fun getDefaultValidator(): Validator<DateRange> = Validator { value, _ ->
        val from = value?.from
        val to = value?.to
        if (from != null && to != null && from.isAfter(to)) {
            ValidationResult.error(reversedMessage)
        } else {
            listOf(start, end)
                .map { picker -> picker.defaultValidator.apply(picker.value, ValueContext(picker)) }
                .firstOrNull { result -> result.isError }
                ?: ValidationResult.ok()
        }
    }

    /** Makes the pickers read-only with the field, since the field does not pass the state to them itself. */
    override fun setReadOnly(readOnly: Boolean) {
        super.setReadOnly(readOnly)
        start.isReadOnly = readOnly
        end.isReadOnly = readOnly
    }

    /** Forwards the validation status changes of the pickers, e.g. on unparsable text that keeps the value empty. */
    override fun addValidationStatusChangeListener(listener: ValidationStatusChangeListener<DateRange>): Registration {
        val forward = ValidationStatusChangeListener<LocalDate> { event ->
            listener.validationStatusChanged(ValidationStatusChangeEvent(this, event.newStatus))
        }
        return Registration.combine(
            start.addValidationStatusChangeListener(forward),
            end.addValidationStatusChangeListener(forward),
        )
    }
}
