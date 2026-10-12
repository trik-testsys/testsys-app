package tech.testsys.bootstrap

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.env.MockEnvironment

class MetricsWebSettingsTests {
    @Test
    fun `should listen on all interfaces unless an address is configured`() {
        val settings = MetricsWebSettings.from(MockEnvironment().withProperty("testsys.metrics.port", "8082"))

        assertEquals(MetricsWebSettings(address = "0.0.0.0", port = 8082), settings)
    }

    @Test
    fun `should use the configured address and port`() {
        val settings = MetricsWebSettings.from(
            MockEnvironment().withProperty("testsys.metrics.address", "10.0.0.5").withProperty("testsys.metrics.port", "9100"),
        )

        assertEquals(MetricsWebSettings(address = "10.0.0.5", port = 9100), settings)
    }

    @Test
    fun `should require a port`() {
        assertThrows(IllegalStateException::class.java) { MetricsWebSettings.from(MockEnvironment()) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["-1", "65536"])
    fun `should reject a port outside the valid range`(port: String) {
        assertThrows(IllegalArgumentException::class.java) {
            MetricsWebSettings.from(MockEnvironment().withProperty("testsys.metrics.port", port))
        }
    }

    @Test
    fun `should reject a blank address`() {
        val environment = MockEnvironment().withProperty("testsys.metrics.address", " ").withProperty("testsys.metrics.port", "0")

        assertThrows(IllegalArgumentException::class.java) { MetricsWebSettings.from(environment) }
    }
}
