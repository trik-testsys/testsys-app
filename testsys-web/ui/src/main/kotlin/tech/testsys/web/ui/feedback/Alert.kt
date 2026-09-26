package tech.testsys.web.ui.feedback

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import tech.testsys.web.ui.ElementHandle
import tech.testsys.web.ui.core.ICON_SIZE_LARGE
import tech.testsys.web.ui.core.svgIcon
import tech.testsys.web.ui.layout.BlockRowScope
import tech.testsys.web.ui.layout.ContentScope

/**
 * Adds an inline message of [kind] with a [title] and an optional [text].
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.alert(kind: FeedbackKind, title: String, text: String? = null): ElementHandle =
    ElementHandle(buildAlert(kind, title, text).also { alert -> add(alert) })

/**
 * Adds an inline message of [kind] on [size] columns of the row, or on the rest of it.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.alert(kind: FeedbackKind, title: String, text: String? = null, size: Int? = null): ElementHandle =
    ElementHandle(place(size, buildAlert(kind, title, text)))

private fun buildAlert(kind: FeedbackKind, title: String, text: String?): Div {
    val icon = svgIcon(kind.alertIcon, ICON_SIZE_LARGE).apply { element.classList.add("ts-alert__icon") }
    val body = Div(Span(title).apply { addClassName("ts-alert__title") }).apply {
        addClassName("ts-alert__text")
        text?.let { description -> add(Span(description).apply { addClassName("ts-alert__desc") }) }
    }
    return Div(icon, body).apply {
        addClassNames("ts-alert", "ts-alert--${kind.alertTone}")
        if (kind == FeedbackKind.Error) element.setAttribute("role", "alert")
    }
}
