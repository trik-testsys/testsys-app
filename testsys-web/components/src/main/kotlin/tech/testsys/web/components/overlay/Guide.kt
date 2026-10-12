@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.overlay

import com.vaadin.flow.component.Component
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.setAttribute

/**
 * Additional page-guide steps shown after the explanation of their block, in declaration order.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class GuideStepsScope internal constructor() {
    internal val steps = mutableListOf<GuideStep>()

    /**
     * Explains [target] with plain [title] and [description]; the target must belong to this block when it is built.
     *
     * @throws IllegalArgumentException if either text is blank or the target is repeated.
     * @since %CURRENT_VERSION%
     */
    fun step(target: ElementHandle, title: String, description: String) {
        require(steps.none { step -> step.target === target.component }) { "Guide target '$title' is already configured" }
        steps += GuideStep(target.component, title = title, description = description)
    }
}

internal class GuideStep(val target: Component, val title: String, val description: String) {
    init {
        require(title.isNotBlank()) { "Guide title must not be blank" }
        require(description.isNotBlank()) { "Guide description for '$title' must not be blank" }
    }

    fun install(order: Int? = null) {
        target.element.setAttribute(HtmlAttribute.DataGuideTitle, title)
        target.element.setAttribute(HtmlAttribute.DataGuideDescription, description)
        order?.let { value -> target.element.setAttribute(HtmlAttribute.DataGuideOrder, value.toString()) }
    }
}

internal class BlockGuide(val title: String, val description: String, val steps: GuideStepsScope) {
    fun install(block: Component) {
        GuideStep(block, title = title, description = description).install()
        steps.steps.forEachIndexed { index, step ->
            require(generateSequence(step.target.element.parent) { parent -> parent.parent }.any { parent -> parent == block.element }) {
                "Guide target '${step.title}' must belong to block '$title'"
            }
            step.install(index)
        }
    }
}
