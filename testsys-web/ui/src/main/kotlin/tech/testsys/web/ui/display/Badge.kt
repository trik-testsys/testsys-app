package tech.testsys.web.ui.display

import com.vaadin.flow.component.Text
import com.vaadin.flow.component.html.Span
import tech.testsys.web.ui.ElementHandle
import tech.testsys.web.ui.layout.BlockRowScope
import tech.testsys.web.ui.layout.ContentScope

/**
 * Adds a pill badge of a state with the page [text], coloured by its [tone].
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.badge(text: String, tone: Tone): ElementHandle = ElementHandle(buildBadge(text, tone).also { badge -> add(badge) })

/**
 * Adds a pill badge of a state on [size] columns of the row, or on the rest of it.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.badge(text: String, tone: Tone, size: Int? = null): ElementHandle = ElementHandle(place(size, buildBadge(text, tone)))

private fun buildBadge(text: String, tone: Tone): Span = Span().apply {
    addClassNames("ts-status", "ts-status--${tone.modifier}")
    add(Span().apply { addClassName("ts-status__dot") }, Text(text))
}
