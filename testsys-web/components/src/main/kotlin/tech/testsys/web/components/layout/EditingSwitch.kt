@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.layout

import com.vaadin.flow.component.dependency.JsModule
import com.vaadin.flow.component.html.Div
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.texts.UiTexts

private const val EDITING_FOCUS_MODULE = "./testsys-ui/editing-focus.ts"

/** Edit, cancel and save actions of a block head; they switch the edit mode of the block. */
internal class EditingSwitch(private val onSave: () -> Boolean, private val onCancel: () -> Unit) {
    /** Adds the actions to the end of the head [bar] and shows the ones of the current mode of [state]. */
    fun install(bar: Div, texts: UiTexts, state: BlockEditState, body: Div) {
        val actions = EditingActions().also { container -> bar.add(container) }
        val head = ContentScope(actions, texts, Placement.Head)
        val start = head.action(texts.editing.start, icon = IconName.Pencil)
        val cancel = head.action(texts.editing.cancel) {
            onClick {
                onCancel()
                state.isEditable = false
            }
        }
        start.onClick {
            state.isEditable = true
            focusFirstEditableInput(body = body, fallback = cancel.button)
        }
        val save = head.mainAction(texts.editing.save) { onClick { if (onSave()) state.isEditable = false } }
        state.follow { editable ->
            start.isVisible = !editable
            cancel.isVisible = editable
            save.isVisible = editable
        }
    }
}

/** Layout-transparent container of the switch actions that loads the client focus search with them. */
@JsModule(EDITING_FOCUS_MODULE)
internal class EditingActions : Div() {
    init {
        addClassName(CssClass.BlockEditing)
    }
}
