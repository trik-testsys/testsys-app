package tech.testsys.infra.settings.internal

import tech.testsys.infra.settings.api.catalog.SettingKey
import tech.testsys.infra.settings.api.catalog.SettingKind
import java.time.Duration
import java.time.Instant

@InternalSettingsApi
internal object SettingsValidator {
    fun canonical(key: SettingKey, value: String): String = when (key.kind) {
        SettingKind.Integer -> value.toInt().also { number ->
            require(number >= if (key == SettingKey.MaxParticipants) 0 else 1) { "${key.property} is out of range: $value" }
        }.toString()
        SettingKind.Long -> value.toLong().also { number ->
            require(number >= 0) { "${key.property} must not be negative: $value" }
        }.toString()
        SettingKind.Boolean -> value.toBooleanStrict().toString()
        SettingKind.Duration -> Duration.parse(value).also { duration ->
            require(duration > Duration.ZERO) { "${key.property} must be positive: $value" }
            when (key) {
                SettingKey.TotalTimeout, SettingKey.RpcTimeout, SettingKey.StatusTimeout, SettingKey.PollInterval -> duration.toNanos()
                SettingKey.ConfirmationCodeLifetime, SettingKey.CommunityInviteTtl, SettingKey.ClassInviteTtl ->
                    Instant.now().plus(duration)
                SettingKey.CleanupMinimumAge -> Instant.now().minus(duration)
                else -> error("Unexpected duration setting: ${key.property}")
            }
        }.toString()
        SettingKind.Text -> value.also { require(value.isNotBlank()) { "${key.property} must not be blank" } }
    }
}
