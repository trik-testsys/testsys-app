@file:OptIn(InternalSettingsApi::class)

package tech.testsys.infra.settings.internal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import tech.testsys.infra.settings.api.SettingKey
import java.time.DateTimeException

class SettingsValidatorTests {
    @ParameterizedTest
    @CsvSource("MaxParticipants,0", "MaxTimeLimitMillis,0", "GradingMaxAttempts,1", "MaxConfirmationAttempts,1")
    fun `should accept agreed lower bounds`(key: SettingKey, value: String) {
        assertEquals(value, SettingsValidator.canonical(key, value))
    }

    @ParameterizedTest
    @EnumSource(SettingKey::class, names = ["ConfirmationCodeLifetime", "CommunityInviteTtl", "ClassInviteTtl", "CleanupMinimumAge"])
    fun `should accept long durations used with instants`(key: SettingKey) {
        assertEquals("PT2640000H", SettingsValidator.canonical(key, "P110000D"))
    }

    @ParameterizedTest
    @EnumSource(SettingKey::class, names = ["TotalTimeout", "RpcTimeout", "StatusTimeout", "PollInterval"])
    fun `should reject durations that overflow required nanoseconds`(key: SettingKey) {
        assertThrows(ArithmeticException::class.java) { SettingsValidator.canonical(key, "P110000D") }
    }

    @ParameterizedTest
    @EnumSource(SettingKey::class, names = ["ConfirmationCodeLifetime", "CommunityInviteTtl", "ClassInviteTtl", "CleanupMinimumAge"])
    fun `should reject durations that overflow the applied instant operation`(key: SettingKey) {
        assertThrows(DateTimeException::class.java) { SettingsValidator.canonical(key, "P800000000000D") }
    }

    @ParameterizedTest
    @CsvSource(
        "MaxParticipants,-1",
        "MaxConfirmationAttempts,0",
        "ConfirmationCodeLifetime,PT0S",
        "CommunityInviteTtl,-PT1S",
        "CleanupBatchSize,0",
        "ShouldRecordVideo,yes",
        "TotalTimeout,PT999999999999H",
    )
    fun `should reject invalid ranges and overflowing durations`(key: SettingKey, value: String) {
        assertThrows(RuntimeException::class.java) { SettingsValidator.canonical(key, value) }
    }
}
