package tech.testsys.web.components.display

import com.vaadin.flow.component.html.Div
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.Placement
import tech.testsys.web.components.layout.placeField

/**
 * Adds a field of [label] whose value is display [content] in a line, e.g. tags or a badge.
 * The label takes [labelSize] columns and the value [size] columns of the row.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.field(label: String, labelSize: Int, size: Int, content: ContentScope.() -> Unit): ElementHandle {
    val value = Div().apply { addClassName("ts-field__content") }
    ContentScope(value, texts, Placement.Body, size).content()
    return ElementHandle(placeField(label, labelSize, size, value, labelAction = null).field)
}
