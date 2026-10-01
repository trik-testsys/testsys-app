package tech.testsys.web.components

import com.vaadin.flow.component.Component
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.signals.Signal

/**
 * Handle of an application-controlled selection whose programmatic updates never invoke the user callback.
 *
 * @param T the immutable selection data.
 * @property data the displayed selection data.
 * @property isEnabled whether user interaction is allowed.
 * @since %CURRENT_VERSION%
 */
class SelectionHandle<T> internal constructor(component: Component, initial: T, render: (T) -> Unit) : ElementHandle(component) {
    private val state = Bindable(component.element, initial, render)
    private val enabled = Bindable(component.element, true) { value -> component.element.isEnabled = value }
    private var listener: (T) -> Unit = {}
    var data: T
        get() = state.value
        set(value) {
            state.value = value
        }
    var isEnabled: Boolean
        get() = enabled.value
        set(value) {
            enabled.value = value
        }

    /**
     * Binds selection data to [signal]; a user action then requests a change through the callback.
     *
     * @since %CURRENT_VERSION%
     */
    fun bindData(signal: Signal<T>): SignalBinding<T> = state.bind(signal)

    /**
     * Binds whether interaction is enabled to [signal].
     *
     * @since %CURRENT_VERSION%
     */
    fun bindEnabled(signal: Signal<Boolean>): SignalBinding<Boolean> = enabled.bind(signal)

    /**
     * Replaces the callback of a changed user selection with [listener].
     *
     * @since %CURRENT_VERSION%
     */
    fun onChange(listener: (T) -> Unit) {
        this.listener = listener
    }

    internal fun choose(value: T) {
        if (!isEnabled || value == data) return
        if (!state.isBound) state.value = value
        listener(value)
    }
}
