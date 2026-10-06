package tech.testsys.infra.diagnostics.api

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

/**
 * Registers the synchronous polygon adapter and its application configuration.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration
@ComponentScan(basePackages = ["tech.testsys.infra.diagnostics.api", "tech.testsys.infra.diagnostics.internal"])
@PropertySource(value = ["classpath:diagnostics-defaults.properties"], encoding = "UTF-8")
class DiagnosticsConfiguration
