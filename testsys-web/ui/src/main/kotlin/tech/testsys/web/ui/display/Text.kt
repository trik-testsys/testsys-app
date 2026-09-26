package tech.testsys.web.ui.display

import com.vaadin.flow.component.html.Div
import tech.testsys.web.ui.TextHandle
import tech.testsys.web.ui.layout.BlockRowScope
import tech.testsys.web.ui.layout.ContentScope

/**
 * Adds a paragraph of plain text.
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.text(value: String): TextHandle = textHandle(Div(value).also { paragraph -> add(paragraph) })

/**
 * Adds a paragraph of plain text on [size] columns of the row, or on the rest of it.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.text(value: String, size: Int? = null): TextHandle = textHandle(place(size, Div(value)))

private fun textHandle(paragraph: Div): TextHandle = TextHandle(paragraph, paragraph)
