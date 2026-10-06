package tech.testsys.infra.diagnostics.internal.polygon

import tech.testsys.infra.diagnostics.internal.InternalDiagnosticsApi

@InternalDiagnosticsApi
internal fun Polygon.elements(): List<Located<PolygonElement>> = constraints.elements.flatMap { element -> element.descendants() }

@InternalDiagnosticsApi
private fun Located<PolygonElement>.descendants(): List<Located<PolygonElement>> {
    val children = when (val element = value) {
        is PolygonElement.Event -> element.body
        is PolygonElement.Message -> element.replacements
        is PolygonElement.Construct -> element.children
        is PolygonElement.TimeLimit, is PolygonElement.EventReference -> emptyList()
    }
    return listOf(this) + children.flatMap { child -> child.descendants() }
}
