@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Footer
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.removeClassName
import tech.testsys.web.components.texts.UiTexts

private const val TABLE_OWNER = "a table"

/**
 * Scope of a block: the rows of its body, the head actions and the footer.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class BlockScope internal constructor(
    private val body: Div,
    internal val columns: Int,
    internal val texts: UiTexts,
    private val editState: BlockEditState,
    internal val title: String?,
    private val isLoadContent: Boolean = false,
) {
    internal var actionsBar: Div? = null
        private set
    internal var footerBar: Footer? = null
        private set
    internal var isFlushBody: Boolean = false
        private set
    internal var tabsBar: Component? = null
        private set
    internal var filtersBar: Component? = null
        private set
    private var editingSwitch: EditingSwitch? = null

    /** Whether [editing] installed the standard switch of the block mode. */
    internal val hasEditingSwitch: Boolean
        get() = editingSwitch != null

    /** Pagination of the table that fills the body, kept for the end of the footer. */
    internal var tablePager: TablePager? = null
        private set

    private var loadedBody: LoadedBody? = null

    /** What fills the whole body, e.g. "a table"; `null` while the body is a grid of rows. */
    private var wholeBody: String? = null

    /**
     * Adds a row of the body; its elements take at most the block columns in total.
     *
     * @throws IllegalStateException if the block holds a table, an empty state or a load.
     * @since %CURRENT_VERSION%
     */
    fun row(content: BlockRowScope.() -> Unit) {
        check(wholeBody == null) { "Block holds $wholeBody; it takes the whole body" }
        val row = Div().apply { addClassName(CssClass.BlockRow) }
        BlockRowScope(row, columns, texts, editState).content()
        if (row.children.findAny().isPresent) body.add(row)
    }

    /**
     * Fills the right side of the block head: filters, badges and small actions.
     *
     * @throws IllegalStateException if this is the content of a load.
     * @since %CURRENT_VERSION%
     */
    fun actions(content: ContentScope.() -> Unit) {
        checkBlockLevel("actions { }")
        ContentScope(headBar(), texts, Placement.Head, columns).content()
    }

    /**
     * Fills the footer of the block, shown under the body on a sunken background.
     *
     * @throws IllegalStateException if this is the content of a load.
     * @since %CURRENT_VERSION%
     */
    fun footer(content: ContentScope.() -> Unit) {
        checkBlockLevel("footer { }")
        ContentScope(footBar(), texts, Placement.Body, columns).content()
    }

    /**
     * Opens the block in view mode and puts the standard edit, cancel and save actions at the end of its head.
     * [onSave] returns whether the values were saved; otherwise the block stays in edit mode. [onCancel] restores the values.
     *
     * @throws IllegalStateException if the block already has an editing switch, or this is the content of a load.
     * @since %CURRENT_VERSION%
     */
    fun editing(onSave: () -> Boolean, onCancel: () -> Unit) {
        checkBlockLevel("editing()")
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
        checkBlockLevel("tabs()")
        check(tabsBar == null) { "Block already has tabs; call tabs() once" }
        tabsBar = tabs
    }

    internal fun placeFilters(filters: Component) {
        checkBlockLevel("filters()")
        check(filtersBar == null) { "Block already has filters; call filters() once" }
        filtersBar = filters
    }

    /** Makes [component] of [owner] the whole body, which is no longer a grid. */
    internal fun placeWhole(component: Component, owner: String) {
        checkWholeBodyPlace(owner)
        body.removeClassName(CssClass.BlockBodyGrid)
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
     * Makes the whole body a body that [owner], e.g. "a load", fills after the block is built.
     *
     * @throws IllegalStateException if the block holds anything in its body, or this is the content of a load.
     */
    internal fun placeLoad(owner: String): LoadedBody {
        checkBlockLevel("load()")
        checkWholeBodyPlace(owner)
        wholeBody = owner
        return LoadedBody(body, columns, texts, editState, title).also { loaded -> loadedBody = loaded }
    }

    /**
     * Completes the block after its content: the editing switch goes after the head actions and the table pagination
     * to the end of the footer; the footer hides with the pagination if nothing else is in it. A loaded body gets the
     * footer for the pagination of a table it may load, hidden while the block has no footer content of its own.
     */
    internal fun finish() {
        editingSwitch?.install(bar = headBar(), texts = texts, state = editState, body = body, columns = columns)
        tablePager?.let { placed ->
            val isOwnFooter = footerBar == null
            placed.placeInto(footBar(), isOwnFooter)
        }
        loadedBody?.let { loaded ->
            val isOwnFooter = footerBar == null
            val bar = footBar()
            if (isOwnFooter) bar.isVisible = false
            loaded.useFooter(bar, isOwnFooter)
        }
    }

    /** Starts what fills the body once the block is built, e.g. the first fetch of a load. */
    internal fun start() {
        loadedBody?.start()
    }

    /** Checks that what [call] adds belongs to the block itself: the content of a load fills the body only. */
    private fun checkBlockLevel(call: String) {
        check(!isLoadContent) { "Content of a load fills the block body only; call $call on the block itself" }
    }

    private fun footBar(): Footer = footerBar ?: Footer().apply { addClassName(CssClass.BlockFoot) }.also { created -> footerBar = created }

    private fun headBar(): Div = actionsBar ?: Div().apply { addClassName(CssClass.BlockActions) }.also { created -> actionsBar = created }
}

/** Pagination of the table of a block and the callback that gets the component to show and hide with it. */
internal class TablePager(private val pager: Component, private val bindHost: (Component) -> Unit) {
    /** Puts the pagination at the end of [footer]; it shows and hides the whole [footer] if that is its own. */
    fun placeInto(footer: Footer, isOwnFooter: Boolean) {
        footer.add(pager)
        bindHost(if (isOwnFooter) footer else pager)
    }

    /** Takes the pagination out of [footer]; its table no longer shows or hides the footer. */
    fun takeOutOf(footer: Footer) {
        bindHost(pager)
        footer.remove(pager)
    }
}
