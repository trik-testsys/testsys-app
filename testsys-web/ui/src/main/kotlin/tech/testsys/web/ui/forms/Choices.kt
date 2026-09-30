package tech.testsys.web.ui.forms

import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.select.Select
import tech.testsys.web.ui.layout.BlockRowScope
import tech.testsys.web.ui.layout.ContentScope

/**
 * Adds a drop-down to choose one of [items], each shown by [itemLabel].
 * The label takes [labelSize] columns and the control [size] columns of the row.
 *
 * @param T the type of the items.
 * @since %CURRENT_VERSION%
 */
fun <T : Any> BlockRowScope.select(
    label: String,
    labelSize: Int,
    size: Int,
    items: List<T>,
    itemLabel: (T) -> String,
    hint: String? = null,
    configure: ValueInput<T?>.() -> Unit = {},
): ValueInput<T?> = addInput(label, labelSize, size, choiceSelect(items, itemLabel), hint, configure)

/**
 * Adds a filter drop-down of [items] shown by [itemLabel], with [label] as its placeholder and accessible name, and a first
 * item [emptyLabel] that resets it to `null` unless that is `null`. It ignores the block edit mode and, small in a block
 * head or a table cell, shows an error as a red border only.
 *
 * @param T the type of the items.
 * @since %CURRENT_VERSION%
 */
fun <T : Any> ContentScope.select(
    label: String,
    items: List<T>,
    itemLabel: (T) -> String,
    emptyLabel: String? = null,
    configure: ValueInput<T?>.() -> Unit = {},
): ValueInput<T?> {
    val control = choiceSelect(items, itemLabel).apply {
        placeholder = label
        emptyLabel?.let { caption ->
            isEmptySelectionAllowed = true
            emptySelectionCaption = caption
        }
    }
    return addLabelLessInput(label, control, configure)
}

/**
 * Adds a checkbox field: the [label] on the left, the box alone in the value cell.
 * The label takes [labelSize] columns and the box [size] columns of the row.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.checkbox(label: String, labelSize: Int, size: Int, configure: ValueInput<Boolean>.() -> Unit = {}): ValueInput<Boolean> =
    addInput(label, labelSize, size, Checkbox(), hint = null, configure)

/** Builds a drop-down of [items] shown by [itemLabel]. */
private fun <T : Any> choiceSelect(items: List<T>, itemLabel: (T) -> String): Select<T?> =
    // Select<T?> makes the value nullable: nothing is chosen until the user picks an item.
    Select<T?>().apply {
        setItems(items)
        setItemLabelGenerator { item -> item?.let(itemLabel).orEmpty() }
    }
