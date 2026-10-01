package tech.testsys.web.components.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Main
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.UiTexts
import tech.testsys.web.components.navigation.PageHeadScope

/**
 * Scope of a page body: the page head, rows and full-width blocks stacked vertically with the standard gap.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class PageScope internal constructor(
    private val main: Main,
    private val texts: UiTexts,
    private val view: Class<out Component>?,
    private val placeHead: (Component) -> Unit,
) {
    private var isHeadAllowed = true

    /**
     * Adds the page head under the Cabinet header: breadcrumbs that end with [title], the title with badges, notes and
     * actions, and the tabs of the sections of one object.
     *
     * @throws IllegalStateException if it is not the first call of the page body or is repeated.
     * @since %CURRENT_VERSION%
     */
    fun head(title: String, content: PageHeadScope.() -> Unit = {}) {
        check(isHeadAllowed) { "head() must be the first call of the page body and made once" }
        isHeadAllowed = false
        placeHead(PageHeadScope(texts, view).apply(content).build(title))
    }

    /**
     * Adds a row of the 24-column page grid filled with slots.
     *
     * @since %CURRENT_VERSION%
     */
    fun row(content: PageRowScope.() -> Unit) {
        isHeadAllowed = false
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
        isHeadAllowed = false
        val block = buildBlock(texts, heading, highlight, span = null, columns = GRID_COLUMNS, content)
        main.add(block.component)
        return block
    }
}
