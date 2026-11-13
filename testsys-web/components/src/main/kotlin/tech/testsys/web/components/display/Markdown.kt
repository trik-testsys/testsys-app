package tech.testsys.web.components.display

import com.vaadin.flow.component.markdown.Markdown
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.layout.BlockRowScope

/**
 * Adds formatted text written in Markdown [content] on [size] columns of the row, or on the rest of it. The content
 * comes from the application, such as its resources: it is not meant for text entered by users.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.markdown(content: String, size: Int? = null): ElementHandle = ElementHandle(place(size, Markdown(content)))
