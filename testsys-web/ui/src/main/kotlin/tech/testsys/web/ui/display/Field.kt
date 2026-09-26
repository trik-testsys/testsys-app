package tech.testsys.web.ui.display

import com.vaadin.flow.component.html.Div
import tech.testsys.web.ui.ElementHandle
import tech.testsys.web.ui.layout.BlockRowScope
import tech.testsys.web.ui.layout.ContentScope
import tech.testsys.web.ui.layout.Placement
import tech.testsys.web.ui.layout.placeField

/**
 * Adds a field of [label] whose value is display [content] in a line, e.g. tags or a badge.
 * The label takes [labelSize] columns and the value [size] columns of the row.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.field(label: String, labelSize: Int, size: Int, content: ContentScope.() -> Unit): ElementHandle {
    val value = Div().apply { addClassName("ts-field__content") }
    ContentScope(value, texts, Placement.Body).content()
    return ElementHandle(placeField(label, labelSize, size, value, labelAction = null).field)
}
