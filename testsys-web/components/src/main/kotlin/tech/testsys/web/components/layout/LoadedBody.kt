@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Footer
import com.vaadin.flow.shared.Registration
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.hasAriaBusy
import tech.testsys.web.components.core.setAriaBusy
import tech.testsys.web.components.core.setClassName
import tech.testsys.web.components.texts.UiTexts

/**
 * Body of a block that a load fills after the block is built, again on every reload: either a placeholder or
 * a failure that fills it whole, or content built by a scope of its own, as the block itself would build it; content
 * that adds nothing hides the body, as a block without rows has none. The pagination of a loaded table goes to the end
 * of the block footer.
 */
internal class LoadedBody(
    private val body: Div,
    private val columns: Int,
    private val texts: UiTexts,
    private val editState: BlockEditState,
    private val title: String?,
) {
    private var footer: Footer? = null
    private var isOwnFooter = false
    private var pager: TablePager? = null
    private var isPagerPlaced = false
    private var editRelay: Registration? = null

    // Whether the body holds anything: false only after content that added nothing.
    private var hasContent = true

    /** The body itself. */
    val component: Component
        get() = body

    /** Whether the body and the pagination of a loaded table, also one loaded later, are shown; not the own footer. */
    var isShown: Boolean = true
        set(value) {
            field = value
            updateVisibility()
            updatePager()
        }

    /** Runs once the block is built; the load starts its first fetch here. */
    var onStart: () -> Unit = {}

    /** Whether the body is marked busy for assistive technologies while it loads. */
    var isBusy: Boolean
        get() = body.element.hasAriaBusy()
        set(value) {
            if (value) body.element.setAriaBusy(true) else body.element.setAriaBusy(null)
        }

    /** Keeps [bar] for the pagination of a loaded table; [isOwn] if nothing else is in it, so it hides with it. */
    fun useFooter(bar: Footer, isOwn: Boolean) {
        footer = bar
        isOwnFooter = isOwn
    }

    fun start() {
        onStart()
    }

    /** Replaces the body with [component] that fills it whole, e.g. a placeholder; [isFlush] drops the padding. */
    fun showWhole(component: Component, isFlush: Boolean) {
        clear()
        body.setClassName(CssClass.BlockBodyGrid, false)
        body.setClassName(CssClass.BlockBodyFlush, isFlush)
        body.add(component)
        hasContent = true
        updateVisibility()
    }

    /**
     * Replaces the body with [content] built as in the block itself: rows on the grid, or a table or an empty state
     * over the whole body. Fields follow the edit mode of the block.
     */
    fun fill(content: BlockScope.() -> Unit) {
        clear()
        body.setClassName(CssClass.BlockBodyGrid, true)
        body.setClassName(CssClass.BlockBodyFlush, false)

        // Each fill gets its own edit state, so that fields of replaced content stop following the block.
        val state = BlockEditState(body.element)
        editRelay = editState.follow { value -> state.isEditable = value }

        val scope = BlockScope(body, columns, texts, state, title, isLoadContent = true).apply(content)
        body.setClassName(CssClass.BlockBodyFlush, scope.isFlushBody)
        hasContent = body.children.findAny().isPresent
        updateVisibility()
        pager = scope.tablePager
        updatePager()
    }

    /** Shows the body while the load is shown and the body holds anything. */
    private fun updateVisibility() {
        body.isVisible = isShown && hasContent
    }

    /** Empties the body, takes the pagination of a loaded table out of the footer and hides the footer if it is own. */
    private fun clear() {
        editRelay?.remove()
        editRelay = null
        takeOutPager()
        pager = null
        body.removeAll()
    }

    /**
     * Keeps the pagination of a loaded table in the footer only while the load is shown: out of the footer, its table
     * no longer shows or hides the footer.
     */
    private fun updatePager() {
        val placed = pager ?: return
        if (!isShown) {
            takeOutPager()
        } else if (!isPagerPlaced) {
            placed.placeInto(checkNotNull(footer) { "Block footer must be set before a load fills the body" }, isOwnFooter)
            isPagerPlaced = true
        }
    }

    /** Takes the pagination of a loaded table out of the footer and hides the footer if it is own. */
    private fun takeOutPager() {
        val bar = footer ?: return
        if (isPagerPlaced) pager?.takeOutOf(bar)
        isPagerPlaced = false
        if (isOwnFooter) bar.isVisible = false
    }
}
