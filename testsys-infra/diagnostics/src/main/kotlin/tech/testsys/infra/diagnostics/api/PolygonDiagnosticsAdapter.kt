package tech.testsys.infra.diagnostics.api

import org.springframework.stereotype.Component
import tech.testsys.domain.builder.api.testDiagnosticResult
import tech.testsys.domain.contract.FileContentReader
import tech.testsys.domain.contract.PolygonDiagnostics
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestDiagnosticResult
import tech.testsys.infra.diagnostics.internal.DiagnosticsProperties
import tech.testsys.infra.diagnostics.internal.InternalDiagnosticsApi
import tech.testsys.infra.diagnostics.internal.polygon.PolygonDiagnosticRunner

/**
 * Synchronous polygon analysis adapter with independent parser and diagnostics for each call.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDiagnosticsApi::class)
class PolygonDiagnosticsAdapter(
    private val properties: DiagnosticsProperties,
    private val fileContentReader: FileContentReader,
) : PolygonDiagnostics {
    override fun diagnose(test: Test): TestDiagnosticResult = testDiagnosticResult {
        testId = test.id
        reports = PolygonDiagnosticRunner().diagnose(
            text = fileContentReader.read(test.data.file).toString(Charsets.UTF_8),
            maxTimeLimitMillis = properties.maxTimeLimitMillis,
            scorePrefix = properties.scorePrefix,
        ).toMutableList()
    }
}
