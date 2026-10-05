package tech.testsys.web.components.core

import com.vaadin.flow.dom.Element
import com.vaadin.flow.dom.ElementEffect
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.Signal

/**
 * State kept by a handle rather than by a Vaadin component, set by the page or bound to a signal of [element]:
 * [apply] shows every new value. While bound, a manual value and a second binding throw [BindingActiveException],
 * as Vaadin bindings do. The initial value is not applied by the constructor, as the caller already built the
 * component in that state.
 *
 * @param T the type of the value.
 */
internal class Bindable<T>(private val element: Element, initial: T, private val apply: (T) -> Unit) {
    private var current: T = initial

    /** Whether [bind] is active. */
    var isBound: Boolean = false
        private set

    /** The current value; setting it while [bind] is active throws [BindingActiveException]. */
    var value: T
        get() = current
        set(newValue) {
            if (isBound) throw BindingActiveException()
            show(newValue)
        }

    /** Binds [signal]: every value it produces is shown by [apply]. A second call throws [BindingActiveException]. */
    fun bind(signal: Signal<T>): SignalBinding<T> = bind(signal) { value -> value }

    /**
     * Binds [signal]: every value it produces is shown by [apply] as [toValue] maps it, while the returned binding
     * reports the values of [signal] itself. A second call throws [BindingActiveException].
     */
    fun <S> bind(signal: Signal<S>, toValue: (S) -> T): SignalBinding<S> {
        if (isBound) throw BindingActiveException()
        isBound = true
        return ElementEffect.bind(element, signal) { _, source -> show(toValue(source)) }
    }

    /** Keeps [newValue] as current while [apply] shows it, and restores the previous one if [apply] rejects it. */
    private fun show(newValue: T) {
        val previous = current
        current = newValue
        try {
            apply(newValue)
        } catch (rejected: IllegalArgumentException) {
            current = previous
            throw rejected
        }
    }
}
