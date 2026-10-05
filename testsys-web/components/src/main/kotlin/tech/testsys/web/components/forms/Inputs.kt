@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.forms

import com.vaadin.flow.component.AbstractField
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasAriaLabel
import com.vaadin.flow.component.HasHelper
import com.vaadin.flow.component.HasValidation
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.data.binder.HasValidator
import tech.testsys.web.components.core.ElementSize
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.setInput
import tech.testsys.web.components.core.setSize
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.LabelAction
import tech.testsys.web.components.layout.fieldValueArea
import tech.testsys.web.components.layout.placeField

/** Adds [control] as a grid field of [label] with the helper [hint] and returns its configured handle. */
internal fun <C, T> BlockRowScope.addInput(
    label: String,
    labelSize: Int,
    size: Int,
    control: C,
    hint: String?,
    configure: ValueInput<T>.() -> Unit,
): ValueInput<T>
    where C : AbstractField<*, T>, C : HasValidation, C : HasValidator<T> {
    (control as? HasHelper)?.helperText = hint
    nameControl(control, label)
    control.element.setInput(true)
    // A native label toggles its checkbox on a click and focuses any other control.
    val labelAction = if (control is Checkbox) LabelAction.Click else LabelAction.Focus
    val parts = placeField(label, labelSize, size, control, labelAction)
    val input = ValueInput(
        parts = parts,
        component = control,
        validation = control,
        validator = control,
        valueArea = parts.valueCell,
    )
    input.followBlock(editState)
    return input.apply(configure)
}

/**
 * Adds [control] without a visible label, e.g. a filter in a block head: [label] is only its accessible name, and the
 * control takes the small size in a compact place.
 */
internal fun <C, T> ContentScope.addLabelLessInput(
    label: String,
    control: C,
    configure: ValueInput<T>.() -> Unit,
): ValueInput<T>
    where C : AbstractField<C, T>, C : HasValidation, C : HasValidator<T> {
    control.element.setSize(if (placement.isCompact) ElementSize.Small else ElementSize.Medium)
    return placeLabelLessInput(label, control, configure)
}

/** Places a label-less control in a value area whose focus and hover remain independent of its edit and enabled modes. */
internal fun <C, T> ContentScope.placeLabelLessInput(
    label: String,
    control: C,
    configure: ValueInput<T>.() -> Unit,
): ValueInput<T>
    where C : AbstractField<*, T>, C : HasValidation, C : HasValidator<T> {
    nameControl(control, label)
    control.element.setInput(true)
    val area = fieldValueArea(label, control).apply { addClassName("ts-field__value--inline") }
    add(area)
    return ValueInput(
        parts = null,
        component = control,
        validation = control,
        validator = control,
        valueArea = area,
    ).apply(configure)
}

/** Gives [control] the accessible name [label], since the visible label of a grid field is not its own. */
internal fun nameControl(control: Component, label: String) {
    if (control is HasAriaLabel) control.setAriaLabel(label) else control.element.setProperty("accessibleName", label)
}
