package tech.testsys.web.components.display

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.Signal
import tech.testsys.web.components.TextHandle
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope

/**
 * Adds a paragraph of plain text.
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.text(value: String): TextHandle = textHandle(Div(value).also { paragraph -> add(paragraph) })

/**
 * Adds a paragraph of text that follows [signal]; a manual [TextHandle.text] afterwards throws
 * [BindingActiveException].
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.text(signal: Signal<String>): TextHandle = bindTextHandle(Div().also { paragraph -> add(paragraph) }, signal)

/**
 * Adds a paragraph of plain text on [size] columns of the row, or on the rest of it.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.text(value: String, size: Int? = null): TextHandle = textHandle(place(size, Div(value)))

/**
 * Adds a paragraph on [size] columns of the row, or on the rest of it, that follows [signal]; a manual
 * [TextHandle.text] afterwards throws [BindingActiveException].
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.text(signal: Signal<String>, size: Int? = null): TextHandle = bindTextHandle(place(size, Div()), signal)

private fun textHandle(paragraph: Div): TextHandle = TextHandle(paragraph)

private fun bindTextHandle(paragraph: Div, signal: Signal<String>): TextHandle =
    textHandle(paragraph).also { handle -> handle.bindText(signal) }
