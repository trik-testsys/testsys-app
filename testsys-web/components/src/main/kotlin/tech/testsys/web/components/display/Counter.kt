@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.display

import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.TextHandle
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.data.formatNumber
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.texts.UiTexts

/**
 * Meaning of a counter; the colour of the counter follows it.
 *
 * @since %CURRENT_VERSION%
 */
enum class CounterKind(internal val cssClass: CssClass) {
    Attention(CssClass.CounterDanger),
    Neutral(CssClass.CounterMuted),
}

/**
 * Adds a small round counter, e.g. of unread messages.
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.counter(value: Int, kind: CounterKind = CounterKind.Attention): TextHandle =
    counterHandle(buildCounter(value, kind, texts).also { counter -> add(counter) })

/**
 * Adds a small round counter on [size] columns of the row, or on the rest of it.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.counter(value: Int, kind: CounterKind = CounterKind.Attention, size: Int? = null): TextHandle =
    counterHandle(place(size, buildCounter(value, kind, texts)))

/** Builds the `.ts-counter` markup showing [value] with the number format of [texts], coloured by [kind]. */
internal fun buildCounter(value: Int, kind: CounterKind, texts: UiTexts): Span =
    Span(formatNumber(value, texts)).apply { addClassNames(CssClass.Counter, kind.cssClass) }

private fun counterHandle(counter: Span): TextHandle = TextHandle(counter, counter)
