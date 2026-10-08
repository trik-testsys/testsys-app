@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.overlay

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.value.ValueChangeMode
import tech.testsys.web.components.actions.ActionHandle
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.dangerAction
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.core.CssUnit
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.setWidth
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.Placement
import tech.testsys.web.components.texts.currentTexts

/**
 * Asks to confirm an action: opens a dialog with [title], [text] and the [action] that runs [onConfirm] and closes it.
 * [isDanger] marks an irreversible action; [typeToConfirm] also requires typing this name. Call it from event handlers.
 *
 * @since %CURRENT_VERSION%
 */
fun confirm(
    title: String,
    text: String? = null,
    action: String,
    isDanger: Boolean = false,
    typeToConfirm: String? = null,
    onConfirm: () -> Unit,
) {
    val texts = currentTexts()
    val shell = DialogShell(texts, title, subtitle = null, size = DialogSize.S, isAlert = isDanger)
    text?.let { description -> shell.content.add(Div(description)) }

    val confirmName = typeToConfirm?.let { name ->
        TextField().apply {
            label = texts.dialog.typeToConfirm(name)
            valueChangeMode = ValueChangeMode.EAGER
            isAutofocus = true
            style.setWidth(value = 100, unit = CssUnit.Percent)
        }.also { field -> shell.content.add(field) }
    }

    val foot = ContentScope(shell.foot, texts, Placement.Body)
    foot.action(texts.dialog.cancel) { onClick { shell.close() } }

    val run: ActionHandle.() -> Unit = {
        onClick {
            onConfirm()
            shell.close()
        }
    }

    val confirmAction = if (isDanger) foot.dangerAction(action, run) else foot.mainAction(action, configure = run)
    confirmName?.let { field ->
        confirmAction.isEnabled = false
        field.addValueChangeListener { event -> confirmAction.isEnabled = event.value.trim() == typeToConfirm }
    }

    shell.open()
    confirmName?.focus()
}
