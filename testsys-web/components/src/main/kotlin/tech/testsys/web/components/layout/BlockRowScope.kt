@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.html.Div
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.UiTexts
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.setGridColumnSpan

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
    fun horizontal(size: Int? = null, content: ContentScope.() -> Unit): ElementHandle = group(CssClass.Hstack, size, content)

    /**
     * Lays out [content] in a column on [size] columns, or on the rest of the row if [size] is `null`.
     *
     * @since %CURRENT_VERSION%
     */
    fun vertical(size: Int? = null, content: ContentScope.() -> Unit): ElementHandle = group(CssClass.Vstack, size, content)

    /** Places [component] on its assigned fractions. */
    internal fun <C : Component> place(size: Int?, component: C): C = placeWithSize(size) { component }.component

    /** Resolves the assignment before building its component, keeping its capacity as semantic data. */
    internal fun <C : Component> placeWithSize(size: Int?, create: (Int) -> C): GridPlacement<C> {
        val columns = if (size == null) track.takeRest() else size.also { taken -> track.take(taken) }
        val component = create(columns)
        component.element.style.setGridColumnSpan(columns)
        row.add(component)
        return GridPlacement(component, columns)
    }

    internal fun <C> placeContent(size: Int?, container: C): ContentScope where C : Component, C : HasComponents {
        val placed = placeWithSize(size) { container }
        return ContentScope(placed.component, texts, Placement.Body, placed.columns)
    }

    private fun group(cssClass: CssClass, size: Int?, content: ContentScope.() -> Unit): ElementHandle {
        val placed = placeWithSize(size) { Div().apply { addClassName(cssClass) } }
        ContentScope(placed.component, texts, Placement.Body, placed.columns).content()
        return ElementHandle(placed.component)
    }
}

/** Component and the logical fractions assigned to it by its row. */
internal class GridPlacement<C : Component>(val component: C, val columns: Int)
