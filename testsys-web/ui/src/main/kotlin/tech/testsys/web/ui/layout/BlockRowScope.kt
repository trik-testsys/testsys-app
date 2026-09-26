package tech.testsys.web.ui.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Div
import tech.testsys.web.ui.ElementHandle
import tech.testsys.web.ui.TestSysDsl
import tech.testsys.web.ui.UiTexts

/**
 * Scope of a block row: elements side by side on the block columns; an element without a size takes the rest of the row.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class BlockRowScope internal constructor(
    private val row: Div,
    columns: Int,
    internal val texts: UiTexts,
    internal val editState: BlockEditState,
) {
    private val track = GridTrack(columns, owner = "Block row")

    /**
     * Lays out [content] in a line on [size] columns, or on the rest of the row if [size] is `null`.
     *
     * @since %CURRENT_VERSION%
     */
    fun horizontal(size: Int? = null, content: ContentScope.() -> Unit): ElementHandle = group("ts-hstack", size, content)

    /**
     * Lays out [content] in a column on [size] columns, or on the rest of the row if [size] is `null`.
     *
     * @since %CURRENT_VERSION%
     */
    fun vertical(size: Int? = null, content: ContentScope.() -> Unit): ElementHandle = group("ts-vstack", size, content)

    /** Places [component] on [size] columns, or on the rest of the row if [size] is `null`, and returns it. */
    internal fun <C : Component> place(size: Int?, component: C): C {
        val columns = if (size == null) track.takeRest() else size.also { taken -> track.take(taken) }
        component.element.style.set("grid-column", "span $columns")
        row.add(component)
        return component
    }

    private fun group(cssClass: String, size: Int?, content: ContentScope.() -> Unit): ElementHandle {
        val group = place(size, Div().apply { addClassName(cssClass) })
        ContentScope(group, texts, Placement.Body).content()
        return ElementHandle(group)
    }
}
