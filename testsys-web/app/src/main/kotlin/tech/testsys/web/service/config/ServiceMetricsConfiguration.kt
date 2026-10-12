package tech.testsys.web.service.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.observation.ServerRequestObservationConvention
import tech.testsys.infra.monitoring.api.TechnicalFailures
import tech.testsys.web.monitoring.WebFailures
import tech.testsys.web.monitoring.WebRequestObservationConvention

/**
 * Provides the metrics of the service interface: request labels and counting of page failures.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration(proxyBeanMethods = false)
class ServiceMetricsConfiguration {
    /**
     * Labels the HTTP requests of the service interface with `service` and its route templates.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun serviceRequestObservationConvention(): ServerRequestObservationConvention = WebRequestObservationConvention(SERVICE)

    /**
     * Counts unexpected exceptions of the service pages before they are shown.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun serviceWebFailures(failures: TechnicalFailures): WebFailures = WebFailures(SERVICE, failures)

    private companion object {
        const val SERVICE = "service"
    }
}
