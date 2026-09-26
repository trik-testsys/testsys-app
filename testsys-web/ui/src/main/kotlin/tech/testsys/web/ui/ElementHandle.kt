package tech.testsys.web.ui

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasText

/**
 * Handle of a design system element that a page can show or hide after building it.
 *
 * @property isVisible whether the element is shown.
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
open class ElementHandle internal constructor(internal val component: Component) {
    var isVisible: Boolean
        get() = component.isVisible
        set(value) {
            component.isVisible = value
        }
}

/**
 * Handle of an element whose text a page replaces, e.g. a stat card value or a counter.
 *
 * @property text the shown text.
 * @since %CURRENT_VERSION%
 */
class TextHandle internal constructor(private val holder: HasText, component: Component) : ElementHandle(component) {
    var text: String
        get() = holder.text
        set(value) {
            holder.text = value
        }
}
