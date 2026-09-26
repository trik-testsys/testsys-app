package tech.testsys.web.ui.layout

import com.vaadin.flow.component.Component
import tech.testsys.web.ui.ElementHandle

/**
 * Handle of a block that a page changes after building it.
 *
 * @property isEditable whether the fields of the block can be edited; a field is editable only if it and its block are.
 * @since %CURRENT_VERSION%
 */
class BlockHandle internal constructor(component: Component, private val editState: BlockEditState) : ElementHandle(component) {
    var isEditable: Boolean
        get() = editState.isEditable
        set(value) {
            editState.isEditable = value
        }
}
