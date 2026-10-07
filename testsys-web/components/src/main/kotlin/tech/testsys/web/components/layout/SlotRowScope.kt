@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.layout

import com.vaadin.flow.component.html.Div
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.setGridColumnSpan
import tech.testsys.web.components.core.setPageColumns
import tech.testsys.web.components.texts.UiTexts

/**
 * Scope of a slot row: blocks side by side on the own 24-column grid of the slot, taking at most 24 columns in total.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class SlotRowScope internal constructor(
    private val row: Div,
    private val slotSize: Int,
    internal val texts: UiTexts,
    private val highlights: HighlightGuard,
) {
    private val track = GridTrack(GRID_COLUMNS, owner = "Slot row")

    /**
     * Adds a block of [size] of the 24 slot columns, or of the whole slot if [size] is `null`;
     * the block is at least one page column wide.
     *
     * @since %CURRENT_VERSION%
     */
    fun block(size: Int? = null, title: String? = null, subtitle: String? = null, content: BlockScope.() -> Unit): BlockHandle =
        place(size, BlockHeading(title, subtitle), highlight = false, content)

    /**
     * Adds a dark block that highlights one thing; a page row holds at most one.
     * The block is at least one page column wide.
     *
     * @since %CURRENT_VERSION%
     */
    fun highlightBlock(size: Int? = null, title: String? = null, subtitle: String? = null, content: BlockScope.() -> Unit): BlockHandle =
        place(size, BlockHeading(title, subtitle), highlight = true, content)

    internal fun <C : com.vaadin.flow.component.Component> placeElement(size: Int?, component: C): C {
        val columns = take(size)
        component.element.style.setGridColumnSpan(columns)
        row.add(component)
        return component
    }

    internal fun place(size: Int?, heading: BlockHeading, highlight: Boolean, content: BlockScope.() -> Unit): BlockHandle {
        val columns = take(size)
        if (highlight) highlights.take()
        val block = buildBlock(texts, heading, highlight, span = columns, columns = GRID_COLUMNS, content)
        block.component.element.style.setPageColumns(slotSize * columns / GRID_COLUMNS.toDouble())
        row.add(block.component)
        return block
    }

    private fun take(size: Int?): Int {
        val columns = size ?: GRID_COLUMNS
        track.take(columns)
        require(slotSize * columns >= GRID_COLUMNS) {
            "Slot row element must be at least one page column wide: slot $slotSize × size $columns < $GRID_COLUMNS"
        }
        return columns
    }
}
