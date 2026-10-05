@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.forms

import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.page.PendingJavaScriptResult
import com.vaadin.flow.component.popover.Popover
import com.vaadin.flow.data.binder.HasValidator
import com.vaadin.flow.data.binder.ValidationResult
import com.vaadin.flow.data.binder.ValidationStatusChangeEvent
import com.vaadin.flow.data.binder.ValidationStatusChangeListener
import com.vaadin.flow.data.binder.Validator
import com.vaadin.flow.data.binder.ValueContext
import com.vaadin.flow.dom.Element
import com.vaadin.flow.shared.Registration
import tech.testsys.web.components.UiTexts
import tech.testsys.web.components.core.AriaPopup
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.CssTheme
import tech.testsys.web.components.core.ElementType
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.addThemeName
import tech.testsys.web.components.core.setAriaHasPopup
import tech.testsys.web.components.core.setAriaHidden
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setHidden
import tech.testsys.web.components.core.setRangePart
import tech.testsys.web.components.core.setType
import tech.testsys.web.components.core.svgIcon
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

/** Keeps typed boundaries and their validators while one canonical React calendar chooses the range. */
internal class DateRangeField(
    private val start: DatePicker,
    private val end: DatePicker,
    private val reversedMessage: String,
    requiredMessage: String,
    texts: UiTexts,
    calendarName: String,
) : CustomField<DateRange>(DateRange()), HasValidator<DateRange> {
    private var isPresenting = false
    private val requiredDescription = Span(requiredMessage).apply {
        setId("ts-range-required-${UUID.randomUUID()}")
        element.setHidden(true)
    }
    private val trigger = NativeButton().apply {
        addClassNames(CssClass.Btn, CssClass.BtnIcon, CssClass.BtnSecondary)
        element.setType(ElementType.Button)
        element.setAriaHasPopup(AriaPopup.Dialog)
        element.setAttribute(HtmlAttribute.AriaLabel, calendarName)
        element.setAttribute(HtmlAttribute.Title, calendarName)
        add(svgIcon(IconName.Calendar))
    }
    private val calendar = DateRangeCalendarAdapter(texts)
    private val popup = Popover(calendar).apply {
        target = trigger
        isModal = true
        isAutofocus = true
        isCloseOnEsc = true
        isCloseOnOutsideClick = true
        setAriaLabel(calendarName)
        addThemeName(CssTheme.Popover)
    }

    init {
        listOf(start to texts.dateFields.rangeFromPrefix, end to texts.dateFields.rangeToPrefix).forEach { (picker, prefix) ->
            picker.prefixComponent = Span(prefix).apply {
                addClassName(CssClass.DateRangePrefix)
                element.setAriaHidden(true)
            }
        }
        calendar.onPick = { chosen ->
            if (isEnabled && !isReadOnly) {
                setPresentationValue(chosen)
                setModelValue(chosen, true)
                if (chosen.to != null) popup.close()
            }
        }
        popup.addOpenedChangeListener { event ->
            if (event.isOpened) {
                if (isEnabled && !isReadOnly) calendar.present(value) else popup.close()
            } else if (isAttached) {
                trigger.focus()
            }
        }
        listOf(start, end).forEach { picker ->
            picker.element.setRangePart(true)
            picker.element.installRangeCalendar(popup.element)
            picker.isAutoOpen = false
            picker.isClearButtonVisible = true
            picker.setManualValidation(true)
        }
        add(Div(start, end, trigger).apply { addClassName(CssClass.DateRange) }, requiredDescription, popup)
        start.addValueChangeListener { event ->
            end.min = event.value
            if (!isPresenting) {
                updateValue()
                calendar.present(value)
            }
        }
        end.addValueChangeListener { event ->
            start.max = event.value
            if (!isPresenting) {
                updateValue()
                calendar.present(value)
            }
        }
        addDetachListener { popup.close() }
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
            calendar.present(newPresentationValue ?: DateRange())
        } finally {
            isPresenting = false
        }
    }

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

    override fun setReadOnly(readOnly: Boolean) {
        super.setReadOnly(readOnly)
        start.isReadOnly = readOnly
        end.isReadOnly = readOnly
        trigger.isEnabled = isEnabled && !readOnly
        popup.close()
    }

    override fun onEnabledStateChanged(enabled: Boolean) {
        super.onEnabledStateChanged(enabled)
        trigger.isEnabled = enabled && !isReadOnly
        if (!enabled) popup.close()
    }

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

private fun Element.installRangeCalendar(popup: Element): PendingJavaScriptResult = executeJs(
    """
    this.addEventListener('keydown', e => {
      if (e.altKey && e.key === 'ArrowDown' && !this.readOnly && !this.disabled) {
        e.preventDefault(); e.stopImmediatePropagation(); $0.opened = true;
      }
    }, true);
    this.addEventListener('opened-changed', () => {
      if (this.opened) {
        this.opened = false;
        if (!this.readOnly && !this.disabled) $0.opened = true;
      }
    });
    """.trimIndent(),
    popup,
)
