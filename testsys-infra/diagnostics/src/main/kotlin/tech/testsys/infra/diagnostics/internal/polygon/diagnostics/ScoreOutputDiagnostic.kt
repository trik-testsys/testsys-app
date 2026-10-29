package tech.testsys.infra.diagnostics.internal.polygon.diagnostics

import tech.testsys.domain.builder.api.diagnosticReport
import tech.testsys.domain.model.task.DiagnosticReport
import tech.testsys.domain.model.task.DiagnosticSeverity
import tech.testsys.infra.diagnostics.internal.InternalDiagnosticsApi
import tech.testsys.infra.diagnostics.internal.polygon.Polygon
import tech.testsys.infra.diagnostics.internal.polygon.PolygonElement
import tech.testsys.infra.diagnostics.internal.polygon.elements

@InternalDiagnosticsApi
internal class ScoreOutputDiagnostic(private val scorePrefix: String) : PolygonDiagnostic {
    override fun diagnose(polygon: Polygon): List<DiagnosticReport> =
        if (polygon.elements().any { (it.value as? PolygonElement.Message)?.text?.contains(scorePrefix) == true }) {
            emptyList()
        } else {
            listOf(
                diagnosticReport {
                    severity = DiagnosticSeverity.Warning
                    data.missingScoreOutput()
                },
            )
        }
}
