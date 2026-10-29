package tech.testsys.bootstrap

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.context.properties.bind.BindException
import org.springframework.core.io.support.ResourcePropertySource
import org.springframework.mock.env.MockEnvironment

class MetricsWebSettingsTests {
    @Test
    fun `should listen on all interfaces at port 8082 by default`() {
        val environment = MockEnvironment().apply { propertySources.addLast(ResourcePropertySource(WEB_DEFAULTS)) }

        assertEquals(MetricsWebSettings(address = "0.0.0.0", port = 8082), MetricsWebSettings.from(environment))
    }

    @Test
    fun `should use the configured address and port`() {
        val settings = MetricsWebSettings.from(
            MockEnvironment().withProperty("testsys.metrics.address", "10.0.0.5").withProperty("testsys.metrics.port", "9100"),
        )

        assertEquals(MetricsWebSettings(address = "10.0.0.5", port = 9100), settings)
    }

    @Test
    fun `should require an address and a port`() {
        assertThrows(BindException::class.java) { MetricsWebSettings.from(MockEnvironment()) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["-1", "65536"])
    fun `should reject a port outside the valid range`(port: String) {
        val environment = MockEnvironment().withProperty("testsys.metrics.address", "0.0.0.0").withProperty("testsys.metrics.port", port)

        assertThrows(IllegalArgumentException::class.java) { MetricsWebSettings.from(environment) }
    }

    @Test
    fun `should reject a blank address`() {
        val environment = MockEnvironment().withProperty("testsys.metrics.address", " ").withProperty("testsys.metrics.port", "0")

        assertThrows(IllegalArgumentException::class.java) { MetricsWebSettings.from(environment) }
    }
}
