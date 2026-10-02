package tech.testsys.web.components.layout

import com.vaadin.flow.dom.Element
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.shared.Registration
import com.vaadin.flow.signals.Signal
import tech.testsys.web.components.Bindable

/**
 * Whether the fields of one block can be edited, set by the page or bound to a signal of the block [element];
 * the fields and the editing switch of the block follow it.
 */
internal class BlockEditState(element: Element) {
    private val listeners = mutableListOf<(Boolean) -> Unit>()
    private val editable =
        Bindable(element, initial = true) { value -> listeners.forEach { listener -> listener(value) } }

    var isEditable: Boolean
        get() = editable.value
        set(value) {
            editable.value = value
        }

    /** Binds [isEditable] to [signal]; a manual value while bound, and a second binding, throw. */
    fun bind(signal: Signal<Boolean>): SignalBinding<Boolean> = editable.bind(signal)

    /** Calls [listener] with the current value now and with the new value on every change, until removed. */
    fun follow(listener: (Boolean) -> Unit): Registration {
        listeners += listener
        listener(isEditable)
        return Registration { listeners -= listener }
    }
}
