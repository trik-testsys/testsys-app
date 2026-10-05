@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.display

import com.vaadin.flow.component.Text
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope

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

internal fun buildBadge(text: String, tone: Tone): Span = Span().apply {
    addClassNames(CssClass.Status, tone.statusClass)
    add(Span().apply { addClassName(CssClass.StatusDot) }, Text(text))
}

private val Tone.statusClass: CssClass
    get() = when (this) {
        Tone.Neutral -> CssClass.StatusNeutral
        Tone.Info -> CssClass.StatusInfo
        Tone.Success -> CssClass.StatusSuccess
        Tone.Warning -> CssClass.StatusWarning
        Tone.Danger -> CssClass.StatusDanger
    }
