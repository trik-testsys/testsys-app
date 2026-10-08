@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.display

import com.vaadin.flow.component.html.Div
import tech.testsys.web.components.FieldHandle
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
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
fun BlockRowScope.field(label: String, labelSize: Int, size: Int, content: ContentScope.() -> Unit): FieldHandle {
    val value = Div().apply { addClassName(CssClass.FieldContent) }
    ContentScope(value, texts, Placement.Body).content()
    val parts = placeField(label = label, labelSize = labelSize, size = size, value = value, labelAction = null)
    return FieldHandle(component = parts.field, valueArea = parts.valueCell)
}
