package tech.testsys.web.components.forms

import com.vaadin.flow.component.AbstractField
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasAriaLabel
import com.vaadin.flow.component.HasHelper
import com.vaadin.flow.component.HasValidation
import com.vaadin.flow.component.HasValue
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.data.binder.HasValidator
import com.vaadin.flow.shared.Registration
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.LabelAction
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
    where C : AbstractField<C, T>, C : HasValidation, C : HasValidator<T> {
    return placeInput(label, labelSize, size, control, hint, valueChanges(control), configure)
}

/**
 * Adds [control] as a grid field of [label] with the helper [hint]; [subscribe] passes its value changes to a listener,
 * for controls whose own listener type does not fit the handle.
 */
internal fun <C, T> BlockRowScope.placeInput(
    label: String,
    labelSize: Int,
    size: Int,
    control: C,
    hint: String?,
    subscribe: (HasValue.ValueChangeListener<in HasValue.ValueChangeEvent<T>>) -> Registration,
    configure: ValueInput<T>.() -> Unit,
): ValueInput<T>
    where C : AbstractField<*, T>, C : HasValidation, C : HasValidator<T> {
    (control as? HasHelper)?.helperText = hint
    nameControl(control, label)
    control.element.setAttribute("data-ts-input", true)
    // A native label toggles its checkbox on a click and focuses any other control.
    val labelAction = if (control is Checkbox) LabelAction.Click else LabelAction.Focus
    val parts = placeField(label, labelSize, size, control, labelAction)
    val input = ValueInput(parts, control, control, control, subscribe)
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
    nameControl(control, label)
    control.element.setAttribute("data-ts-input", true)
    control.element.setAttribute("data-ts-size", if (placement.isCompact) "sm" else "md")
    add(control)
    return ValueInput(parts = null, control, control, control, valueChanges(control)).apply(configure)
}

/** Gives [control] the accessible name [label], since the visible label of a grid field is not its own. */
internal fun nameControl(control: Component, label: String) {
    if (control is HasAriaLabel) control.setAriaLabel(label) else control.element.setProperty("accessibleName", label)
}

/** Passes the value changes of [control] to a listener of the handle. */
private fun <C : AbstractField<C, T>, T> valueChanges(
    control: C,
): (HasValue.ValueChangeListener<in HasValue.ValueChangeEvent<T>>) -> Registration {
    return { listener -> control.addValueChangeListener { event -> listener.valueChanged(event) } }
}
