@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HtmlContainer
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeLabel
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.page.PendingJavaScriptResult
import com.vaadin.flow.dom.Element
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.ElementRole
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.setAriaHidden
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setGridColumnSpan
import tech.testsys.web.components.core.setRole

/** Parts of a grid field that its handle changes: the whole field and the required mark of its label. */
internal class FieldParts(val field: Div, val requiredMark: Span, val valueCell: Div)

/** What a click on the label of a form field does to its control in the browser, named by the control method. */
internal enum class LabelAction(val method: String) {
    Focus("focus"),
    Click("click"),
}

/**
 * Places a field of [label] on [labelSize] columns and [value] on [size] columns next to it. A form control gets
 * a `<label>` whose click runs [labelAction] on it in the browser; display content ([labelAction] `null`) gets a plain caption.
 */
internal fun BlockRowScope.placeField(label: String, labelSize: Int, size: Int, value: Component, labelAction: LabelAction?): FieldParts {
    require(labelSize >= 1 && size >= 1) { "Field '$label' needs label and value sizes of at least 1, got $labelSize and $size" }
    val requiredMark = Span("*").apply {
        addClassName(CssClass.FieldRequired)
        element.setAriaHidden(true)
        isVisible = false
    }
    val caption: HtmlContainer = if (labelAction == null) Div() else NativeLabel()
    caption.addClassName(CssClass.FieldLabel)
    caption.style.setGridColumnSpan(labelSize)
    caption.add(Span(label).apply { addClassName(CssClass.FieldText) }, requiredMark)
    if (labelAction != null) runOnLabelClick(caption, value, labelAction)
    val valueCell = fieldValueArea(label, value).apply {
        style.setGridColumnSpan(size)
    }
    val field = Div(caption, valueCell).apply { addClassNames(CssClass.Field, CssClass.FieldGrid) }
    place(labelSize + size, field)
    return FieldParts(field = field, requiredMark = requiredMark, valueCell = valueCell)
}

/** Wraps [value] in an independently focusable field value area named by [label]. */
internal fun fieldValueArea(label: String, value: Component): Div = Div(value).apply {
    addClassName(CssClass.FieldValue)
    element.setRole(ElementRole.Group)
    element.setAttribute(HtmlAttribute.AriaLabel, label)
}

/**
 * Runs [action] on [control] when [caption] is clicked, in the browser: no server round trip, and the pressed label
 * does not take the focus away first. The snippet runs on every attach, since the browser element may be new, and
 * installs the listeners once per element, since a second click listener would toggle a checkbox back.
 */
private fun runOnLabelClick(caption: Component, control: Component, action: LabelAction) {
    caption.addAttachListener {
        caption.element.installLabelAction(control.element, action)
    }
}

private fun Element.installLabelAction(control: Element, action: LabelAction): PendingJavaScriptResult = executeJs(
    "if (this.__tsLabelAction) return; this.__tsLabelAction = true; " +
        "this.addEventListener('mousedown', e => e.preventDefault()); " +
        "this.addEventListener('click', () => $0.${action.method}())",
    control,
)
