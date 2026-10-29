package tech.testsys.web.components.forms

import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.NumberField
import tech.testsys.web.components.layout.BlockRowScope

/**
 * Adds a whole-number field; [min], [max] and [step] are checked by the default validator, [unit] is shown after
 * the value. The label takes [labelSize] columns and the control [size] columns of the row.
 *
 * @since 1.0.0-beta.1
 */
fun BlockRowScope.integerInput(
    label: String,
    labelSize: Int,
    size: Int,
    hint: String? = null,
    min: Int? = null,
    max: Int? = null,
    step: Int? = null,
    unit: String? = null,
    configure: ValueInput<Int?>.() -> Unit = {},
): ValueInput<Int?> {
    val errors = texts.messages.uiForm
    val control = IntegerField().apply {
        if (min != null) setMin(min)
        if (max != null) setMax(max)
        if (step != null) setStep(step)
        if (unit != null) suffixComponent = Span(unit)
        i18n = IntegerField.IntegerFieldI18n()
            .setBadInputErrorMessage(errors.fieldBadInput())
            .setMinErrorMessage(errors.fieldBelowMin())
            .setMaxErrorMessage(errors.fieldAboveMax())
            .setStepErrorMessage(errors.fieldStepMismatch())
    }

    return addInput<IntegerField, Int?>(label, labelSize, size, control, hint, configure)
}

/**
 * Adds a fractional-number field with the same constraints as [integerInput].
 * The label takes [labelSize] columns and the control [size] columns of the row.
 *
 * @since 1.0.0-beta.1
 */
fun BlockRowScope.decimalInput(
    label: String,
    labelSize: Int,
    size: Int,
    hint: String? = null,
    min: Double? = null,
    max: Double? = null,
    step: Double? = null,
    unit: String? = null,
    configure: ValueInput<Double?>.() -> Unit = {},
): ValueInput<Double?> {
    val errors = texts.messages.uiForm
    val control = NumberField().apply {
        if (min != null) setMin(min)
        if (max != null) setMax(max)
        if (step != null) setStep(step)
        if (unit != null) suffixComponent = Span(unit)
        i18n = NumberField.NumberFieldI18n()
            .setBadInputErrorMessage(errors.fieldBadInput())
            .setMinErrorMessage(errors.fieldBelowMin())
            .setMaxErrorMessage(errors.fieldAboveMax())
            .setStepErrorMessage(errors.fieldStepMismatch())
    }

    return addInput<NumberField, Double?>(label, labelSize, size, control, hint, configure)
}
