package tech.testsys.infra.diagnostics.internal.polygon.diagnostics

import tech.testsys.domain.model.task.DiagnosticReport
import tech.testsys.infra.diagnostics.internal.InternalDiagnosticsApi
import tech.testsys.infra.diagnostics.internal.polygon.Polygon

@InternalDiagnosticsApi
internal interface PolygonDiagnostic {
    fun diagnose(polygon: Polygon): List<DiagnosticReport>
}
