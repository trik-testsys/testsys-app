package tech.testsys.web.ui.actions

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Span
import tech.testsys.web.ui.ElementHandle
import tech.testsys.web.ui.TestSysDsl

/**
 * Handle of an action button.
 *
 * @property isEnabled whether the action can be clicked.
 * @property isLoading whether the action shows a spinner and ignores clicks, keeping its look.
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class ActionHandle internal constructor(internal val button: Button, private val icon: Component?) : ElementHandle(button) {
    var isEnabled: Boolean
        get() = button.isEnabled
        set(value) {
            button.isEnabled = value
        }

    var isLoading: Boolean = false
        set(value) {
            field = value
            button.icon = if (value) Span().apply { addClassName("ts-spinner") } else icon
            if (value) button.element.setAttribute("aria-busy", "true") else button.element.removeAttribute("aria-busy")
        }

    /**
     * Runs [listener] when the action is clicked while it is not loading.
     *
     * @since %CURRENT_VERSION%
     */
    fun onClick(listener: () -> Unit) {
        button.addClickListener { _ -> if (!isLoading) listener() }
    }
}
