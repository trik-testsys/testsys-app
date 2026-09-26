package tech.testsys.web.ui.data

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeTable
import com.vaadin.flow.component.html.NativeTableBody
import com.vaadin.flow.component.html.NativeTableCell
import com.vaadin.flow.component.html.NativeTableHeader
import com.vaadin.flow.component.html.NativeTableHeaderCell
import com.vaadin.flow.component.html.NativeTableRow
import com.vaadin.flow.component.html.Span
import tech.testsys.web.ui.UiTexts

private const val ARROW_DOWN = " ↓"
private const val ARROW_UP = " ↑"
private const val ARIA_SORT_NONE = "none"
private const val SELECT_COLUMN_WIDTH = "52px"

/** Client-side filter of key presses on a sortable header: Enter and Space sort like a click. */
internal const val SORT_KEY_FILTER: String = "event.key === 'Enter' || event.key === ' '"

/** Controls inside a row whose clicks are their own. */
private const val ROW_CONTROLS =
    "vaadin-checkbox, vaadin-button, vaadin-text-field, button, a, input, label, select, textarea, [role=button]"

/** Client-side filter of row clicks: clicks on controls inside a row do not click the row itself. */
internal const val ROW_CLICK_FILTER: String = "!event.target.closest('$ROW_CONTROLS')"

/**
 * Paged table of rows fetched by [fetch], [pageSize] rows a page: the `.ts-table` markup, sorting by column keys,
 * a [pager] and, if [isSelectable], a checkbox column; all state lives on the server. [key] identifies a row;
 * [highlighted] rows are shown as selected besides the checked ones, e.g. the current value of a lookup.
 */
internal class DataTable<T>(
    private val texts: UiTexts,
    private val key: (T) -> Any,
    private val pageSize: Int,
    private val isSelectable: Boolean,
    private val fetch: (PageRequest) -> Page<T>,
    private val spec: TableSpec<T>,
    private val highlighted: (T) -> Boolean = { false },
) {
    val table: NativeTable = NativeTable().apply { addClassName("ts-table") }
    val pager: Pager = Pager(texts) { target -> load(target) }

    /** Keys of the selected rows, kept across pages, sorting and reloads. */
    val selected: MutableSet<Any> = linkedSetOf()

    /** Called with the keys of the selected rows after every change of the selection. */
    var onSelectionChange: (Set<Any>) -> Unit = {}

    /** Whether the table and its pagination are shown. */
    var isShown: Boolean = true
        set(value) {
            field = value
            table.isVisible = value
            updatePagerVisibility()
        }

    private val body = NativeTableBody()
    private val sortHeaders = mutableListOf<SortHeader>()
    private var sort: Sort? = null
    private var page = 0
    private var pageCount = 1
    private var shown: Page<T> = Page(emptyList(), 0)
    private val shownRows = mutableListOf<ShownRow>()
    private val headerCheckbox = Checkbox().apply {
        setAriaLabel(texts.table.selectAll)
        addValueChangeListener { event -> if (event.isFromClient) selectPage(event.value) }
    }

    /** Component whose visibility follows whether the pager is needed; the block footer if the pager is alone there. */
    var pagerHost: Component = pager.root
        set(value) {
            field = value
            updatePagerVisibility()
        }

    init {
        require(pageSize >= 1) { "Table page size must be at least 1, got $pageSize" }
        table.add(NativeTableHeader(headerRow()), body)
        load(0)
    }

    /** Fetches page [target] (from 0); moves to the last page with rows if the total shrank below it. */
    fun load(target: Int) {
        var result = fetchPage(target)
        page = target
        val lastPage = pageCountOf(result.total) - 1
        if (target > lastPage) {
            page = lastPage
            result = fetchPage(page)
        }
        pageCount = pageCountOf(result.total)
        render(result)
    }

    /** Fetches the current page again, or the first one if [toFirstPage]. */
    fun reload(toFirstPage: Boolean) {
        load(if (toFirstPage) 0 else page)
    }

    /** Unselects all rows and shows the current page again without fetching it. */
    fun clearSelection() {
        selected.clear()
        render(shown)
        onSelectionChange(selected.toSet())
    }

    private fun pageCountOf(total: Int): Int = maxOf(1, (total + pageSize - 1) / pageSize)

    private fun fetchPage(target: Int): Page<T> = fetch(PageRequest(offset = target * pageSize, limit = pageSize, sort = sort))

    private fun headerRow(): NativeTableRow = NativeTableRow().apply {
        if (isSelectable) add(NativeTableHeaderCell(headerCheckbox).apply { style.set("width", SELECT_COLUMN_WIDTH) })
        spec.columns.forEach { column ->
            val cell = NativeTableHeaderCell(column.title)
            if (column.kind == CellKind.Number) cell.addClassName("ts-right")
            column.sortKey?.let { sortKey ->
                cell.addClassName("ts-sortable")
                cell.element.setAttribute("tabindex", "0")
                cell.element.setAttribute("aria-sort", ARIA_SORT_NONE)
                cell.element.addEventListener("click") { toggleSort(sortKey) }
                cell.element.addEventListener("keydown") { toggleSort(sortKey) }.setFilter(SORT_KEY_FILTER).preventDefault()
                sortHeaders += SortHeader(sortKey, column.title, cell)
            }
            add(cell)
        }
    }

    private fun toggleSort(sortKey: String) {
        val current = sort
        val next = if (current?.key == sortKey) current.copy(isDescending = !current.isDescending) else Sort(sortKey, true)
        sort = next
        sortHeaders.forEach { header ->
            val isSorted = header.sortKey == sortKey
            val (arrow, ariaSort) = when {
                !isSorted -> "" to ARIA_SORT_NONE
                next.isDescending -> ARROW_DOWN to "descending"
                else -> ARROW_UP to "ascending"
            }
            header.cell.setClassName("ts-sorted", isSorted)
            header.cell.text = header.title + arrow
            header.cell.element.setAttribute("aria-sort", ariaSort)
        }
        load(0)
    }

    private fun render(result: Page<T>) {
        shown = result
        shownRows.clear()
        body.removeAll()
        if (result.rows.isEmpty()) {
            body.add(emptyRow())
        } else {
            result.rows.forEach { row -> body.add(rowOf(row)) }
        }
        val first = page * pageSize
        val from = if (result.rows.isEmpty()) 0 else first + 1
        pager.show(page = page, pageCount = pageCount, from = from, to = first + result.rows.size, total = result.total)
        updatePagerVisibility()
        updateHeaderCheckbox()
    }

    private fun emptyRow(): NativeTableRow {
        val empty = Div(Span(spec.emptyText).apply { addClassName("ts-empty__title") }).apply { addClassName("ts-empty") }
        val cell = NativeTableCell(empty).apply {
            element.setAttribute("colspan", (spec.columns.size + if (isSelectable) 1 else 0).toString())
            style.set("padding", "0")
        }
        return NativeTableRow(cell).apply { addClassName("ts-row-empty") }
    }

    private fun rowOf(row: T): NativeTableRow = NativeTableRow().apply {
        val shownRow = ShownRow(key(row), this, isHighlighted = highlighted(row))
        shownRows += shownRow
        if (isSelectable) add(NativeTableCell(shownRow.checkbox))
        shownRow.update()
        spec.columns.forEach { column ->
            val cell = NativeTableCell()
            column.kind.cssClass?.split(' ')?.forEach { cssClass -> cell.addClassName(cssClass) }
            column.fill(row, cell)
            add(cell)
        }
        spec.rowClick?.let { listener ->
            addClassName("ts-row-clickable")
            element.addEventListener("click") { listener(row) }.setFilter(ROW_CLICK_FILTER)
        }
    }

    private fun selectPage(isSelected: Boolean) {
        shownRows.forEach { shownRow -> shownRow.select(isSelected) }
        onSelectionChange(selected.toSet())
    }

    private fun updateHeaderCheckbox() {
        val selectedCount = shownRows.count { shownRow -> shownRow.key in selected }
        val isAll = shownRows.isNotEmpty() && selectedCount == shownRows.size
        headerCheckbox.value = isAll
        headerCheckbox.isIndeterminate = !isAll && selectedCount > 0
        headerCheckbox.isEnabled = shownRows.isNotEmpty()
    }

    private fun updatePagerVisibility() {
        pagerHost.isVisible = isShown && pageCount > 1
    }

    /** Row of the shown page: its [key], its markup and its selection checkbox. */
    private inner class ShownRow(val key: Any, private val row: NativeTableRow, private val isHighlighted: Boolean) {
        val checkbox = Checkbox(key in selected).apply {
            setAriaLabel(texts.table.selectRow)
            addValueChangeListener { event ->
                if (event.isFromClient) {
                    select(event.value)
                    updateHeaderCheckbox()
                    onSelectionChange(selected.toSet())
                }
            }
        }

        fun select(isSelected: Boolean) {
            if (isSelected) selected += key else selected -= key
            checkbox.value = isSelected
            update()
        }

        fun update() {
            row.setClassName("ts-row-selected", isHighlighted || key in selected)
        }
    }

    /** Header cell of a sortable column with its plain [title]. */
    private class SortHeader(val sortKey: String, val title: String, val cell: NativeTableHeaderCell)
}
