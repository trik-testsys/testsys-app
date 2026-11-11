package tech.testsys.bootstrap

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.env.MockEnvironment

class ServiceWebSettingsTests {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = ["short", "                                                               "])
    fun `should reject missing blank or short secrets without including them in the error`(secret: String?) {
        val environment = MockEnvironment()
        if (secret != null) environment.setProperty("testsys.service.secret", secret)

        val failure = assertThrows(IllegalArgumentException::class.java) { ServiceWebSettings.from(environment) }

        assertTrue(checkNotNull(failure.message).contains("64"))
        if (!secret.isNullOrBlank()) assertFalse(checkNotNull(failure.message).contains(secret))
    }

    @Test
    fun `should accept exactly 64 characters and compare the entire code`() {
        val secret = "A".repeat(64)
        val settings = ServiceWebSettings.from(
            serviceEnvironment(secret).withProperty("testsys.service.grafana-url", "http://127.0.0.1:3000/"),
        )

        assertTrue(settings.matches(secret))
        assertFalse(settings.matches(secret + "A"))
        assertFalse(settings.matches("B" + secret.drop(1)))
        assertFalse(settings.toString().contains(secret))
    }

    @Test
    fun `should bind the digest of a sign-in token to the secret`() {
        fun settingsOf(secret: String) = ServiceWebSettings.from(
            serviceEnvironment(secret).withProperty("testsys.service.grafana-url", "http://127.0.0.1:3000/"),
        )

        val first = settingsOf("A".repeat(64)).signInTokenHash("token")
        val same = settingsOf("A".repeat(64)).signInTokenHash("token")
        val other = settingsOf("B".repeat(64)).signInTokenHash("token")

        assertEquals(first, same)
        assertNotEquals(first, other)
    }

    @Test
    fun `should accept an absolute https Grafana address with a path and keep it unchanged`() {
        val settings = ServiceWebSettings.from(
            serviceEnvironment().withProperty("testsys.service.grafana-url", "https://grafana.example.test/sub/?orgId=1"),
        )

        assertEquals("https://grafana.example.test/sub/?orgId=1", settings.grafanaUrl)
    }

    @Test
    fun `should reject a missing Grafana address`() {
        val environment = serviceEnvironment()

        val failure = assertThrows(IllegalArgumentException::class.java) { ServiceWebSettings.from(environment) }

        assertEquals(GRAFANA_URL_ERROR, failure.message)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "   ",
            "grafana.example.test/",
            "/grafana/",
            "ftp://grafana.example.test/",
            "javascript:alert(1)",
            "https:///grafana/",
            "https://viewer:password@grafana.example.test/",
            "http://grafana example/",
        ],
    )
    fun `should reject an invalid Grafana address without including it in the error`(grafanaUrl: String) {
        val environment = serviceEnvironment().withProperty("testsys.service.grafana-url", grafanaUrl)

        val failure = assertThrows(IllegalArgumentException::class.java) { ServiceWebSettings.from(environment) }

        assertEquals(GRAFANA_URL_ERROR, failure.message)
    }

    private fun serviceEnvironment(secret: String = "C".repeat(64)): MockEnvironment = MockEnvironment()
        .withProperty("testsys.service.secret", secret)
        .withProperty("testsys.service.address", "127.0.0.1")
        .withProperty("testsys.service.port", "0")

    private companion object {
        const val GRAFANA_URL_ERROR = "testsys.service.grafana-url must be an absolute http or https URL without user information"
    }
}
