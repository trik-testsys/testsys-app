@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components

import com.vaadin.flow.component.Component
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.Signal
import tech.testsys.web.components.core.Bindable
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.getAttribute
import tech.testsys.web.components.core.removeAttribute
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setObscured

/**
 * Handle of a field whose value can be visually obscured independently of visibility and editability.
 *
 * @property isObscured whether the value is blurred until its area is hovered or focused; a manual change while
 * [bindObscured] is bound, and a second binding, throw [BindingActiveException].
 * @since %CURRENT_VERSION%
 */
open class FieldHandle internal constructor(component: Component, private val valueArea: Component) : ElementHandle(component) {
    private val originalTabIndex = valueArea.element.getAttribute(HtmlAttribute.TabIndex)
    private val obscured = Bindable(component.element, initial = false) { value ->
        valueArea.element.setObscured(value)
        if (value) {
            valueArea.element.setAttribute(HtmlAttribute.TabIndex, ObscuredFocus.TAB_INDEX)
        } else if (originalTabIndex == null) {
            valueArea.element.removeAttribute(HtmlAttribute.TabIndex)
        } else {
            valueArea.element.setAttribute(HtmlAttribute.TabIndex, originalTabIndex)
        }
    }

    var isObscured: Boolean
        get() = obscured.value
        set(value) {
            obscured.value = value
        }

    /**
     * Binds [isObscured] to [signal] without changing the value, visibility or edit mode.
     *
     * @since %CURRENT_VERSION%
     */
    fun bindObscured(signal: Signal<Boolean>): SignalBinding<Boolean> = obscured.bind(signal)
}

private object ObscuredFocus {
    const val TAB_INDEX = "0"
}
