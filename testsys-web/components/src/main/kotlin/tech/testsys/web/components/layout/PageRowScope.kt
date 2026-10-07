@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.layout

import com.vaadin.flow.component.html.Div
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.setGridColumnSpan
import tech.testsys.web.components.texts.UiTexts

/**
 * Scope of a page row: slots whose sizes take at most 24 columns in total.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class PageRowScope internal constructor(private val row: Div, private val texts: UiTexts) {
    private val track = GridTrack(GRID_COLUMNS, owner = "Row")
    private val highlights = HighlightGuard()

    /**
     * Adds a slot of [size] columns; its content is laid out in rows on the own 24-column grid of the slot.
     *
     * @since %CURRENT_VERSION%
     */
    fun slot(size: Int, content: SlotScope.() -> Unit) {
        track.take(size)
        val slot = Div().apply {
            addClassName(CssClass.Slot)
            style.setGridColumnSpan(size)
        }
        row.add(slot)
        SlotScope(slot, size, texts, highlights).content()
    }
}

/** Allows at most one highlight block per page row. */
internal class HighlightGuard {
    private var isTaken = false

    fun take() {
        check(!isTaken) { "Row already holds a highlight block; a row holds at most one" }
        isTaken = true
    }
}
