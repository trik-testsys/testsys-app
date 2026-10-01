package tech.testsys.web.components.display

import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.TextHandle
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope

/**
 * Meaning of a counter; the colour of the counter follows it.
 *
 * @since %CURRENT_VERSION%
 */
enum class CounterKind(internal val tone: String) {
    Attention("danger"),
    Neutral("muted"),
}

/**
 * Adds a small round counter, e.g. of unread messages.
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.counter(value: Int, kind: CounterKind = CounterKind.Attention): TextHandle =
    counterHandle(buildCounter(value, kind).also { counter -> add(counter) })

/**
 * Adds a small round counter on [size] columns of the row, or on the rest of it.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.counter(value: Int, kind: CounterKind = CounterKind.Attention, size: Int? = null): TextHandle =
    counterHandle(place(size, buildCounter(value, kind)))

/** Builds the `.ts-counter` markup showing [value], coloured by [kind]. */
internal fun buildCounter(value: Int, kind: CounterKind): Span =
    Span(value.toString()).apply { addClassNames("ts-counter", "ts-counter--${kind.tone}") }

private fun counterHandle(counter: Span): TextHandle = TextHandle(counter, counter)
