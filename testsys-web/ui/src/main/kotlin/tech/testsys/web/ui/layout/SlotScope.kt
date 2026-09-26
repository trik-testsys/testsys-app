package tech.testsys.web.ui.layout

import com.vaadin.flow.component.html.Div
import tech.testsys.web.ui.TestSysDsl
import tech.testsys.web.ui.UiTexts

/**
 * Scope of a slot: rows stacked vertically on the columns of the slot.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class SlotScope internal constructor(
    private val slot: Div,
    private val size: Int,
    private val texts: UiTexts,
    private val highlights: HighlightGuard,
) {
    /**
     * Adds a row of the slot; its blocks take at most the slot columns in total.
     *
     * @since %CURRENT_VERSION%
     */
    fun row(content: SlotRowScope.() -> Unit) {
        val row = Div().apply { addClassName("ts-slot__row") }
        slot.add(row)
        SlotRowScope(row, size, texts, highlights).content()
    }
}
