@file:OptIn(InternalSettingsApi::class)

package tech.testsys.infra.settings.internal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.testsys.infra.settings.api.catalog.SettingKey

class SettingsResolverTests {
    @Test
    fun `should ignore invalid environment value shadowed by durable override`() {
        val environment = settingsEnvironment().withProperty(SettingKey.MaxParticipants.property, "invalid")
        val values = SettingsResolver(environment).resolve(mapOf(SettingKey.MaxParticipants.property to "0"))

        assertEquals(0, values.int(SettingKey.MaxParticipants))
    }

    @Test
    fun `should preserve score prefix whitespace and parse spring cleanup duration`() {
        val environment = settingsEnvironment().withProperty(SettingKey.CleanupMinimumAge.property, "24h")
        val values = SettingsResolver(environment).resolve(mapOf(SettingKey.ScorePrefix.property to " score: "))

        assertEquals(" score: ", values[SettingKey.ScorePrefix])
        assertEquals("PT24H", values[SettingKey.CleanupMinimumAge])
    }

    @Test
    fun `should reject a setting without an override and an environment value`() {
        val environment = settingsEnvironment().apply { propertySources.remove(SettingKey.GradingMaxAttempts.property) }

        val error = assertThrows(IllegalArgumentException::class.java) {
            SettingsResolver(environment).resolve(emptyMap())
        }

        assertEquals("Missing setting testsys.grading.max-attempts", error.message)
    }

    @Test
    fun `should reject excluded and unknown properties`() {
        assertThrows(IllegalArgumentException::class.java) {
            SettingsResolver(settingsEnvironment()).resolve(mapOf("testsys.mail.host" to "mail"))
        }
    }
}
