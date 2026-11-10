@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.forms

import com.vaadin.flow.component.AbstractField
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasAriaLabel
import com.vaadin.flow.component.HasHelper
import com.vaadin.flow.component.HasValidation
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.data.binder.HasValidator
import tech.testsys.web.components.core.DomProperty
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.setInput
import tech.testsys.web.components.core.setProperty
import tech.testsys.web.components.layout.BlockRowScope
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
    where C : AbstractField<*, T>, C : HasValidation, C : HasValidator<T> {
    (control as? HasHelper)?.helperText = hint
    nameControl(control, label)
    control.element.setInput(true)
    // A native label toggles its checkbox on a click and focuses any other control.
    val labelAction = if (control is Checkbox) LabelAction.Click else LabelAction.Focus
    val parts = placeField(
        label = label,
        labelSize = labelSize,
        size = size,
        value = control,
        labelAction = labelAction,
    )
    val input = ValueInput(
        parts = parts,
        component = control,
        validation = control,
        validator = control,
    )
    input.followBlock(editState)
    return input.apply(configure)
}

/** Gives [control] the accessible name [label], since the visible label of a grid field is not its own. */
internal fun nameControl(control: Component, label: String) {
    if (control is HasAriaLabel) control.setAriaLabel(label) else control.element.setProperty(DomProperty.AccessibleName, label)
}
