package tech.testsys.infra.diagnostics.internal

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Application configuration of polygon diagnostics.
 *
 * @property maxTimeLimitMillis the inclusive upper time limit in milliseconds.
 * @property scorePrefix the nonblank text identifying a score output message.
 * @since %CURRENT_VERSION%
 */
@InternalDiagnosticsApi
@Component
class DiagnosticsProperties(
    @Suppress("UnnecessaryAnnotationUseSiteTarget")
    @param:Value("\${testsys.diagnostics.max-time-limit-millis}")
    val maxTimeLimitMillis: Long,
    @Suppress("UnnecessaryAnnotationUseSiteTarget")
    @param:Value("\${testsys.diagnostics.score-prefix}")
    val scorePrefix: String,
) {
    init {
        require(maxTimeLimitMillis >= 0) { "Diagnostics maxTimeLimitMillis=$maxTimeLimitMillis must not be negative" }
        require(scorePrefix.isNotBlank()) { "Diagnostics scorePrefix must not be blank" }
    }
}
