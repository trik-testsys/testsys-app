package tech.testsys.web.ui.forms

import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.select.Select
import tech.testsys.web.ui.layout.BlockRowScope

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
): ValueInput<T?> {
    // Select<T?> makes the field value nullable: nothing is chosen until the user picks an item.
    val control = Select<T?>().apply {
        setItems(items)
        setItemLabelGenerator { item -> item?.let(itemLabel).orEmpty() }
    }
    return addInput(label, labelSize, size, control, hint, configure)
}

/**
 * Adds a checkbox field: the [label] on the left, the box alone in the value cell.
 * The label takes [labelSize] columns and the box [size] columns of the row.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.checkbox(label: String, labelSize: Int, size: Int, configure: ValueInput<Boolean>.() -> Unit = {}): ValueInput<Boolean> =
    addInput(label, labelSize, size, Checkbox(), hint = null, configure)
