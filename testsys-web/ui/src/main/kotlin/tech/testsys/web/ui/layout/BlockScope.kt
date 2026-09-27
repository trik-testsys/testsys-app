package tech.testsys.web.ui.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Footer
import tech.testsys.web.ui.TestSysDsl
import tech.testsys.web.ui.UiTexts

private const val TABLE_OWNER = "a table"

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
    internal val title: String?,
) {
    internal var actionsBar: Div? = null
        private set
    internal var footerBar: Footer? = null
        private set
    internal var isFlushBody: Boolean = false
        private set
    internal var tabsBar: Component? = null
        private set
    private var editingSwitch: EditingSwitch? = null
    private var tablePager: TablePager? = null

    /** What fills the whole body, e.g. "a table"; `null` while the body is a grid of rows. */
    private var wholeBody: String? = null

    /**
     * Adds a row of the body; its elements take at most the block columns in total.
     *
     * @throws IllegalStateException if the block holds a table or an empty state.
     * @since %CURRENT_VERSION%
     */
    fun row(content: BlockRowScope.() -> Unit) {
        check(wholeBody == null) { "Block holds $wholeBody; it takes the whole body" }
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

    /** Checks that [owner], e.g. "a table", can take the whole body: it holds neither rows nor another such content. */
    internal fun checkWholeBodyPlace(owner: String) {
        check(wholeBody == null) { "Block already holds $wholeBody; $owner takes the whole body" }
        check(!body.children.findAny().isPresent) { "Block holds rows; $owner takes the whole body" }
    }

    /** Checks that the block can take a table. */
    internal fun checkTablePlace() {
        checkWholeBodyPlace(TABLE_OWNER)
    }

    /**
     * Keeps [tabs] for the block head.
     *
     * @throws IllegalStateException if the block already has tabs.
     */
    internal fun placeTabs(tabs: Component) {
        check(tabsBar == null) { "Block already has tabs; call tabs() once" }
        tabsBar = tabs
    }

    /** Makes [component] of [owner] the whole body, which is no longer a grid. */
    internal fun placeWhole(component: Component, owner: String) {
        checkWholeBodyPlace(owner)
        body.removeClassName("ts-block__body--grid")
        body.add(component)
        wholeBody = owner
    }

    /**
     * Makes [table] the whole flush body and keeps [pager] for the end of the footer; [bindPagerHost] gets its host.
     */
    internal fun placeTable(table: Component, pager: Component, bindPagerHost: (Component) -> Unit) {
        placeWhole(table, TABLE_OWNER)
        requestFlushBody()
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
