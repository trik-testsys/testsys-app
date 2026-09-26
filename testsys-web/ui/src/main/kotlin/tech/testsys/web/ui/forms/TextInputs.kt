package tech.testsys.web.ui.forms

import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import tech.testsys.web.ui.layout.BlockRowScope

/**
 * Adds a single-line text field with a [label] and an optional [hint] under it.
 * The label takes [labelSize] columns and the control [size] columns of the row.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.textInput(
    label: String,
    labelSize: Int,
    size: Int,
    hint: String? = null,
    configure: ValueInput<String>.() -> Unit = {},
): ValueInput<String> = addInput(label, labelSize, size, TextField(), hint, configure)

/**
 * Adds a single-line field for codes and identifiers, typed in a monospace font.
 * The label takes [labelSize] columns and the control [size] columns of the row.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.codeInput(
    label: String,
    labelSize: Int,
    size: Int,
    hint: String? = null,
    configure: ValueInput<String>.() -> Unit = {},
): ValueInput<String> = addInput(label, labelSize, size, TextField().apply { element.setAttribute("data-ts-mono", true) }, hint, configure)

/**
 * Adds a multi-line text field from [minLines] (two by default) to [maxLines] (no limit by default) lines high that
 * scrolls beyond [maxLines], so equal values fix its height; the label takes [labelSize] and the control [size] columns.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.textArea(
    label: String,
    labelSize: Int,
    size: Int,
    hint: String? = null,
    minLines: Int? = null,
    maxLines: Int? = null,
    configure: ValueInput<String>.() -> Unit = {},
): ValueInput<String> {
    require(minLines == null || minLines >= 1) { "Text area '$label' needs at least 1 line of height, got $minLines" }
    require(maxLines == null || maxLines >= 1) { "Text area '$label' needs at least 1 line before scrolling, got $maxLines" }
    require(minLines == null || maxLines == null || minLines <= maxLines) {
        "Text area '$label' has minLines $minLines above maxLines $maxLines"
    }
    val area = TextArea().apply {
        if (minLines != null) minRows = minLines
        maxRows = maxLines
    }
    return addInput(label, labelSize, size, area, hint, configure)
}
