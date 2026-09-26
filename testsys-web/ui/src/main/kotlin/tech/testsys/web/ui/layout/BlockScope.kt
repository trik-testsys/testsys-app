package tech.testsys.web.ui.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Footer
import tech.testsys.web.ui.TestSysDsl
import tech.testsys.web.ui.UiTexts

/**
 * Scope of a block: the rows of its body, the head actions and the footer.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class BlockScope internal constructor(
    private val body: Div,
    private val columns: Int,
    internal val texts: UiTexts,
    private val editState: BlockEditState,
) {
    internal var actionsBar: Div? = null
        private set
    internal var footerBar: Footer? = null
        private set
    internal var isFlushBody: Boolean = false
        private set
    private var editingSwitch: EditingSwitch? = null
    private var tablePager: TablePager? = null

    /**
     * Adds a row of the body; its elements take at most the block columns in total.
     *
     * @throws IllegalStateException if the block holds a table.
     * @since %CURRENT_VERSION%
     */
    fun row(content: BlockRowScope.() -> Unit) {
        check(tablePager == null) { "Block holds a table; a table takes the whole body" }
        val row = Div().apply { addClassName("ts-block__row") }
        BlockRowScope(row, columns, texts, editState).content()
        if (row.children.findAny().isPresent) body.add(row)
    }

    /**
     * Fills the right side of the block head: filters, badges and small actions.
     *
     * @since %CURRENT_VERSION%
     */
    fun actions(content: ContentScope.() -> Unit) {
        ContentScope(headBar(), texts, Placement.Head).content()
    }

    /**
     * Fills the footer of the block, shown under the body on a sunken background.
     *
     * @since %CURRENT_VERSION%
     */
    fun footer(content: ContentScope.() -> Unit) {
        ContentScope(footBar(), texts, Placement.Body).content()
    }

    /**
     * Opens the block in view mode and puts the standard edit, cancel and save actions at the end of its head.
     * [onSave] returns whether the values were saved; otherwise the block stays in edit mode. [onCancel] restores the values.
     *
     * @throws IllegalStateException if the block already has an editing switch.
     * @since %CURRENT_VERSION%
     */
    fun editing(onSave: () -> Boolean, onCancel: () -> Unit) {
        check(editingSwitch == null) { "Block already has an editing switch; call editing() once" }
        editingSwitch = EditingSwitch(onSave, onCancel)
        editState.isEditable = false
    }

    /** Asks for a body without padding; called by content that runs edge to edge, such as tables and lists. */
    internal fun requestFlushBody() {
        isFlushBody = true
    }

    /** Checks that the block can take a table: it holds neither rows nor another table. */
    internal fun checkTablePlace() {
        check(tablePager == null) { "Block already holds a table; call table() once" }
        check(!body.children.findAny().isPresent) { "Block holds rows; a table takes the whole body" }
    }

    /**
     * Makes [table] the whole flush body, which is no longer a grid, and keeps [pager] for the end of the footer;
     * [bindPagerHost] gets its host.
     */
    internal fun placeTable(table: Component, pager: Component, bindPagerHost: (Component) -> Unit) {
        checkTablePlace()
        requestFlushBody()
        body.removeClassName("ts-block__body--grid")
        body.add(table)
        tablePager = TablePager(pager, bindPagerHost)
    }

    /**
     * Completes the block after its content: the editing switch goes after the head actions and the table pagination
     * to the end of the footer; the footer hides with the pagination if nothing else is in it.
     */
    internal fun finish() {
        editingSwitch?.install(headBar(), texts, editState)
        tablePager?.let { placed ->
            val isOwnFooter = footerBar == null
            val bar = footBar()
            bar.add(placed.pager)
            placed.bindHost(if (isOwnFooter) bar else placed.pager)
        }
    }

    private fun footBar(): Footer = footerBar ?: Footer().apply { addClassName("ts-block__foot") }.also { created -> footerBar = created }

    private fun headBar(): Div = actionsBar ?: Div().apply { addClassName("ts-block__actions") }.also { created -> actionsBar = created }
}

/** Pagination of the table of a block and the callback that gets the component to show and hide with it. */
private class TablePager(val pager: Component, val bindHost: (Component) -> Unit)
