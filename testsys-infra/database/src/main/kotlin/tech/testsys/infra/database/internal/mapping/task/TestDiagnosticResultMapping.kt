package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.*
import tech.testsys.domain.model.task.*
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.DiagnosticReportJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.DiagnosticSeverityJpaEnum
import java.util.Base64

/**
 * Mapping of polygon diagnostic messages and their XML locations.
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object TestDiagnosticResultMapping {
    /**
     * Creates a stored message at [position] for the polygon result in [requestId].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(requestId: Long, testId: Long, position: Int, report: DiagnosticReport): DiagnosticReportJpaEntity {
        val (reason, parameters) = when (val data = report.data) {
            is DiagnosticData.UnknownElement -> "UnknownElement" to encode(listOf(data.tag))
            is DiagnosticData.MalformedXml -> "MalformedXml" to encode(listOf(data.details))
            is DiagnosticData.MissingChild -> "MissingChild" to encode(listOf(data.tag))
            is DiagnosticData.MissingAttribute -> "MissingAttribute" to encode(listOf(data.attribute))
            is DiagnosticData.InvalidAttributeValue ->
                "InvalidAttributeValue" to encode(listOf(data.attribute, data.expected, data.actual))
            is DiagnosticData.InvalidChildCount -> "InvalidChildCount" to encode(listOf(data.expected.toString(), data.actual.toString()))
            DiagnosticData.MissingTimeLimit -> "MissingTimeLimit" to ""
            is DiagnosticData.MultipleTimeLimits -> "MultipleTimeLimits" to encode(listOf(data.count.toString()))
            is DiagnosticData.NegativeTimeLimit -> "NegativeTimeLimit" to encode(listOf(data.value.toString()))
            is DiagnosticData.ExcessiveTimeLimit -> "ExcessiveTimeLimit" to encode(listOf(data.value.toString(), data.maximum.toString()))
            is DiagnosticData.InvalidEventId -> "InvalidEventId" to encode(listOf(data.id))
            DiagnosticData.MissingScoreOutput -> "MissingScoreOutput" to ""
        }
        return DiagnosticReportJpaEntity(
            requestId = requestId,
            testId = testId,
            position = position,
            severity = when (report.severity) {
                DiagnosticSeverity.Info -> DiagnosticSeverityJpaEnum.INFO
                DiagnosticSeverity.Warning -> DiagnosticSeverityJpaEnum.WARNING
                DiagnosticSeverity.Error -> DiagnosticSeverityJpaEnum.ERROR
            },
            reason = reason,
            parameters = parameters,
            location = report.location?.let { location ->
                encode(
                    listOf(location.attribute.orEmpty()) + location.path.flatMap { segment ->
                        listOf(segment.tag, segment.index.toString())
                    },
                )
            },
        )
    }

    /**
     * Restores a typed [DiagnosticReport] from a stored [row].
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(row: DiagnosticReportJpaEntity): DiagnosticReport {
        val parameters = decode(row.parameters)
        return diagnosticReport {
            severity = when (row.severity) {
                DiagnosticSeverityJpaEnum.INFO -> DiagnosticSeverity.Info
                DiagnosticSeverityJpaEnum.WARNING -> DiagnosticSeverity.Warning
                DiagnosticSeverityJpaEnum.ERROR -> DiagnosticSeverity.Error
            }
            when (row.reason) {
                "UnknownElement" -> data.unknownElement {
                    tag = parameters[0]
                }
                "MalformedXml" -> data.malformedXml {
                    details = parameters[0]
                }
                "MissingChild" -> data.missingChild {
                    tag = parameters[0]
                }
                "MissingAttribute" -> data.missingAttribute {
                    attribute = parameters[0]
                }
                "InvalidAttributeValue" -> data.invalidAttributeValue {
                    attribute = parameters[0]
                    expected = parameters[1]
                    actual = parameters[2]
                }
                "InvalidChildCount" -> data.invalidChildCount {
                    expected = parameters[0].toInt()
                    actual = parameters[1].toInt()
                }
                "MissingTimeLimit" -> data.missingTimeLimit()
                "MultipleTimeLimits" -> data.multipleTimeLimits {
                    count = parameters[0].toInt()
                }
                "NegativeTimeLimit" -> data.negativeTimeLimit {
                    value = parameters[0].toLong()
                }
                "ExcessiveTimeLimit" -> data.excessiveTimeLimit {
                    value = parameters[0].toLong()
                    maximum = parameters[1].toLong()
                }
                "InvalidEventId" -> data.invalidEventId {
                    id = parameters[0]
                }
                "MissingScoreOutput" -> data.missingScoreOutput()
                else -> error("Unknown stored diagnostic reason=${row.reason}")
            }
            location = row.location?.let { encoded ->
                val parts = decode(encoded)
                diagnosticLocation {
                    attribute = parts.first().ifEmpty { null }
                    path = parts.drop(1).chunked(2).map { pair ->
                        diagnosticPathSegment {
                            tag = pair[0]
                            index = pair[1].toInt()
                        }
                    }.toMutableList()
                }
            }
        }
    }

    private fun encode(parts: List<String>): String =
        parts.joinToString(".") { Base64.getEncoder().encodeToString(it.toByteArray(Charsets.UTF_8)) }

    private fun decode(value: String): List<String> = value.split(".").map { String(Base64.getDecoder().decode(it), Charsets.UTF_8) }
}
