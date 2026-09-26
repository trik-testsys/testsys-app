package tech.testsys.web.ui.layout

import com.vaadin.flow.component.html.Div
import tech.testsys.web.ui.UiTexts
import tech.testsys.web.ui.actions.action
import tech.testsys.web.ui.actions.mainAction
import tech.testsys.web.ui.core.IconName

/** Edit, cancel and save actions of a block head; they switch the edit mode of the block. */
internal class EditingSwitch(private val onSave: () -> Boolean, private val onCancel: () -> Unit) {
    /** Adds the actions to the end of the head [bar] and shows the ones of the current mode of [state]. */
    fun install(bar: Div, texts: UiTexts, state: BlockEditState) {
        val head = ContentScope(bar, texts, Placement.Head)
        val start = head.action(texts.editing.start, icon = IconName.Pencil) { onClick { state.isEditable = true } }
        val cancel = head.action(texts.editing.cancel) {
            onClick {
                onCancel()
                state.isEditable = false
            }
        }
        val save = head.mainAction(texts.editing.save) { onClick { if (onSave()) state.isEditable = false } }
        state.follow { editable ->
            start.isVisible = !editable
            cancel.isVisible = editable
            save.isVisible = editable
        }
    }
}
