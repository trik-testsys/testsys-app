@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.feedback

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.notification.Notification
import tech.testsys.web.components.core.ElementRole
import tech.testsys.web.components.core.ICON_SIZE_SMALL
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.setRole
import tech.testsys.web.components.core.svgIcon

/** How long a toast stays on screen. */
internal const val TOAST_DURATION_MS: Int = 4000

/**
 * Shows a toast of [kind] with a [title] and an optional [description] in the bottom right corner of the current UI;
 * call it from event handlers.
 *
 * @since %CURRENT_VERSION%
 */
fun toast(kind: FeedbackKind, title: String, description: String? = null) {
    val icon = Span(svgIcon(kind.toastIcon, ICON_SIZE_SMALL)).apply { addClassName("ts-toast__icon") }
    val text = Div(Span(title).apply { addClassName("ts-toast__title") }).apply {
        addClassName("ts-toast__text")
        description?.let { value -> add(Span(value).apply { addClassName("ts-toast__desc") }) }
    }
    val card = Div(icon, text).apply {
        addClassNames("ts-toast", "ts-toast--${kind.toastTone}")
        element.setRole(ElementRole.Status)
    }
    Notification(card).apply {
        duration = TOAST_DURATION_MS
        position = Notification.Position.BOTTOM_END
        element.themeList.add("ts-toast")
        open()
    }
}
