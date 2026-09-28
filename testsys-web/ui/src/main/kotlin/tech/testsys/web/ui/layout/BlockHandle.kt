package tech.testsys.web.ui.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.Signal
import tech.testsys.web.ui.ElementHandle

/**
 * Handle of a block that a page changes after building it.
 *
 * @property isEditable whether the fields of the block can be edited; a field is editable only if it and its block are.
 * A manual change while [bindEditable] is bound, and a second binding, throw [BindingActiveException].
 * @since %CURRENT_VERSION%
 */
class BlockHandle internal constructor(
    component: Component,
    private val editState: BlockEditState,
    private val hasEditingSwitch: Boolean,
) : ElementHandle(component) {
    var isEditable: Boolean
        get() = editState.isEditable
        set(value) {
            editState.isEditable = value
        }

    /**
     * Binds [isEditable] to [signal]: the fields of the block follow every value it produces at once. A manual
     * [isEditable] while bound, and a second binding, throw [BindingActiveException].
     *
     * @throws IllegalStateException if the block has an editing switch, which drives its mode itself.
     * @since %CURRENT_VERSION%
     */
    fun bindEditable(signal: Signal<Boolean>): SignalBinding<Boolean> {
        check(!hasEditingSwitch) { "Block has an editing switch that drives its mode; bind the mode or call editing(), not both" }
        return editState.bind(signal)
    }
}
