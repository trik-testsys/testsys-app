package tech.testsys.web.monitoring

import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.observation.ObservationPredicate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * Shapes the single metrics registry of the shared parent: HTTP latency buckets for the average and the 95th percentile,
 * a bound on endpoint labels, no long task timers and no Spring Security observations.
 *
 * @since 1.0.0-beta.1
 */
@Configuration(proxyBeanMethods = false)
class MetricsConfiguration {
    /**
     * Publishes fixed latency buckets of HTTP requests; client-side percentiles are not computed.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun httpRequestBuckets(): MeterFilter = object : MeterFilter {
        override fun configure(id: Meter.Id, config: DistributionStatisticConfig): DistributionStatisticConfig? =
            if (id.name == HTTP_SERVER_REQUESTS) {
                DistributionStatisticConfig.builder()
                    .serviceLevelObjectives(*HTTP_BUCKETS.map { bucket -> bucket.toNanos().toDouble() }.toDoubleArray())
                    .build()
                    .merge(config)
            } else {
                config
            }
    }

    /**
     * Denies HTTP meters beyond 100 distinct endpoint labels, guarding against an unnormalized path.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun httpEndpointLimit(): MeterFilter = MeterFilter.maximumAllowableTags(HTTP_SERVER_REQUESTS, URI, MAX_ENDPOINTS, MeterFilter.deny())

    /**
     * Drops long task timers, which observations would create for every active request.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun longTaskTimers(): MeterFilter = MeterFilter.deny { id -> id.type == Meter.Type.LONG_TASK_TIMER }

    /**
     * Skips the observations of Spring Security filters and authorizations; requests are observed once by the web layer.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun securityObservations(): ObservationPredicate = ObservationPredicate { name, _ -> !name.startsWith("spring.security.") }

    private companion object {
        const val HTTP_SERVER_REQUESTS = "http.server.requests"
        const val URI = "uri"
        const val MAX_ENDPOINTS = 100

        val HTTP_BUCKETS: List<Duration> = listOf(
            Duration.ofMillis(10),
            Duration.ofMillis(25),
            Duration.ofMillis(50),
            Duration.ofMillis(100),
            Duration.ofMillis(250),
            Duration.ofMillis(500),
            Duration.ofSeconds(1),
            Duration.ofMillis(2_500),
            Duration.ofSeconds(5),
            Duration.ofSeconds(10),
            Duration.ofSeconds(30),
        )
    }
}
