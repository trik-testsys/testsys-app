@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.overlay

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.page.PendingJavaScriptResult
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.dom.Element
import tech.testsys.web.components.actions.ActionHandle
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.dangerAction
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.core.CssUnit
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.setWidth
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.Placement
import tech.testsys.web.components.texts.currentUiLocalization

private const val DANGER_COOLDOWN_SECONDS = 3

/**
 * Asks to confirm an action: opens a dialog with [title], [text] and the [action] that runs [onConfirm] and closes it.
 * [isDanger] marks an irreversible action, whose button ignores clicks for the first seconds; [typeToConfirm] requires
 * the name with surrounding whitespace ignored on both sides.
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
    val texts = currentUiLocalization()
    val shell = DialogShell(texts, title, subtitle = null, size = DialogSize.S, isAlert = isDanger)
    text?.let { description -> shell.content.add(Div(description)) }

    val confirmName = typeToConfirm?.let { name ->
        TextField().apply {
            label = texts.messages.uiOverlay.dialogTypeToConfirm(name = name)
            valueChangeMode = ValueChangeMode.EAGER
            isAutofocus = true
            style.setWidth(value = 100, unit = CssUnit.Percent)
        }.also { field -> shell.content.add(field) }
    }

    val foot = ContentScope(shell.foot, texts, Placement.Body)
    foot.action(texts.messages.uiOverlay.dialogCancel()) { onClick { shell.close() } }

    val run: ActionHandle.() -> Unit = {
        onClick {
            onConfirm()
            shell.close()
        }
    }

    val confirmAction = if (isDanger) foot.dangerAction(action, run) else foot.mainAction(action, configure = run)
    confirmName?.let { field ->
        confirmAction.isEnabled = false
        field.addValueChangeListener { event -> confirmAction.isEnabled = event.value.trim() == typeToConfirm.trim() }
    }

    if (isDanger) confirmAction.button.element.startCooldown()
    shell.open()
    confirmName?.focus()
}

/**
 * Makes the button inert on the client for [DANGER_COOLDOWN_SECONDS], showing the seconds left, so a hasty click
 * cannot confirm. The server state is untouched: the type-to-confirm check may still enable the button meanwhile.
 */
private fun Element.startCooldown(): PendingJavaScriptResult = executeJs(
    """
    let left = $0;
    this.inert = true;
    this.setAttribute('data-ts-cooldown', left);
    const tick = setInterval(() => {
      left -= 1;
      if (left > 0) { this.setAttribute('data-ts-cooldown', left); return; }
      clearInterval(tick);
      this.removeAttribute('data-ts-cooldown');
      this.inert = false;
    }, 1000);
    """.trimIndent(),
    DANGER_COOLDOWN_SECONDS,
)
