package tech.testsys.web.service

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import tech.testsys.infra.monitoring.api.TechnicalFailures

/**
 * Stands in for the metrics registry and failure counting of the shared parent in service contexts without a parent.
 */
@TestConfiguration(proxyBeanMethods = false)
class ServiceMetricsTestConfiguration {
    @Bean
    fun prometheusMeterRegistry(): PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    @Bean
    fun technicalFailures(registry: PrometheusMeterRegistry): TechnicalFailures = TechnicalFailures(registry)
}
