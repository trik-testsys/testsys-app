@file:OptIn(InternalSettingsApi::class)

package tech.testsys.infra.settings.internal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import tech.testsys.infra.settings.api.catalog.SettingKey
import tech.testsys.infra.settings.api.store.StoredSettings
import java.time.Duration
import java.util.UUID

class SettingsInitializerTests {
    @ParameterizedTest
    @EnumSource(SettingKey::class, names = ["ConfirmationCodeLifetime", "CommunityInviteTtl", "ClassInviteTtl", "CleanupMinimumAge"])
    fun `should restore long durable durations used with instants`(key: SettingKey) {
        val stored = StoredSettings(7, mapOf(key.property to "P110000D"), UUID(0, 7))

        val runtime = SettingsInitializer.initialize(stored, settingsEnvironment())

        assertEquals(7, runtime.snapshot().revision)
        assertEquals(Duration.ofDays(110_000), runtime.snapshot().values.duration(key))
    }

    @Test
    fun `should use latest committed values as active after restart`() {
        val stored = StoredSettings(7, mapOf(SettingKey.MaxMessageBytes.property to "1024"), UUID(0, 7))
        val runtime = SettingsInitializer.initialize(stored, settingsEnvironment())

        assertEquals(7, runtime.snapshot().revision)
        assertEquals(1024, runtime.snapshot().activeMaxMessageBytes)
    }

    @Test
    fun `should abort startup rather than fall back from an invalid durable value`() {
        val stored = StoredSettings(7, mapOf(SettingKey.GradingMaxAttempts.property to "0"), UUID(0, 7))

        assertThrows(IllegalArgumentException::class.java) { SettingsInitializer.initialize(stored, settingsEnvironment()) }
    }
}
