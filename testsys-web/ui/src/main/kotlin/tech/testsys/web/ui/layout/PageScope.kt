package tech.testsys.web.ui.layout

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Main
import tech.testsys.web.ui.TestSysDsl
import tech.testsys.web.ui.UiTexts

/**
 * Scope of a page body: rows and full-width blocks stacked vertically with the standard gap.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class PageScope internal constructor(private val main: Main, private val texts: UiTexts) {
    /**
     * Adds a row of the 24-column page grid filled with slots.
     *
     * @since %CURRENT_VERSION%
     */
    fun row(content: PageRowScope.() -> Unit) {
        val row = Div().apply { addClassName("ts-row") }
        main.add(row)
        PageRowScope(row, texts).content()
    }

    /**
     * Adds a full-width block with an optional [title] and [subtitle].
     *
     * @since %CURRENT_VERSION%
     */
    fun block(title: String? = null, subtitle: String? = null, content: BlockScope.() -> Unit): BlockHandle =
        place(BlockHeading(title, subtitle), highlight = false, content)

    /**
     * Adds a full-width dark block that highlights one thing, such as a running timer.
     *
     * @since %CURRENT_VERSION%
     */
    fun highlightBlock(title: String? = null, subtitle: String? = null, content: BlockScope.() -> Unit): BlockHandle =
        place(BlockHeading(title, subtitle), highlight = true, content)

    private fun place(heading: BlockHeading, highlight: Boolean, content: BlockScope.() -> Unit): BlockHandle {
        val block = buildBlock(texts, heading, highlight, span = null, columns = GRID_COLUMNS, content)
        main.add(block.component)
        return block
    }
}
