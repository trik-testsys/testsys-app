package tech.testsys.web.ui.data

import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.Text
import tech.testsys.web.ui.TestSysDsl
import tech.testsys.web.ui.UiTexts
import tech.testsys.web.ui.core.IconName
import tech.testsys.web.ui.feedback.EmptyContent
import tech.testsys.web.ui.layout.ContentScope
import tech.testsys.web.ui.layout.Placement
import tech.testsys.web.ui.overlay.MenuScope
import tech.testsys.web.ui.overlay.menu
import java.time.LocalDate
import java.time.LocalDateTime

/** How a column lays out its cells; the look follows the meaning of the column. */
internal enum class CellKind(val cssClass: String?) {
    Text(null),
    Code("ts-num"),
    Number("ts-num ts-right"),
    Date(null),
    Content(null),
    Menu("ts-right"),
}

/** Column of a table: its [title], optional [sortKey], [width] and how it fills a cell of a row. */
internal class TableColumn<T>(
    val title: String,
    val sortKey: String?,
    val width: ColumnWidth,
    val kind: CellKind,
    val fill: (T, HasComponents) -> Unit,
)

/** Columns and settings collected by a [TableScope]. */
internal class TableSpec<T>(val columns: List<TableColumn<T>>, val empty: EmptyContent, val rowClick: ((T) -> Unit)?)

/**
 * Scope of a table: its columns in order, the empty state and the row click.
 *
 * @param T the type of the rows.
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class TableScope<T> internal constructor(private val texts: UiTexts) {
    private val columns = mutableListOf<TableColumn<T>>()
    private var emptyContent: EmptyContent = EmptyContent(texts.table.empty)
    private var rowClick: ((T) -> Unit)? = null

    /** Whether [empty] set the empty state, so that a table which sets its own can tell. */
    internal var hasOwnEmpty: Boolean = false
        private set

    /** Whether [menuColumn] added the menu column, which must stay the last one. */
    internal var hasMenuColumn: Boolean = false
        private set

    /**
     * Adds a column of plain text; a column with [sortKey] can be sorted by it, and [width] is chosen by what the
     * column holds.
     *
     * @throws IllegalStateException if the menu column is already added.
     * @since %CURRENT_VERSION%
     */
    fun textColumn(title: String, sortKey: String? = null, width: ColumnWidth = ColumnWidth.Auto, value: (T) -> String?) {
        add(title, sortKey, width, CellKind.Text) { row -> value(row) ?: EMPTY_CELL }
    }

    /**
     * Adds a column of identifiers and codes in a monospace font. [sortKey] and [width] are as in [textColumn].
     *
     * @throws IllegalStateException if the menu column is already added.
     * @since %CURRENT_VERSION%
     */
    fun codeColumn(title: String, sortKey: String? = null, width: ColumnWidth = ColumnWidth.Auto, value: (T) -> String?) {
        add(title, sortKey, width, CellKind.Code) { row -> value(row) ?: EMPTY_CELL }
    }

    /**
     * Adds a column of numbers aligned right, with digits grouped by the locale. [sortKey] and [width] are as in
     * [textColumn].
     *
     * @throws IllegalStateException if the menu column is already added.
     * @since %CURRENT_VERSION%
     */
    fun numberColumn(title: String, sortKey: String? = null, width: ColumnWidth = ColumnWidth.Auto, value: (T) -> Number?) {
        add(title, sortKey, width, CellKind.Number) { row -> formatNumber(value(row), texts) }
    }

    /**
     * Adds a column of dates in the calendar format of the locale. [sortKey] and [width] are as in [textColumn].
     *
     * @throws IllegalStateException if the menu column is already added.
     * @since %CURRENT_VERSION%
     */
    fun dateColumn(title: String, sortKey: String? = null, width: ColumnWidth = ColumnWidth.Auto, value: (T) -> LocalDate?) {
        add(title, sortKey, width, CellKind.Date) { row -> formatDate(value(row), texts) }
    }

    /**
     * Adds a column of dates with times in the calendar format of the locale. [sortKey] and [width] are as in
     * [textColumn].
     *
     * @throws IllegalStateException if the menu column is already added.
     * @since %CURRENT_VERSION%
     */
    fun dateTimeColumn(title: String, sortKey: String? = null, width: ColumnWidth = ColumnWidth.Auto, value: (T) -> LocalDateTime?) {
        add(title, sortKey, width, CellKind.Date) { row -> formatDateTime(value(row), texts) }
    }

    /**
     * Adds a column whose cells hold display [content] of the row, e.g. a badge, tags or actions. [sortKey] and [width]
     * are as in [textColumn].
     *
     * @throws IllegalStateException if the menu column is already added.
     * @since %CURRENT_VERSION%
     */
    fun column(title: String, sortKey: String? = null, width: ColumnWidth = ColumnWidth.Auto, content: ContentScope.(T) -> Unit) {
        checkBeforeMenuColumn()
        columns += TableColumn(title, sortKey, width, CellKind.Content) { row, cell ->
            ContentScope(cell, texts, Placement.Cell).content(row)
        }
    }

    /**
     * Adds the last, narrow column with an action menu of each row filled by [content].
     *
     * @throws IllegalStateException if the table already has a menu column.
     * @since %CURRENT_VERSION%
     */
    fun menuColumn(content: MenuScope.(T) -> Unit) {
        checkBeforeMenuColumn()
        hasMenuColumn = true
        columns += TableColumn(title = "", sortKey = null, width = ColumnWidth.Auto, kind = CellKind.Menu) { row, cell ->
            ContentScope(cell, texts, Placement.Cell).menu { content(row) }
        }
    }

    /**
     * Sets the empty state shown instead of rows when there are none: [title], optional [description], [icon]
     * and [actions], e.g. a reset of the filter.
     *
     * @since %CURRENT_VERSION%
     */
    fun empty(title: String, description: String? = null, icon: IconName = IconName.File, actions: ContentScope.() -> Unit = {}) {
        emptyContent = EmptyContent(title, description, icon, actions)
        hasOwnEmpty = true
    }

    /**
     * Runs [listener] with the row a user clicks, except clicks on its checkbox and actions.
     *
     * @since %CURRENT_VERSION%
     */
    fun onRowClick(listener: (T) -> Unit) {
        rowClick = listener
    }

    internal fun spec(): TableSpec<T> = TableSpec(columns.toList(), emptyContent, rowClick)

    private fun add(title: String, sortKey: String?, width: ColumnWidth, kind: CellKind, text: (T) -> String) {
        checkBeforeMenuColumn()
        columns += TableColumn(title, sortKey, width, kind) { row, cell -> cell.add(Text(text(row))) }
    }

    private fun checkBeforeMenuColumn() {
        check(!hasMenuColumn) { "The menu column must be the last and only one; declare other columns before menuColumn()" }
    }
}
