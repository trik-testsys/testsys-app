@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.layout

import com.vaadin.flow.component.html.Div
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.setGridColumnSpan
import tech.testsys.web.components.texts.UiTexts

/**
 * Scope of a slot row: blocks side by side whose sizes take at most the slot columns in total.
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
    private val track = GridTrack(slotSize, owner = "Slot row")

    /**
     * Adds a block of [size] columns, or of the whole slot if [size] is `null`.
     *
     * @since %CURRENT_VERSION%
     */
    fun block(size: Int? = null, title: String? = null, subtitle: String? = null, content: BlockScope.() -> Unit): BlockHandle =
        place(size, BlockHeading(title, subtitle), highlight = false, content)

    /**
     * Adds a dark block that highlights one thing; a page row holds at most one.
     *
     * @since %CURRENT_VERSION%
     */
    fun highlightBlock(size: Int? = null, title: String? = null, subtitle: String? = null, content: BlockScope.() -> Unit): BlockHandle =
        place(size, BlockHeading(title, subtitle), highlight = true, content)

    internal fun <C : com.vaadin.flow.component.Component> placeElement(size: Int?, component: C): C {
        val columns = size ?: slotSize
        track.take(columns)
        component.element.style.setGridColumnSpan(columns)
        row.add(component)
        return component
    }

    internal fun place(size: Int?, heading: BlockHeading, highlight: Boolean, content: BlockScope.() -> Unit): BlockHandle {
        val columns = size ?: slotSize
        track.take(columns)
        if (highlight) highlights.take()
        val block = buildBlock(texts, heading, highlight, span = columns, columns = columns, content)
        row.add(block.component)
        return block
    }
}
