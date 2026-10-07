@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.layout

import com.vaadin.flow.component.html.Div
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.setPageColumns
import tech.testsys.web.components.texts.UiTexts

/**
 * Scope of a page row: blocks side by side whose sizes take at most 24 columns in total.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class PageRowScope internal constructor(private val row: Div, private val texts: UiTexts) {
    private val track = GridTrack(GRID_COLUMNS, owner = "Row")
    private var hasHighlight = false

    /**
     * Adds a block of [size] columns, or of the rest of the row if [size] is `null`; such a block closes the row.
     *
     * @throws IllegalArgumentException if [size] is out of `1..24`.
     * @throws IllegalStateException if the block does not fit in the row.
     * @since %CURRENT_VERSION%
     */
    fun block(size: Int? = null, title: String? = null, subtitle: String? = null, content: BlockScope.() -> Unit): BlockHandle =
        place(size, BlockHeading(title, subtitle), highlight = false, content)

    /**
     * Adds a dark block that highlights one thing, such as a running timer; a row holds at most one.
     * The block takes [size] columns, or the rest of the row if [size] is `null`; such a block closes the row.
     *
     * @throws IllegalArgumentException if [size] is out of `1..24`.
     * @throws IllegalStateException if the block does not fit in the row or the row already holds a highlight block.
     * @since %CURRENT_VERSION%
     */
    fun highlightBlock(size: Int? = null, title: String? = null, subtitle: String? = null, content: BlockScope.() -> Unit): BlockHandle =
        place(size, BlockHeading(title, subtitle), highlight = true, content)

    private fun place(size: Int?, heading: BlockHeading, highlight: Boolean, content: BlockScope.() -> Unit): BlockHandle {
        val columns = size?.also(track::take) ?: track.takeRest()
        if (highlight) {
            check(!hasHighlight) { "Row already holds a highlight block; a row holds at most one" }
            hasHighlight = true
        }
        val block = buildBlock(texts, heading, highlight, span = columns, columns = GRID_COLUMNS, content)
        block.component.element.style.setPageColumns(columns.toDouble())
        row.add(block.component)
        return block
    }
}
