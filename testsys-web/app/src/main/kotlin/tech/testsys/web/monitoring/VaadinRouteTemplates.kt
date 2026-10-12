package tech.testsys.web.monitoring

import com.vaadin.flow.router.RouteData

/**
 * Matches request paths against Vaadin route templates and names the matched template without parameter values or
 * value patterns, for example `/developer/tasks/:taskId`. A path matching several templates takes the one with
 * the most literal segments, as the Vaadin router prefers static segments.
 */
internal class VaadinRouteTemplates(templates: Collection<String>) {
    private val routes = templates.distinct().map(::Template).sortedByDescending { template -> template.literals }

    /** Returns the label of the template matching [path], or `null` if no route matches it. */
    fun match(path: String): String? {
        val segments = path.split('/').filter { segment -> segment.isNotEmpty() }
        return routes.firstOrNull { template -> template.matches(segments) }?.label
    }

    private class Template(template: String) {
        private val segments = split(template).map(::Segment)

        val literals = segments.count { segment -> segment.literal != null }

        val label = "/" + segments.joinToString("/") { segment -> segment.label }

        fun matches(path: List<String>): Boolean = matches(path, segment = 0, position = 0)

        private fun matches(path: List<String>, segment: Int, position: Int): Boolean {
            if (segment == segments.size) return position == path.size

            val current = segments[segment]
            return when {
                current.isWildcard -> (position..path.size).any { end ->
                    path.subList(position, end).all(current::accepts) && matches(path, segment + 1, end)
                }
                current.isOptional && matches(path, segment + 1, position) -> true
                else -> position < path.size && current.accepts(path[position]) && matches(path, segment + 1, position + 1)
            }
        }
    }

    /** One template segment: a [literal], or a parameter with an optional or wildcard marker and a value pattern. */
    private class Segment(source: String) {
        val literal: String? = source.takeUnless { value -> value.startsWith(':') }

        private val declaration = source.removePrefix(":").substringBefore('(')

        private val pattern = source.substringAfter('(', "").takeIf { value -> value.isNotEmpty() }?.removeSuffix(")")?.let(::Regex)

        val isOptional = literal == null && declaration.endsWith('?')

        val isWildcard = literal == null && declaration.endsWith('*')

        val label: String = literal ?: ":$declaration"

        fun accepts(value: String): Boolean = if (literal != null) literal == value else pattern?.matches(value) != false
    }

    companion object {
        /** Collects the templates of [routes] and their aliases. */
        fun of(routes: List<RouteData>): VaadinRouteTemplates = VaadinRouteTemplates(
            routes.flatMap { route -> listOf(route.template) + route.routeAliases.map { alias -> alias.template } },
        )

        /** Splits a template by slashes outside the parentheses of parameter patterns. */
        private fun split(template: String): List<String> {
            val segments = mutableListOf<String>()
            val current = StringBuilder()
            var depth = 0
            template.forEach { character ->
                if (character == '/' && depth == 0) {
                    segments.add(current.toString())
                    current.clear()
                } else {
                    if (character == '(') depth++
                    if (character == ')') depth--
                    current.append(character)
                }
            }
            segments.add(current.toString())
            return segments.filter { segment -> segment.isNotEmpty() }
        }
    }
}
