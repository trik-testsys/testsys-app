package tech.testsys.web.ui.layout

import com.vaadin.flow.component.html.Div
import tech.testsys.web.ui.TestSysDsl
import tech.testsys.web.ui.UiTexts

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
     * Adds a slot of [size] columns; its content is laid out in rows of the same columns.
     *
     * @since %CURRENT_VERSION%
     */
    fun slot(size: Int, content: SlotScope.() -> Unit) {
        track.take(size)
        val slot = Div().apply {
            addClassName("ts-slot")
            style.set("grid-column", "span $size")
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
