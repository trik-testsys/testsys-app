package tech.testsys.web.monitoring

import io.micrometer.core.instrument.LongTaskTimer
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.observation.Observation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.concurrent.TimeUnit

class MetricsConfigurationTests {
    private val configuration = MetricsConfiguration()
    private val registry = SimpleMeterRegistry()

    @Test
    fun `should publish fixed latency buckets of HTTP requests`() {
        registry.config().meterFilter(configuration.httpRequestBuckets())

        val timer = Timer.builder("http.server.requests").register(registry)

        val buckets = timer.takeSnapshot().histogramCounts().map { count -> count.bucket(TimeUnit.MILLISECONDS) }
        assertEquals(listOf(10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0, 2500.0, 5000.0, 10_000.0, 30_000.0), buckets)
    }

    @Test
    fun `should drop HTTP series beyond one hundred endpoints`() {
        registry.config().meterFilter(configuration.httpEndpointLimit())
        registerEndpoints(registry, count = 100)

        Timer.builder("http.server.requests").tag("uri", "/page/101").register(registry)

        assertEquals(100, registry.find("http.server.requests").timers().size)
        assertNull(registry.find("http.server.requests").tag("uri", "/page/101").timer())
    }

    @Test
    fun `should not register long task timers`() {
        registry.config().meterFilter(configuration.longTaskTimers())

        LongTaskTimer.builder("http.server.requests.active").register(registry)

        assertEquals(emptyList<Any>(), registry.meters)
    }

    @ParameterizedTest
    @CsvSource(
        "spring.security.filterchains, false",
        "spring.security.authorizations, false",
        "http.server.requests, true",
    )
    fun `should skip only Spring Security observations`(name: String, observed: Boolean) {
        val isObserved = configuration.securityObservations().test(name, Observation.Context())

        assertEquals(observed, isObserved)
    }

    private fun registerEndpoints(registry: MeterRegistry, count: Int) {
        for (index in 1..count) {
            Timer.builder("http.server.requests").tag("uri", "/page/$index").register(registry)
        }
    }
}
