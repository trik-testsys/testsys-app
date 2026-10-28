package tech.testsys.infra.settings.internal

import org.springframework.core.env.Environment
import tech.testsys.infra.settings.api.catalog.SettingKey
import tech.testsys.infra.settings.api.catalog.SettingsValues
import java.time.Duration

@InternalSettingsApi
internal class SettingsResolver(private val environment: Environment) {
    fun resolve(overrides: Map<String, String>): SettingsValues {
        require(overrides.keys.all { property -> SettingKey.entries.any { key -> key.property == property } }) {
            "Stored settings contain unknown properties"
        }
        return SettingsValues(
            SettingKey.entries.associateWith { key ->
                val raw = requireNotNull(overrides[key.property] ?: environment.getProperty(key.property)) {
                    "Missing setting ${key.property}"
                }
                val value = if (key == SettingKey.CleanupMinimumAge && key.property !in overrides) cleanupDuration(raw) else raw
                SettingsValidator.canonical(key, value)
            },
        )
    }

    private fun cleanupDuration(value: String): String {
        val units = mapOf(
            "" to Duration.ofMillis(1),
            "ns" to Duration.ofNanos(1),
            "us" to Duration.ofNanos(MICROSECOND_NANOS),
            "ms" to Duration.ofMillis(1),
            "s" to Duration.ofSeconds(1),
            "m" to Duration.ofMinutes(1),
            "h" to Duration.ofHours(1),
            "d" to Duration.ofDays(1),
        )
        val match = Regex("([+-]?\\d+)(ns|us|ms|s|m|h|d)?", RegexOption.IGNORE_CASE).matchEntire(value) ?: return value
        return units.getValue(match.groupValues[2].lowercase()).multipliedBy(match.groupValues[1].toLong()).toString()
    }

    private companion object {
        const val MICROSECOND_NANOS = 1_000L
    }
}
