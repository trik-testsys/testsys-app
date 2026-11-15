@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.feedback

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.notification.Notification
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.CssTheme
import tech.testsys.web.components.core.ElementRole
import tech.testsys.web.components.core.ElementType
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.ICON_SIZE_SMALL
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.add
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setRole
import tech.testsys.web.components.core.setType
import tech.testsys.web.components.core.svgIcon
import tech.testsys.web.components.texts.currentUiLocalization

/** How long a toast other than an error stays on screen. */
internal const val TOAST_DURATION_MS: Int = 4000

/** Vaadin duration of a notification that stays until it is closed. */
private const val UNTIL_CLOSED = 0

/**
 * Shows a toast of [kind] with a [title] and an optional [description] in the bottom right corner of the current UI;
 * call it from event handlers. Every toast has a close button. An error stays until it is closed and is announced
 * at once; the other kinds leave by themselves.
 *
 * @throws IllegalStateException if there is no current UI or no localization is bound to it.
 * @since 1.0.0-beta.1
 */
fun toast(kind: FeedbackKind, title: String, description: String? = null) {
    val icon = Span(svgIcon(kind.toastIcon, ICON_SIZE_SMALL)).apply { addClassName(CssClass.ToastIcon) }
    val text = Div(Span(title).apply { addClassName(CssClass.ToastTitle) }).apply {
        addClassName(CssClass.ToastText)
        description?.let { value -> add(Span(value).apply { addClassName(CssClass.ToastDesc) }) }
    }

    val isError = kind == FeedbackKind.Error
    val notification = Notification()
    val close = NativeButton().apply {
        addClassName(CssClass.ToastClose)
        element.setAttribute(HtmlAttribute.AriaLabel, currentUiLocalization().messages.uiOverlay.dialogClose())
        element.setType(ElementType.Button)
        add(svgIcon(IconName.X, ICON_SIZE_SMALL))
        addClickListener { notification.close() }
    }
    val card = Div(icon, text, close).apply {
        addClassNames(CssClass.Toast, kind.toastClass)
        element.setRole(if (isError) ElementRole.Alert else ElementRole.Status)
    }

    notification.apply {
        add(card)
        duration = if (isError) UNTIL_CLOSED else TOAST_DURATION_MS
        isAssertive = isError
        position = Notification.Position.BOTTOM_END
        element.themeList.add(CssTheme.Toast)
        open()
    }
}
