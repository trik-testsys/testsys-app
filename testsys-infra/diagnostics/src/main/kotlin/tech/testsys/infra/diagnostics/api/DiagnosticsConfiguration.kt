package tech.testsys.infra.diagnostics.api

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import tech.testsys.infra.diagnostics.internal.DiagnosticsProperties
import tech.testsys.infra.diagnostics.internal.InternalDiagnosticsApi
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.infra.settings.api.Settings
import tech.testsys.infra.settings.api.settingsOf

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
    internal fun diagnosticsSettings(environment: Environment, runtime: RuntimeSettings? = null): Settings<DiagnosticsProperties> =
        settingsOf(DiagnosticsProperties::class.java, environment, runtime)
}
