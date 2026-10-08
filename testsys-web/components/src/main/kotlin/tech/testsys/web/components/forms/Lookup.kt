package tech.testsys.web.components.forms

import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.PageRequest
import tech.testsys.web.components.data.TableScope
import tech.testsys.web.components.data.TableSpec
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.overlay.DialogSize
import tech.testsys.web.components.texts.UiTexts

/**
 * Adds a field of one entity chosen in a dialog that searches with [fetch] and lists rows in [columns], [pageSize]
 * rows a page; [display] gives the text of the chosen value. The label takes [labelSize] and the control [size] columns of the row;
 * [dialogSize] gives the width of the dialog when the default one does not fit [columns].
 *
 * @param T the type of the entities; they are matched with the fetched rows by `equals` and `hashCode`, so [T] must identify
 * an entity by them, as domain entities do by id.
 * @throws IllegalArgumentException if [pageSize] is below one, or [columns] declares no columns, sets its own empty
 * state or row click, which the lookup owns, or adds a menu column.
 * @since %CURRENT_VERSION%
 */
fun <T : Any> BlockRowScope.lookup(
    label: String,
    labelSize: Int,
    size: Int,
    fetch: (query: String, request: PageRequest) -> Page<T>,
    display: (T) -> String,
    columns: TableScope<T>.() -> Unit,
    hint: String? = null,
    pageSize: Int = LOOKUP_PAGE_SIZE,
    dialogSize: DialogSize = DialogSize.M,
    configure: ValueInput<T?>.() -> Unit = {},
): ValueInput<T?> {
    val tableColumns = lookupColumns(label, pageSize, columns)
    val control = LookupField(texts, label, display, fetch, pageSize, tableColumns, dialogSize)
    return addInput(label, labelSize, size, control, hint, configure)
}

/**
 * Field of one entity chosen in the lookup dialog titled [title] with a table of [columns]: shows [display] of the value
 * on the value button and clears the value. A click on a row chooses it and closes the dialog; the current value is
 * highlighted.
 */
internal class LookupField<T : Any>(
    texts: UiTexts,
    private val title: String,
    private val display: (T) -> String,
    private val fetch: (String, PageRequest) -> Page<T>,
    private val pageSize: Int,
    private val columns: TableSpec<T>,
    private val dialogSize: DialogSize,
) : LookupFrame<T?>(texts, title, emptyValue = null) {
    init {
        updateView(value)
    }

    override fun valueName(current: T?): String = current?.let(display).orEmpty()

    override fun valueEquals(value1: T?, value2: T?): Boolean = value1 === value2

    override fun buildDialog(): LookupDialog<T> {
        val dialog = LookupDialog(
            texts,
            title,
            dialogSize,
            fetch,
            pageSize,
            columns,
            isSelectable = false,
            selected = emptySet(),
            highlighted = { row -> row == value },
        ) { row ->
            choose(row)
            shell.close()
        }

        dialog.shell.foot.add(dialog.table.pager.root)
        dialog.table.pagerHost = dialog.shell.foot
        return dialog
    }
}
