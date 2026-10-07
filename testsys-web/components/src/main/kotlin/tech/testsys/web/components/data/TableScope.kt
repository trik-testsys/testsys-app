@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.data

import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.Text
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.feedback.EmptyContent
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.GRID_COLUMNS
import tech.testsys.web.components.layout.Placement
import tech.testsys.web.components.overlay.MenuScope
import tech.testsys.web.components.overlay.iconMenu
import tech.testsys.web.components.texts.UiTexts
import java.time.LocalDate
import java.time.LocalDateTime

/** How a column lays out its cells; the look follows the meaning of the column. */
internal enum class CellKind(val cssClasses: List<CssClass>) {
    Text(emptyList()),
    Code(listOf(CssClass.Num)),
    Number(listOf(CssClass.Num, CssClass.Right)),
    Date(listOf(CssClass.Num)),
    Content(emptyList()),
    Menu(listOf(CssClass.Right)),
}

/** Column of a table: its [title], optional [sortKey], [size] and how it fills a cell of a row. */
internal class TableColumn<T>(
    val title: String,
    val sortKey: String?,
    val size: Int?,
    val kind: CellKind,
    val fill: (T, HasComponents) -> Unit,
)

/** Columns and settings collected by a [TableScope]. */
internal class TableSpec<T>(
    val columns: List<TableColumn<T>>,
    val empty: EmptyContent,
    val layout: TableLayout,
    val rowClick: ((T) -> Unit)?,
)

/**
 * Scope of a table: its columns in order, the empty state and the row click.
 *
 * @param T the type of the rows.
 * @property gridColumns the inherited or explicitly expanded logical capacity for reusable column builders.
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class TableScope<T> internal constructor(
    private val texts: UiTexts,
    val gridColumns: Int = GRID_COLUMNS,
    private val selectionSize: Int = 0,
) {
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
     * Adds a plain text column on [size] grid fractions; `null` takes the remainder and ends ordinary columns.
     * [sortKey] enables sorting by an application key.
     *
     * @throws IllegalStateException if the menu or a remaining ordinary column is already added.
     * @since %CURRENT_VERSION%
     */
    fun textColumn(title: String, sortKey: String? = null, size: Int? = null, value: (T) -> String?) {
        add(title, sortKey, size, CellKind.Text) { row -> value(row) ?: EMPTY_CELL }
    }

    /**
     * Adds a column of identifiers and codes in a monospace font. [sortKey] and [size] are as in [textColumn].
     *
     * @throws IllegalStateException if the menu or a remaining ordinary column is already added.
     * @since %CURRENT_VERSION%
     */
    fun codeColumn(title: String, sortKey: String? = null, size: Int? = null, value: (T) -> String?) {
        add(title, sortKey, size, CellKind.Code) { row -> value(row) ?: EMPTY_CELL }
    }

    /**
     * Adds a column of numbers aligned right, with digits grouped by the locale. [sortKey] and [size] are as in
     * [textColumn].
     *
     * @throws IllegalStateException if the menu or a remaining ordinary column is already added.
     * @since %CURRENT_VERSION%
     */
    fun numberColumn(title: String, sortKey: String? = null, size: Int? = null, value: (T) -> Number?) {
        add(title, sortKey, size, CellKind.Number) { row -> formatNumber(value(row), texts) }
    }

    /**
     * Adds a monospace column of dates in the calendar format of the locale. [sortKey] and [size] are as in [textColumn].
     *
     * @throws IllegalStateException if the menu or a remaining ordinary column is already added.
     * @since %CURRENT_VERSION%
     */
    fun dateColumn(title: String, sortKey: String? = null, size: Int? = null, value: (T) -> LocalDate?) {
        add(title, sortKey, size, CellKind.Date) { row -> formatDate(value(row), texts) }
    }

    /**
     * Adds a monospace column of dates with times in the calendar format of the locale. [sortKey] and [size] are as in
     * [textColumn].
     *
     * @throws IllegalStateException if the menu or a remaining ordinary column is already added.
     * @since %CURRENT_VERSION%
     */
    fun dateTimeColumn(title: String, sortKey: String? = null, size: Int? = null, value: (T) -> LocalDateTime?) {
        add(title, sortKey, size, CellKind.Date) { row -> formatDateTime(value(row), texts) }
    }

    /**
     * Adds a column whose cells hold display [content] of the row, e.g. a badge, tags or actions. [sortKey] and [size]
     * are as in [textColumn].
     *
     * @throws IllegalStateException if the menu or a remaining ordinary column is already added.
     * @since %CURRENT_VERSION%
     */
    fun column(title: String, sortKey: String? = null, size: Int? = null, content: ContentScope.(T) -> Unit) {
        checkBeforeMenuColumn()
        columns += TableColumn(title, sortKey, size, CellKind.Content) { row, cell ->
            ContentScope(cell, texts, Placement.Cell).content(row)
        }
    }

    /**
     * Adds the last column on [size] fractions with an action menu of each row filled by [content].
     *
     * @throws IllegalStateException if the table already has a menu column.
     * @since %CURRENT_VERSION%
     */
    fun menuColumn(size: Int = 1, content: MenuScope.(T) -> Unit) {
        menuColumn(ariaLabel = { texts.menu.actions }, size = size, content = content)
    }

    /**
     * Adds the last action menu column on [size] fractions with a complete accessible name from [ariaLabel].
     *
     * @throws IllegalStateException if the table already has a menu column.
     * @since %CURRENT_VERSION%
     */
    fun menuColumn(ariaLabel: (T) -> String, size: Int = 1, content: MenuScope.(T) -> Unit) {
        check(!hasMenuColumn) { "Table already has a menu column" }
        require(size in 1..gridColumns) { "Table menu size must be in 1..$gridColumns, got $size" }

        hasMenuColumn = true
        columns += TableColumn(title = "", sortKey = null, size = size, kind = CellKind.Menu) { row, cell ->
            ContentScope(cell, texts, Placement.Cell).iconMenu(ariaLabel(row)) { content(row) }
        }
    }

    /**
     * Sets the empty state shown instead of rows when there are none: [title], optional [description], [icon]
     * and [actions], e.g. a reset of the filter.
     *
     * @since %CURRENT_VERSION%
     */
    fun empty(title: String, description: String? = null, icon: IconName = IconName.File, actions: ContentScope.() -> Unit = {}) {
        emptyContent = EmptyContent(title = title, description = description, icon = icon, actions = actions)
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

    internal fun spec(): TableSpec<T> {
        val ordinary = columns.filter { column -> column.kind != CellKind.Menu }
        val menuSize = columns.firstOrNull { column -> column.kind == CellKind.Menu }?.size ?: 0
        val layout = resolveTableLayout(
            gridColumns = gridColumns,
            sizes = ordinary.map { column -> column.size },
            selectionSize = selectionSize,
            menuSize = menuSize,
        )

        return TableSpec(columns.toList(), emptyContent, layout, rowClick)
    }

    private fun add(title: String, sortKey: String?, size: Int?, kind: CellKind, text: (T) -> String) {
        checkBeforeMenuColumn()
        columns += TableColumn(title, sortKey, size, kind) { row, cell -> cell.add(Text(text(row))) }
    }

    private fun checkBeforeMenuColumn() {
        check(columns.all { column -> column.kind == CellKind.Menu || column.size != null }) {
            "Table is full: an ordinary column without a size already takes the rest of the grid"
        }
        check(!hasMenuColumn) { "The menu column must be the last and only one; declare other columns before menuColumn()" }
    }
}
