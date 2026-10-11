package tech.testsys.infra.diagnostics.api

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import tech.testsys.infra.diagnostics.internal.DiagnosticsProperties
import tech.testsys.infra.diagnostics.internal.InternalDiagnosticsApi
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.infra.settings.api.SettingKey

/**
 * Registers the synchronous polygon adapter and its application configuration.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration
@ComponentScan(basePackages = ["tech.testsys.infra.diagnostics.api", "tech.testsys.infra.diagnostics.internal"])
@PropertySource(value = ["classpath:diagnostics-defaults.properties"], encoding = "UTF-8")
@OptIn(InternalDiagnosticsApi::class)
class DiagnosticsConfiguration {
    @Bean
    internal fun diagnosticsProperties(environment: Environment, runtime: RuntimeSettings? = null): DiagnosticsProperties {
        val values = runtime?.snapshot()?.values
        return DiagnosticsProperties(
            maxTimeLimitMillis = values?.long(SettingKey.MaxTimeLimitMillis)
                ?: environment.getRequiredProperty("testsys.diagnostics.max-time-limit-millis", Long::class.java),
            scorePrefix = values?.get(SettingKey.ScorePrefix)
                ?: environment.getRequiredProperty("testsys.diagnostics.score-prefix"),
        )
    }
}
