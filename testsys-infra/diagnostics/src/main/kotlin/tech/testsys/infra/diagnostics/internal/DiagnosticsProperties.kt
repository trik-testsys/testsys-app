package tech.testsys.infra.diagnostics.internal

/**
 * Application configuration of polygon diagnostics.
 *
 * @property maxTimeLimitMillis the inclusive upper time limit in milliseconds.
 * @property scorePrefix the nonblank text identifying a score output message.
 * @since %CURRENT_VERSION%
 */
@InternalDiagnosticsApi
class DiagnosticsProperties(
    val maxTimeLimitMillis: Long,
    val scorePrefix: String,
) {
    init {
        require(maxTimeLimitMillis >= 0) { "Diagnostics maxTimeLimitMillis=$maxTimeLimitMillis must not be negative" }
        require(scorePrefix.isNotBlank()) { "Diagnostics scorePrefix must not be blank" }
    }
}
