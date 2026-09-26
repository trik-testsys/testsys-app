package tech.testsys.web.ui.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.html.Div
import tech.testsys.web.ui.RawVaadin
import tech.testsys.web.ui.TestSysDsl
import tech.testsys.web.ui.UiTexts

/**
 * Scope of block content: a group in a block row, the head actions or the footer.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class ContentScope internal constructor(
    internal val container: HasComponents,
    internal val texts: UiTexts,
    internal val placement: Placement,
) {
    /**
     * Lays out [content] in a line with the standard gap, wrapping it when it does not fit.
     *
     * @since %CURRENT_VERSION%
     */
    fun horizontal(content: ContentScope.() -> Unit) {
        group("ts-hstack", content)
    }

    /**
     * Lays out [content] in a column with the standard gap.
     *
     * @since %CURRENT_VERSION%
     */
    fun vertical(content: ContentScope.() -> Unit) {
        group("ts-vstack", content)
    }

    /**
     * Adds a raw Vaadin [component] that the design system does not cover yet.
     *
     * @since %CURRENT_VERSION%
     */
    @RawVaadin
    fun custom(component: Component) {
        container.add(component)
    }

    internal fun add(component: Component) {
        container.add(component)
    }

    private fun group(cssClass: String, content: ContentScope.() -> Unit) {
        val group = Div().apply { addClassName(cssClass) }
        container.add(group)
        ContentScope(group, texts, placement).content()
    }
}

/** Place of content inside a block; controls take a smaller size in the head. */
internal enum class Placement {
    Body,
    Head,
}
