package tech.testsys.web.ui.data

import tech.testsys.web.ui.TestSysDsl
import tech.testsys.web.ui.layout.BlockScope

/** Rows of a table page when the page does not choose. */
internal const val DEFAULT_PAGE_SIZE: Int = 20

/**
 * Handle of a table: reloads its rows and shows or hides it with its pagination.
 *
 * @param T the type of the rows.
 * @property isVisible whether the table and its pagination are shown; the block stays.
 * @property selected the keys of the selected rows, kept across pages.
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class TableHandle<T> internal constructor(internal val table: DataTable<T>) {
    var isVisible: Boolean
        get() = table.isShown
        set(value) {
            table.isShown = value
        }

    val selected: Set<Any>
        get() = table.selected.toSet()

    /**
     * Unselects all rows.
     *
     * @since %CURRENT_VERSION%
     */
    fun clearSelection() {
        table.clearSelection()
    }

    /**
     * Runs [listener] with the keys of the selected rows after every change of the selection.
     *
     * @since %CURRENT_VERSION%
     */
    fun onSelectionChange(listener: (Set<Any>) -> Unit) {
        table.onSelectionChange = listener
    }

    /**
     * Fetches the current page again, or the first one if [toFirstPage], e.g. after a filter change.
     *
     * @since %CURRENT_VERSION%
     */
    fun refresh(toFirstPage: Boolean = false) {
        table.reload(toFirstPage)
    }
}

/**
 * Fills the whole block body with a table of rows fetched by [fetch] page by page, [pageSize] rows a page;
 * [key] identifies a row, and a [selectable] table adds a checkbox column. The pagination goes to the end of the block footer.
 *
 * @param T the type of the rows.
 * @throws IllegalArgumentException if [pageSize] is below one or the table declares no columns.
 * @throws IllegalStateException if the block already holds rows or a table.
 * @since %CURRENT_VERSION%
 */
fun <T> BlockScope.table(
    key: (T) -> Any,
    pageSize: Int = DEFAULT_PAGE_SIZE,
    selectable: Boolean = false,
    fetch: (PageRequest) -> Page<T>,
    content: TableScope<T>.() -> Unit,
): TableHandle<T> {
    val spec = TableScope<T>(texts).apply(content).spec()
    require(spec.columns.isNotEmpty()) { "Table must declare at least one column" }
    checkTablePlace()
    val table = DataTable(texts, key, pageSize, selectable, fetch, spec)
    placeTable(table.table, table.pager.root) { host -> table.pagerHost = host }
    return TableHandle(table)
}
