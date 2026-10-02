package tech.testsys.web.components.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HtmlContainer
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeLabel
import com.vaadin.flow.component.html.Span

/** Parts of a grid field that its handle changes: the whole field and the required mark of its label. */
internal class FieldParts(val field: Div, val requiredMark: Span)

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
        addClassName("ts-field__required")
        element.setAttribute("aria-hidden", "true")
        isVisible = false
    }
    val caption: HtmlContainer = if (labelAction == null) Div() else NativeLabel()
    caption.addClassName("ts-field__label")
    caption.style.set("grid-column", "span $labelSize")
    caption.add(Span(label).apply { addClassName("ts-field__text") }, requiredMark)
    if (labelAction != null) runOnLabelClick(caption, value, labelAction)
    val valueCell = Div(value).apply {
        addClassName("ts-field__value")
        style.set("grid-column", "span $size")
    }
    val field = Div(caption, valueCell).apply { addClassNames("ts-field", "ts-field--grid") }
    place(labelSize + size, field)
    return FieldParts(field, requiredMark)
}

/**
 * Runs [action] on [control] when [caption] is clicked, in the browser: no server round trip, and the pressed label
 * does not take the focus away first. The snippet runs on every attach, since the browser element may be new, and
 * installs the listeners once per element, since a second click listener would toggle a checkbox back.
 */
private fun runOnLabelClick(caption: Component, control: Component, action: LabelAction) {
    caption.addAttachListener {
        caption.element.executeJs(
            "if (this.__tsLabelAction) return; this.__tsLabelAction = true; " +
                "this.addEventListener('mousedown', e => e.preventDefault()); " +
                "this.addEventListener('click', () => $0.${action.method}())",
            control.element,
        )
    }
}
