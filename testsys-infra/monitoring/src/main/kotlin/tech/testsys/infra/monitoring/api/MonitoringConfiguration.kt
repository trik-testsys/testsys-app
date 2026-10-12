package tech.testsys.infra.monitoring.api

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Registers the shared [TechnicalFailures] of the context; without a metrics registry failures are not exported.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration(proxyBeanMethods = false)
class MonitoringConfiguration {
    /**
     * Counts technical failures in the registry of the context, or in none if the context has no registry.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun technicalFailures(registry: ObjectProvider<MeterRegistry>): TechnicalFailures =
        registry.ifAvailable?.let(::TechnicalFailures) ?: TechnicalFailures.NONE
}
