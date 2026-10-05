@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.html.Div
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.texts.UiTexts

/**
 * Scope of a flow of content without sizes: head, footer and row groups of a block, the value of a field, table cells,
 * dialog footers, page head actions and empty state actions.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class ContentScope internal constructor(
    internal val container: HasComponents,
    internal val texts: UiTexts,
    internal val placement: Placement,
    internal val gridColumns: Int,
) {
    /**
     * Lays out [content] in a line with the standard gap, wrapping it when it does not fit.
     *
     * @since %CURRENT_VERSION%
     */
    fun horizontal(content: ContentScope.() -> Unit) {
        group(CssClass.Hstack, content)
    }

    /**
     * Lays out [content] in a column with the standard gap.
     *
     * @since %CURRENT_VERSION%
     */
    fun vertical(content: ContentScope.() -> Unit) {
        group(CssClass.Vstack, content)
    }

    internal fun add(component: Component) {
        container.add(component)
    }

    private fun group(cssClass: CssClass, content: ContentScope.() -> Unit) {
        val group = Div().apply { addClassName(cssClass) }
        container.add(group)
        ContentScope(group, texts, placement, gridColumns).content()
    }
}

/** Place of content; controls of an [isCompact] place take the small size. */
internal enum class Placement(val isCompact: Boolean) {
    Body(isCompact = false),
    Head(isCompact = true),
    PageHead(isCompact = false),
    Empty(isCompact = true),
    Cell(isCompact = true),
}
