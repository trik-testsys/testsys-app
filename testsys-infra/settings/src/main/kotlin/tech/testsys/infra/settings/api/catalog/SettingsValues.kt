package tech.testsys.infra.settings.api.catalog

import java.time.Duration
import java.util.Collections
import java.util.EnumMap

/**
 * Immutable complete set of canonical, validated settings values.
 *
 * @since %CURRENT_VERSION%
 */
class SettingsValues(values: Map<SettingKey, String>) {
    private val values = Collections.unmodifiableMap(EnumMap<SettingKey, String>(SettingKey::class.java).apply { putAll(values) })

    /**
     * Returns the canonical value of [key].
     *
     * @since %CURRENT_VERSION%
     */
    operator fun get(key: SettingKey): String = values.getValue(key)

    /** Returns the present values keyed by their Environment property names. */
    internal fun properties(): Map<String, String> = values.mapKeys { (key, _) -> key.property }

    /**
     * Returns an integer setting.
     *
     * @since %CURRENT_VERSION%
     */
    fun int(key: SettingKey): Int = get(key).toInt()

    /**
     * Returns a long setting.
     *
     * @since %CURRENT_VERSION%
     */
    fun long(key: SettingKey): Long = get(key).toLong()

    /**
     * Returns a boolean setting.
     *
     * @since %CURRENT_VERSION%
     */
    fun boolean(key: SettingKey): Boolean = get(key).toBooleanStrict()

    /**
     * Returns a duration setting.
     *
     * @since %CURRENT_VERSION%
     */
    fun duration(key: SettingKey): Duration = Duration.parse(get(key))
}
