package tech.testsys.web.ui.layout

/** Whether the fields of one block can be edited; the fields and the editing switch of the block follow it. */
internal class BlockEditState {
    private val listeners = mutableListOf<(Boolean) -> Unit>()

    var isEditable: Boolean = true
        set(value) {
            field = value
            listeners.forEach { listener -> listener(value) }
        }

    /** Calls [listener] with the current value now and with the new value on every change. */
    fun follow(listener: (Boolean) -> Unit) {
        listeners += listener
        listener(isEditable)
    }
}
