package tech.testsys.bootstrap

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration
import org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.PrometheusScrapeEndpoint
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Metrics web child: a servlet server without Vaadin, security and sessions that only exposes the Prometheus scrape
 * endpoint of the shared registry. Its own requests are not observed.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration(proxyBeanMethods = false)
@ImportAutoConfiguration(
    ConfigurationPropertiesAutoConfiguration::class,
    PropertyPlaceholderAutoConfiguration::class,
    SslAutoConfiguration::class,
    HttpMessageConvertersAutoConfiguration::class,
    TomcatServletWebServerAutoConfiguration::class,
    DispatcherServletAutoConfiguration::class,
    WebMvcAutoConfiguration::class,
    EndpointAutoConfiguration::class,
    WebEndpointAutoConfiguration::class,
    ManagementContextAutoConfiguration::class,
)
class MetricsWebConfiguration {
    /**
     * Scrapes the shared registry; an endpoint the parent already created is reused.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    @ConditionalOnMissingBean
    fun prometheusScrapeEndpoint(registry: PrometheusRegistry, config: PrometheusConfig): PrometheusScrapeEndpoint =
        PrometheusScrapeEndpoint(registry, config.prometheusProperties())
}
