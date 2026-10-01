package tech.testsys.web.components

import com.vaadin.flow.component.Component
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.signals.Signal

/**
 * Handle of a display updated with immutable application data or a signal.
 *
 * @param T the type of display data.
 * @property data the currently presented data; manual changes while bound throw the usual binding exception.
 * @since %CURRENT_VERSION%
 */
class DataHandle<T> internal constructor(component: Component, initial: T, render: (T) -> Unit) : ElementHandle(component) {
    private val state = Bindable(component.element, initial, render)
    var data: T
        get() = state.value
        set(value) {
            state.value = value
        }

    /**
     * Binds the displayed data to [signal] while the component is attached.
     *
     * @since %CURRENT_VERSION%
     */
    fun bindData(signal: Signal<T>): SignalBinding<T> = state.bind(signal)
}
