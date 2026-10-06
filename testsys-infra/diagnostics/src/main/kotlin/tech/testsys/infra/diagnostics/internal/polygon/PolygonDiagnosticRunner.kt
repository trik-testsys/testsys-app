package tech.testsys.infra.diagnostics.internal.polygon

import tech.testsys.domain.model.task.DiagnosticReport
import tech.testsys.infra.diagnostics.internal.InternalDiagnosticsApi
import tech.testsys.infra.diagnostics.internal.polygon.diagnostics.InvalidEventIdDiagnostic
import tech.testsys.infra.diagnostics.internal.polygon.diagnostics.ScoreOutputDiagnostic
import tech.testsys.infra.diagnostics.internal.polygon.diagnostics.TimeLimitConstraintDiagnostic

@InternalDiagnosticsApi
internal class PolygonDiagnosticRunner {
    fun diagnose(text: String, maxTimeLimitMillis: Long, scorePrefix: String): List<DiagnosticReport> {
        val parsed = PolygonParser().parse(text)
        val polygon = parsed.polygon ?: return parsed.reports
        return parsed.reports + listOf(
            TimeLimitConstraintDiagnostic(maxTimeLimitMillis),
            InvalidEventIdDiagnostic(),
            ScoreOutputDiagnostic(scorePrefix),
        ).flatMap { it.diagnose(polygon) }
    }
}
