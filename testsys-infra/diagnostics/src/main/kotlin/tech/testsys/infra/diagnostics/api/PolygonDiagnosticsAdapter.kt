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
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.infra.settings.api.SettingKey

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
    private val runtime: RuntimeSettings? = null,
) : PolygonDiagnostics {
    override fun diagnose(test: Test): TestDiagnosticResult {
        val values = runtime?.snapshot()?.values
        val maximum = values?.long(SettingKey.MaxTimeLimitMillis) ?: properties.maxTimeLimitMillis
        val prefix = values?.get(SettingKey.ScorePrefix) ?: properties.scorePrefix
        return testDiagnosticResult {
            testId = test.id
            reports = PolygonDiagnosticRunner().diagnose(
                text = fileContentReader.read(test.data.file).toString(Charsets.UTF_8),
                maxTimeLimitMillis = maximum,
                scorePrefix = prefix,
            ).toMutableList()
        }
    }
}
