package tech.testsys.web.app.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import tech.testsys.infra.database.api.DatabaseConfiguration
import tech.testsys.infra.diagnostics.api.DiagnosticsConfiguration
import tech.testsys.infra.grpc.api.GrpcConfiguration

/**
 * Connects the configurations of the infrastructure modules that implement the ports of the operations.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration
@Import(DatabaseConfiguration::class, GrpcConfiguration::class, DiagnosticsConfiguration::class)
class InfraConfiguration
