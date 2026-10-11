@file:OptIn(InternalSettingsApi::class)

package tech.testsys.infra.settings.internal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.testsys.infra.settings.api.SettingKey

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
    fun `should reject excluded and unknown properties`() {
        assertThrows(IllegalArgumentException::class.java) {
            SettingsResolver(settingsEnvironment()).resolve(mapOf("testsys.mail.host" to "mail"))
        }
    }
}
