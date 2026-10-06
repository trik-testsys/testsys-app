package tech.testsys.infra.diagnostics.internal.polygon

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.parser.Parser
import tech.testsys.domain.builder.api.diagnosticLocation
import tech.testsys.domain.builder.api.diagnosticPathSegment
import tech.testsys.domain.builder.api.diagnosticReport
import tech.testsys.domain.model.task.DiagnosticLocation
import tech.testsys.domain.model.task.DiagnosticPathSegment
import tech.testsys.domain.model.task.DiagnosticReport
import tech.testsys.domain.model.task.DiagnosticSeverity
import tech.testsys.infra.diagnostics.internal.InternalDiagnosticsApi

@InternalDiagnosticsApi
internal data class PolygonParsingResult(val polygon: Polygon?, val reports: List<DiagnosticReport>)

@InternalDiagnosticsApi
internal class PolygonParser {
    fun parse(text: String): PolygonParsingResult {
        val syntaxError = XmlWellFormednessValidator().error(text)
        if (syntaxError != null) {
            return PolygonParsingResult(
                polygon = null,
                reports = listOf(
                    diagnosticReport {
                        severity = DiagnosticSeverity.Error
                        data.malformedXml { details = syntaxError }
                    },
                ),
            )
        }
        val document = Ksoup.parse(text, parser = Parser.xmlParser())
        return ParseSession().parse(document)
    }

    @InternalDiagnosticsApi
    private class ParseSession {
        private val reports = mutableListOf<DiagnosticReport>()

        fun parse(document: Element): PolygonParsingResult = try {
            val roots = document.children()
            val rootElement = roots.single()
            val root = xmlNode(rootElement, emptyList(), 1)
            if (rootElement.tagName() != "root") fail(root) { data.missingChild { tag = "root" } }
            val world = requiredChild(root, setOf("world"))
            val constraints = requiredChild(root, setOf("constraints"))
            val regions = world.children().mapNotNull { node ->
                if (node.tag == "region") {
                    val id = required(node, "id")
                    node.children().forEach(::unknown)
                    Located(value = id, location = node.location)
                } else {
                    unknown(node)
                    null
                }
            }
            root.children().filter { it.tag != "world" && it.tag != "constraints" }.forEach(::unknown)
            val elements = constraints.children().mapNotNull { parseConstraint(it) }
            PolygonParsingResult(
                polygon = Polygon(world = PolygonWorld(regions), constraints = PolygonConstraints(elements)),
                reports = reports.toList(),
            )
        } catch (failure: ParsingFailure) {
            PolygonParsingResult(polygon = null, reports = reports + failure.report)
        }

        private fun parseConstraint(node: XmlNode): Located<PolygonElement>? = when (node.tag) {
            "timelimit" -> {
                childrenCount(node, 0)
                located(node, PolygonElement.TimeLimit(long(node, "value")), "value")
            }
            "init", "initialization" -> construct(node, node.children().mapNotNull { parseTrigger(it) })
            "constraint" -> {
                required(node, "failMessage")
                boolean(node, "checkOnce")
                childrenCount(node, 1)
                construct(node, listOfNotNull(parseCondition(node.children().single())))
            }
            "event" -> {
                childrenCount(node, 2)
                boolean(node, "dropsOnFire")
                boolean(node, "settedUpInitially")
                val condition = requiredChild(node, setOf("condition", "conditions"))
                val trigger = requiredChild(node, setOf("trigger", "triggers"))
                located(
                    node,
                    PolygonElement.Event(
                        id = node.attributes["id"],
                        body = listOfNotNull(parseCondition(condition), parseTrigger(trigger)),
                    ),
                )
            }
            else -> unknown(node)
        }

        private fun parseCondition(node: XmlNode): Located<PolygonElement>? = when (node.tag) {
            "condition", "not" -> {
                childrenCount(node, 1)
                construct(node, node.children().mapNotNull { parseCondition(it) })
            }
            "conditions" -> {
                choice(node, "glue", setOf("and", "or"), required = true)
                construct(node, node.children().mapNotNull { parseCondition(it) })
            }
            "equals", "notEqual", "greater", "notGreater", "less", "notLess" -> {
                childrenCount(node, 2)
                construct(node, node.children().mapNotNull { parseExpression(it) })
            }
            "true" -> {
                childrenCount(node, 0)
                construct(node, emptyList())
            }
            "inside" -> {
                required(node, "objectId")
                required(node, "regionId")
                choice(node, "objectPoint", setOf("center", "any", "all"), required = false)
                childrenCount(node, 0)
                construct(node, emptyList())
            }
            "settedUp", "dropped" -> {
                childrenCount(node, 0)
                located(node, PolygonElement.EventReference(required(node, "id")), "id")
            }
            "timer" -> {
                integer(node, "timeout")
                boolean(node, "forceDropOnTimeout")
                childrenCount(node, 0)
                construct(node, emptyList())
            }
            else -> unknown(node)
        }

        private fun parseTrigger(node: XmlNode): Located<PolygonElement>? = when (node.tag) {
            "trigger" -> {
                childrenCount(node, 1)
                construct(node, node.children().mapNotNull { parseTrigger(it) })
            }
            "triggers" -> construct(node, node.children().mapNotNull { parseTrigger(it) })
            "fail" -> {
                required(node, "message")
                childrenCount(node, 0)
                construct(node, emptyList())
            }
            "success" -> {
                boolean(node, "deferred")
                childrenCount(node, 0)
                construct(node, emptyList())
            }
            "setter" -> {
                required(node, "name")
                childrenCount(node, 1)
                construct(node, node.children().mapNotNull { parseExpression(it) })
            }
            "setUp", "drop" -> {
                childrenCount(node, 0)
                located(node, PolygonElement.EventReference(required(node, "id")), "id")
            }
            "message", "log" -> {
                val text = required(node, "text")
                val replacements = node.children().mapNotNull { child ->
                    if (child.tag == "replace") {
                        required(child, "var")
                        childrenCount(child, 1)
                        construct(child, child.children().mapNotNull { parseExpression(it) })
                    } else {
                        unknown(child)
                    }
                }
                if (node.tag == "message") {
                    located(node, PolygonElement.Message(text = text, replacements = replacements))
                } else {
                    construct(node, replacements)
                }
            }
            else -> unknown(node)
        }

        private fun parseExpression(node: XmlNode): Located<PolygonElement>? = when (node.tag) {
            "int" -> {
                integer(node, "value")
                leaf(node)
            }
            "double" -> {
                val value = required(node, "value")
                if (value.toDoubleOrNull()?.isFinite() != true) {
                    invalid(node = node, attribute = "value", expected = "finite double", actual = value)
                }
                leaf(node)
            }
            "bool" -> {
                choice(node, "value", setOf("true", "false"), required = true)
                leaf(node)
            }
            "string" -> {
                required(node, "value")
                leaf(node)
            }
            "variableValue", "typeOf", "objectState" -> {
                val attribute = when (node.tag) {
                    "variableValue" -> "name"
                    "typeOf" -> "objectId"
                    else -> "object"
                }
                required(node, attribute)
                leaf(node)
            }
            "minus", "abs", "boundingRect" -> expression(node, 1)
            "mul", "sum", "difference", "min", "max", "distance" -> expression(node, 2)
            else -> unknown(node)
        }

        private fun leaf(node: XmlNode): Located<PolygonElement> {
            childrenCount(node, 0)
            return construct(node, emptyList())
        }

        private fun expression(node: XmlNode, children: Int): Located<PolygonElement> {
            childrenCount(node, children)
            return construct(node, node.children().mapNotNull { parseExpression(it) })
        }

        private fun construct(node: XmlNode, children: List<Located<PolygonElement>>): Located<PolygonElement> =
            located(node, PolygonElement.Construct(tag = node.tag, attributes = node.attributes, children = children))

        private fun located(node: XmlNode, value: PolygonElement, attribute: String? = null): Located<PolygonElement> =
            Located(value = value, location = location(node, attribute))

        private fun unknown(node: XmlNode): Located<PolygonElement>? {
            reports += diagnosticReport {
                severity = DiagnosticSeverity.Info
                data.unknownElement { tag = node.tag }
                location = node.location
            }
            return null
        }

        private fun requiredChild(node: XmlNode, tags: Set<String>): XmlNode {
            val children = node.children().filter { it.tag in tags }
            if (children.isEmpty()) fail(node) { data.missingChild { tag = tags.joinToString("|") } }
            if (children.size != 1) {
                fail(node) {
                    data.invalidChildCount {
                        expected = 1
                        actual = children.size
                    }
                }
            }
            return children.single()
        }

        private fun required(node: XmlNode, attribute: String): String = node.attributes[attribute]
            ?: fail(node) { data.missingAttribute { this.attribute = attribute } }

        private fun childrenCount(node: XmlNode, count: Int) {
            if (node.children().size != count) {
                fail(node) {
                    data.invalidChildCount {
                        expected = count
                        actual = node.children().size
                    }
                }
            }
        }

        private fun long(node: XmlNode, attribute: String): Long {
            val value = required(node, attribute)
            return value.toLongOrNull() ?: invalid(node = node, attribute = attribute, expected = "long integer", actual = value)
        }

        private fun integer(node: XmlNode, attribute: String): Int {
            val value = required(node, attribute)
            return value.toIntOrNull() ?: invalid(node = node, attribute = attribute, expected = "integer", actual = value)
        }

        private fun boolean(node: XmlNode, attribute: String) = choice(node, attribute, setOf("true", "false"), required = false)

        private fun choice(node: XmlNode, attribute: String, choices: Set<String>, required: Boolean) {
            val value = if (required) required(node, attribute) else node.attributes[attribute] ?: return
            if (value !in choices) invalid(node = node, attribute = attribute, expected = choices.joinToString("|"), actual = value)
        }

        private fun invalid(node: XmlNode, attribute: String, expected: String, actual: String): Nothing = fail(node, attribute) {
            data.invalidAttributeValue {
                this.attribute = attribute
                this.expected = expected
                this.actual = actual
            }
        }

        private fun fail(
            node: XmlNode,
            attribute: String? = null,
            configure: tech.testsys.domain.builder.task.DiagnosticReportBuilder.() -> Unit,
        ): Nothing = throw ParsingFailure(
            diagnosticReport {
                severity = DiagnosticSeverity.Error
                location = location(node, attribute)
                configure()
            },
        )

        private fun location(node: XmlNode, attribute: String?): DiagnosticLocation = diagnosticLocation {
            path = node.location.path.toMutableList()
            this.attribute = attribute
        }
    }

    @InternalDiagnosticsApi
    private class ParsingFailure(val report: DiagnosticReport) : RuntimeException()

    @InternalDiagnosticsApi
    private class XmlNode(val element: Element, val location: DiagnosticLocation) {
        val tag: String = element.tagName()
        val attributes: Map<String, String> = element.attributes().associate { it.key to it.value }
        fun children(): List<XmlNode> {
            val counts = mutableMapOf<String, Int>()
            return element.children().map { child ->
                val index = counts.getOrDefault(child.tagName(), 0) + 1
                counts[child.tagName()] = index
                xmlNode(child, location.path, index)
            }
        }
    }

    @InternalDiagnosticsApi
    private companion object {
        fun xmlNode(element: Element, parent: List<DiagnosticPathSegment>, index: Int): XmlNode {
            val segment = diagnosticPathSegment {
                tag = element.tagName()
                this.index = index
            }
            return XmlNode(
                element = element,
                location = diagnosticLocation {
                    path = (parent + segment).toMutableList()
                },
            )
        }
    }
}
