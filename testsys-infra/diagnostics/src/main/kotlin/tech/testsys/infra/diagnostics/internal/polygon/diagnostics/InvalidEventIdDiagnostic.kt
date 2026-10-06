package tech.testsys.infra.diagnostics.internal.polygon.diagnostics

import tech.testsys.domain.builder.api.diagnosticReport
import tech.testsys.domain.model.task.DiagnosticReport
import tech.testsys.domain.model.task.DiagnosticSeverity
import tech.testsys.infra.diagnostics.internal.InternalDiagnosticsApi
import tech.testsys.infra.diagnostics.internal.polygon.Polygon
import tech.testsys.infra.diagnostics.internal.polygon.PolygonElement
import tech.testsys.infra.diagnostics.internal.polygon.elements

@InternalDiagnosticsApi
internal class InvalidEventIdDiagnostic : PolygonDiagnostic {
    override fun diagnose(polygon: Polygon): List<DiagnosticReport> {
        val elements = polygon.elements()
        val eventIds = elements.mapNotNull { (it.value as? PolygonElement.Event)?.id }.toSet()
        return elements.mapNotNull { element ->
            val reference = element.value as? PolygonElement.EventReference
            if (reference != null && reference.id !in eventIds) {
                diagnosticReport {
                    severity = DiagnosticSeverity.Error
                    data.invalidEventId { id = reference.id }
                    location = element.location
                }
            } else {
                null
            }
        }
    }
}
