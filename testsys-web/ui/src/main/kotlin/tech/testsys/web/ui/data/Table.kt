package tech.testsys.web.ui.data

import com.vaadin.flow.component.UI
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.Signal
import tech.testsys.web.ui.Background
import tech.testsys.web.ui.Bindable
import tech.testsys.web.ui.TestSysDsl
import tech.testsys.web.ui.layout.BlockScope

/** Rows of a table page when the page does not choose. */
internal const val DEFAULT_PAGE_SIZE: Int = 20

/**
 * Handle of a table: reloads its rows and shows or hides it with its pagination.
 *
 * @param T the type of the rows.
 * @property isVisible whether the table and its pagination are shown; the block stays. A manual change while
 * [bindVisible] is bound, and a second binding, throw [BindingActiveException].
 * @property selected the keys of the selected rows, kept across pages.
 * @property selection the keys of the selected rows as a read-only signal; changes with every change of the
 * selection, including [clearSelection].
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class TableHandle<T> internal constructor(internal val table: DataTable<T>) {
    private val visible = Bindable(table.root.element, initial = true) { value -> table.isShown = value }

    // The UI that shows the table: the current one while the page is built, then the one it is attached to. Kept here
    // because refresh may run in a thread without a current UI, where the component tree must not be read.
    @Volatile
    private var ui: UI? = UI.getCurrent()

    // Refreshes skipped while the table was detached, and whether one of them asked for the first page; UI thread only.
    private var hasSkippedRefresh = false
    private var isSkippedToFirstPage = false

    init {
        table.root.addAttachListener { event ->
            ui = event.ui
            if (hasSkippedRefresh) reloadNow(isSkippedToFirstPage)
        }
    }

    var isVisible: Boolean
        get() = visible.value
        set(value) {
            visible.value = value
        }

    val selected: Set<Any>
        get() = table.selected.toSet()

    val selection: Signal<Set<Any>> = table.selection

    /**
     * Binds [isVisible] to [signal]: every value it produces is shown at once. A manual [isVisible] while bound,
     * and a second binding, throw [BindingActiveException].
     *
     * @since %CURRENT_VERSION%
     */
    fun bindVisible(signal: Signal<Boolean>): SignalBinding<Boolean> = visible.bind(signal)

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
     * Fetches the current page again and flashes its new rows, or fetches the first page if [toFirstPage]. May be
     * called from any thread: the fetch runs in the UI thread. A closed page ignores the call; a detached table, e.g.
     * after navigation to another route, fetches nothing until it is attached again and then refreshes once.
     *
     * @since %CURRENT_VERSION%
     */
    fun refresh(toFirstPage: Boolean = false) {
        val shownBy = ui
        if (shownBy == null) {
            refreshIfAttached(toFirstPage)
        } else {
            Background.inUi(shownBy) { refreshIfAttached(toFirstPage) }
        }
    }

    /** Refreshes an attached table; a detached one keeps the refresh for its next attach, the first page winning. */
    private fun refreshIfAttached(toFirstPage: Boolean) {
        if (table.root.isAttached) {
            reloadNow(toFirstPage)
        } else {
            hasSkippedRefresh = true
            isSkippedToFirstPage = isSkippedToFirstPage || toFirstPage
        }
    }

    private fun reloadNow(toFirstPage: Boolean) {
        hasSkippedRefresh = false
        isSkippedToFirstPage = false
        table.reload(toFirstPage, highlightNew = !toFirstPage)
    }
}

/**
 * Fills the block body with a horizontally scrollable table fetched by [fetch], [pageSize] rows a page;
 * [key] identifies a row, and a [selectable] table adds a checkbox column. The pagination goes to the end of the block footer.
 *
 * @param T the type of the rows.
 * @throws IllegalArgumentException if [pageSize] is below one or the table declares no columns.
 * @throws IllegalStateException if the block already holds rows, a table, an empty state or a load.
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
    placeTable(table = table.root, pager = table.pager.root) { host -> table.pagerHost = host }
    return TableHandle(table)
}
