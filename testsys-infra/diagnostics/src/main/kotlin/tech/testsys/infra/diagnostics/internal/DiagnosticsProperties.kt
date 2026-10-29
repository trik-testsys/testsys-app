package tech.testsys.infra.diagnostics.internal

import tech.testsys.infra.settings.api.SettingsProperties

/**
 * Application configuration of polygon diagnostics, bound from `testsys.diagnostics`.
 *
 * @property maxTimeLimitMillis the inclusive upper time limit in milliseconds.
 * @property scorePrefix the nonblank text identifying a score output message.
 * @since 1.0.0-beta.1
 */
@InternalDiagnosticsApi
@SettingsProperties("testsys.diagnostics")
class DiagnosticsProperties(
    val maxTimeLimitMillis: Long,
    val scorePrefix: String,
) {
    init {
        require(maxTimeLimitMillis >= 0) { "Diagnostics maxTimeLimitMillis=$maxTimeLimitMillis must not be negative" }
        require(scorePrefix.isNotBlank()) { "Diagnostics scorePrefix must not be blank" }
    }
}
