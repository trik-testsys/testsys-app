@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.actions

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.Signal
import tech.testsys.web.components.Bindable
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.setAriaBusy

/**
 * Handle of an action button.
 *
 * @property isEnabled whether the action can be clicked.
 * @property isLoading whether the action shows a spinner and ignores clicks, keeping its look; a manual change while
 * bound, and a second binding, throw [BindingActiveException].
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class ActionHandle internal constructor(internal val button: Button, private val icon: Component?) : ElementHandle(button) {
    private val loading = Bindable(button.element, initial = false) { value ->
        button.icon = if (value) Span().apply { addClassName(CssClass.Spinner) } else icon
        if (value) button.element.setAriaBusy(true) else button.element.setAriaBusy(null)
    }

    var isEnabled: Boolean
        get() = button.isEnabled
        set(value) {
            button.isEnabled = value
        }

    var isLoading: Boolean
        get() = loading.value
        set(value) {
            loading.value = value
        }

    /**
     * Binds [isEnabled] to [signal]: every value it produces is applied at once. A manual [isEnabled] while bound,
     * and a second binding, throw [BindingActiveException].
     *
     * @since %CURRENT_VERSION%
     */
    fun bindEnabled(signal: Signal<Boolean>): SignalBinding<Boolean> = button.bindEnabled(signal)

    /**
     * Binds [isLoading] to [signal]: every value it produces is shown at once. A manual [isLoading] while bound,
     * and a second binding, throw [BindingActiveException].
     *
     * @since %CURRENT_VERSION%
     */
    fun bindLoading(signal: Signal<Boolean>): SignalBinding<Boolean> = loading.bind(signal)

    /**
     * Runs [listener] when the action is clicked while it is not loading.
     *
     * @since %CURRENT_VERSION%
     */
    fun onClick(listener: () -> Unit) {
        button.addClickListener { _ -> if (!loading.value) listener() }
    }
}
