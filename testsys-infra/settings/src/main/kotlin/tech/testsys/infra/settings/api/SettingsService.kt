package tech.testsys.infra.settings.api

/**
 * Current service-page state; an uncertain durable outcome locks further edits until restart.
 *
 * @property revision the latest known durable revision, including compensation writes.
 * @property snapshot the known effective snapshot.
 * @property persistence the certainty of the saved values.
 * @since %CURRENT_VERSION%
 */
data class SettingsOverview(val revision: Long, val snapshot: SettingsSnapshot, val persistence: SettingsPersistence)

/**
 * Certainty of the durable state, independent of the known effective snapshot.
 *
 * @since %CURRENT_VERSION%
 */
enum class SettingsPersistence {
    /**
     * The saved set is confirmed.
     *
     * @since %CURRENT_VERSION%
     */
    Known,

    /**
     * A commit or compensation outcome cannot be confirmed.
     *
     * @since %CURRENT_VERSION%
     */
    Uncertain,
}

/**
 * Atomic service edits and effective/saved settings overview; technical failures never imply successful rollback.
 *
 * @since %CURRENT_VERSION%
 */
interface SettingsService {
    /**
     * Returns a consistent overview without changing settings or running maintenance.
     *
     * @since %CURRENT_VERSION%
     */
    fun overview(): SettingsOverview

    /**
     * Validates and applies only [changes] if [expectedRevision] is current; no partial success is returned.
     *
     * @since %CURRENT_VERSION%
     */
    fun change(expectedRevision: Long, changes: Map<SettingKey, String>): SettingsChangeResult
}

/**
 * Result of an atomic settings edit.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface SettingsChangeResult {
    /**
     * Successfully published state.
     *
     * @property overview the resulting saved and active state.
     * @since %CURRENT_VERSION%
     */
    data class Applied(val overview: SettingsOverview) : SettingsChangeResult

    /**
     * Field validation failure; no write occurred.
     *
     * @property errors the rejected fields and explanations.
     * @since %CURRENT_VERSION%
     */
    data class Invalid(val errors: Map<SettingKey, String>) : SettingsChangeResult

    /**
     * The form revision no longer matches; reload before editing again.
     *
     * @since %CURRENT_VERSION%
     */
    data object Stale : SettingsChangeResult

    /**
     * A confirmed failure with the previous effective and saved values restored.
     *
     * @since %CURRENT_VERSION%
     */
    data object Failed : SettingsChangeResult

    /**
     * The commit or compensation could not be confirmed; further writes are locked until restart.
     *
     * @since %CURRENT_VERSION%
     */
    data object Uncertain : SettingsChangeResult
}
