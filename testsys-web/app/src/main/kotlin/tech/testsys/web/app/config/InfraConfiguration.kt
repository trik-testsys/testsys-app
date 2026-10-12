package tech.testsys.web.app.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import tech.testsys.infra.database.api.DatabaseConfiguration
import tech.testsys.infra.diagnostics.api.DiagnosticsConfiguration
import tech.testsys.infra.grpc.api.GrpcConfiguration
import tech.testsys.infra.mail.api.MailConfiguration
import tech.testsys.infra.monitoring.api.MonitoringConfiguration
import tech.testsys.infra.settings.api.SettingsConfiguration

/**
 * Connects the configurations of the infrastructure modules that implement the ports of the operations and count
 * technical failures.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration
@Import(
    DatabaseConfiguration::class,
    SettingsConfiguration::class,
    GrpcConfiguration::class,
    DiagnosticsConfiguration::class,
    MailConfiguration::class,
    MonitoringConfiguration::class,
)
class InfraConfiguration
