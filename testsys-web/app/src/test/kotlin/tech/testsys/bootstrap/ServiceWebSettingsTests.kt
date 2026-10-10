package tech.testsys.bootstrap

import org.junit.jupiter.api.Assertions.assertFalse
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
            MockEnvironment().withProperty("testsys.service.secret", secret)
                .withProperty("testsys.service.address", "127.0.0.1").withProperty("testsys.service.port", "0"),
        )

        assertTrue(settings.matches(secret))
        assertFalse(settings.matches(secret + "A"))
        assertFalse(settings.matches("B" + secret.drop(1)))
        assertFalse(settings.toString().contains(secret))
    }
}
