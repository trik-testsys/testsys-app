@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.forms

import com.vaadin.flow.component.AbstractField
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasAriaLabel
import com.vaadin.flow.component.HasHelper
import com.vaadin.flow.component.HasValidation
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.data.binder.HasValidator
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.DomProperty
import tech.testsys.web.components.core.ElementRole
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setInput
import tech.testsys.web.components.core.setProperty
import tech.testsys.web.components.core.setRole
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.FieldParts
import tech.testsys.web.components.layout.GuestCardScope
import tech.testsys.web.components.layout.LabelAction
import tech.testsys.web.components.layout.placeField
import tech.testsys.web.components.layout.placeStackedField

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

/**
 * Adds [control] to a guest card as a field of [label] above it, with the helper [hint] under it and [aside] on the right
 * of the label, and returns its configured handle.
 */
internal fun <C, T> GuestCardScope.addStackedInput(
    label: String,
    control: C,
    hint: String?,
    aside: (ContentScope.() -> Unit)?,
    configure: ValueInput<T>.() -> Unit,
): ValueInput<T>
    where C : AbstractField<*, T>, C : HasValidation, C : HasValidator<T> {
    (control as? HasHelper)?.helperText = hint
    nameControl(control, label)
    control.element.setInput(true)
    val parts = placeStackedField(body, label, control, aside)
    return ValueInput(
        parts = parts,
        component = control,
        validation = control,
        validator = control,
    ).apply(configure)
}

/**
 * Places [control] without a visible label in a value area that is the whole field: [label] is only its accessible name.
 */
internal fun <C, T> ContentScope.placeLabelLessInput(
    label: String,
    control: C,
    configure: ValueInput<T>.() -> Unit,
): ValueInput<T>
    where C : AbstractField<*, T>, C : HasValidation, C : HasValidator<T> {
    nameControl(control, label)
    control.element.setInput(true)
    val area = Div(control).apply {
        addClassNames(CssClass.FieldValue, CssClass.FieldValueInline)
        element.setRole(ElementRole.Group)
        element.setAttribute(HtmlAttribute.AriaLabel, label)
    }
    add(area)
    return ValueInput(
        parts = FieldParts(field = area, requiredMark = Span().apply { isVisible = false }, valueCell = area),
        component = control,
        validation = control,
        validator = control,
    ).apply(configure)
}

/** Gives [control] the accessible name [label], since the visible label of a grid field is not its own. */
internal fun nameControl(control: Component, label: String) {
    if (control is HasAriaLabel) control.setAriaLabel(label) else control.element.setProperty(DomProperty.AccessibleName, label)
}
