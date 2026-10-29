package tech.testsys.infra.diagnostics.internal.polygon

import tech.testsys.domain.model.task.DiagnosticLocation
import tech.testsys.infra.diagnostics.internal.InternalDiagnosticsApi

@InternalDiagnosticsApi
internal data class Located<T>(val value: T, val location: DiagnosticLocation)
