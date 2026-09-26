package tech.testsys.web.ui.data

import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.Text
import tech.testsys.web.ui.TestSysDsl
import tech.testsys.web.ui.UiTexts
import tech.testsys.web.ui.layout.ContentScope
import tech.testsys.web.ui.layout.Placement
import java.time.LocalDate
import java.time.LocalDateTime

/** How a column lays out its cells; the look follows the meaning of the column. */
internal enum class CellKind(val cssClass: String?) {
    Text(null),
    Code("ts-num"),
    Number("ts-num ts-right"),
    Date(null),
    Content(null),
}

/** Column of a table: its [title], optional [sortKey] and how it fills a cell of a row. */
internal class TableColumn<T>(
    val title: String,
    val sortKey: String?,
    val kind: CellKind,
    val fill: (T, HasComponents) -> Unit,
)

/** Columns and settings collected by a [TableScope]. */
internal class TableSpec<T>(val columns: List<TableColumn<T>>, val emptyText: String, val rowClick: ((T) -> Unit)?)

/**
 * Scope of a table: its columns in order, the empty text and the row click.
 *
 * @param T the type of the rows.
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class TableScope<T> internal constructor(private val texts: UiTexts) {
    private val columns = mutableListOf<TableColumn<T>>()
    private var emptyText: String = texts.table.empty
    private var rowClick: ((T) -> Unit)? = null

    /** Whether [empty] set the empty text, so that a table which sets its own can tell. */
    internal var hasOwnEmptyText: Boolean = false
        private set

    /**
     * Adds a column of plain text; a column with [sortKey] can be sorted by it.
     *
     * @since %CURRENT_VERSION%
     */
    fun textColumn(title: String, sortKey: String? = null, value: (T) -> String?) {
        add(title, sortKey, CellKind.Text) { row -> value(row) ?: EMPTY_CELL }
    }

    /**
     * Adds a column of identifiers and codes in a monospace font.
     *
     * @since %CURRENT_VERSION%
     */
    fun codeColumn(title: String, sortKey: String? = null, value: (T) -> String?) {
        add(title, sortKey, CellKind.Code) { row -> value(row) ?: EMPTY_CELL }
    }

    /**
     * Adds a column of numbers aligned right, with digits grouped by the locale.
     *
     * @since %CURRENT_VERSION%
     */
    fun numberColumn(title: String, sortKey: String? = null, value: (T) -> Number?) {
        add(title, sortKey, CellKind.Number) { row -> formatNumber(value(row), texts) }
    }

    /**
     * Adds a column of dates in the calendar format of the locale.
     *
     * @since %CURRENT_VERSION%
     */
    fun dateColumn(title: String, sortKey: String? = null, value: (T) -> LocalDate?) {
        add(title, sortKey, CellKind.Date) { row -> formatDate(value(row), texts) }
    }

    /**
     * Adds a column of dates with times in the calendar format of the locale.
     *
     * @since %CURRENT_VERSION%
     */
    fun dateTimeColumn(title: String, sortKey: String? = null, value: (T) -> LocalDateTime?) {
        add(title, sortKey, CellKind.Date) { row -> formatDateTime(value(row), texts) }
    }

    /**
     * Adds a column whose cells hold display [content] of the row, e.g. a badge, tags or actions.
     *
     * @since %CURRENT_VERSION%
     */
    fun column(title: String, sortKey: String? = null, content: ContentScope.(T) -> Unit) {
        columns += TableColumn(title, sortKey, CellKind.Content) { row, cell ->
            ContentScope(cell, texts, Placement.Body).content(row)
        }
    }

    /**
     * Sets the [text] shown instead of rows when there are none.
     *
     * @since %CURRENT_VERSION%
     */
    fun empty(text: String) {
        emptyText = text
        hasOwnEmptyText = true
    }

    /**
     * Runs [listener] with the row a user clicks, except clicks on its checkbox and actions.
     *
     * @since %CURRENT_VERSION%
     */
    fun onRowClick(listener: (T) -> Unit) {
        rowClick = listener
    }

    internal fun spec(): TableSpec<T> = TableSpec(columns.toList(), emptyText, rowClick)

    private fun add(title: String, sortKey: String?, kind: CellKind, text: (T) -> String) {
        columns += TableColumn(title, sortKey, kind) { row, cell -> cell.add(Text(text(row))) }
    }
}
