package tech.testsys.infra.diagnostics.internal.polygon.diagnostics

import tech.testsys.domain.builder.api.diagnosticReport
import tech.testsys.domain.model.task.DiagnosticReport
import tech.testsys.domain.model.task.DiagnosticSeverity
import tech.testsys.infra.diagnostics.internal.InternalDiagnosticsApi
import tech.testsys.infra.diagnostics.internal.polygon.Polygon
import tech.testsys.infra.diagnostics.internal.polygon.PolygonElement

@InternalDiagnosticsApi
internal class TimeLimitConstraintDiagnostic(private val maximum: Long) : PolygonDiagnostic {
    override fun diagnose(polygon: Polygon): List<DiagnosticReport> {
        val limits = polygon.constraints.elements.filter { it.value is PolygonElement.TimeLimit }
        if (limits.isEmpty()) {
            return listOf(
                diagnosticReport {
                    severity = DiagnosticSeverity.Error
                    data.missingTimeLimit()
                },
            )
        }
        val countReports = if (limits.size > 1) {
            listOf(
                diagnosticReport {
                    severity = DiagnosticSeverity.Error
                    data.multipleTimeLimits { count = limits.size }
                },
            )
        } else {
            emptyList()
        }
        return countReports + limits.mapNotNull { limit ->
            val value = (limit.value as? PolygonElement.TimeLimit)?.milliseconds
                ?: error("Time limit filter returned another element")
            when {
                value < 0 -> diagnosticReport {
                    severity = DiagnosticSeverity.Error
                    data.negativeTimeLimit { this.value = value }
                    location = limit.location
                }
                value > maximum -> diagnosticReport {
                    severity = DiagnosticSeverity.Error
                    data.excessiveTimeLimit {
                        this.value = value
                        this.maximum = this@TimeLimitConstraintDiagnostic.maximum
                    }
                    location = limit.location
                }
                else -> null
            }
        }
    }
}
