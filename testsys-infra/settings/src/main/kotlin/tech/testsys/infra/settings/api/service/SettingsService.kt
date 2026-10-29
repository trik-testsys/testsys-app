package tech.testsys.infra.settings.api.service

import tech.testsys.infra.settings.api.SettingsSnapshot
import tech.testsys.infra.settings.api.catalog.SettingKey

/**
 * Current service-page state; an uncertain durable outcome locks further edits until restart.
 *
 * @property revision the latest known durable revision, including compensation writes.
 * @property snapshot the known effective snapshot.
 * @property persistence the certainty of the saved values.
 * @since 1.0.0-beta.1
 */
data class SettingsOverview(val revision: Long, val snapshot: SettingsSnapshot, val persistence: SettingsPersistence)

/**
 * Certainty of the durable state, independent of the known effective snapshot.
 *
 * @since 1.0.0-beta.1
 */
enum class SettingsPersistence {
    /**
     * The saved set is confirmed.
     *
     * @since 1.0.0-beta.1
     */
    Known,

    /**
     * A commit or compensation outcome cannot be confirmed.
     *
     * @since 1.0.0-beta.1
     */
    Uncertain,
}

/**
 * Atomic service edits and effective/saved settings overview; technical failures never imply successful rollback.
 *
 * @since 1.0.0-beta.1
 */
interface SettingsService {
    /**
     * Returns a consistent overview without changing settings or running maintenance.
     *
     * @since 1.0.0-beta.1
     */
    fun overview(): SettingsOverview

    /**
     * Validates and applies only [changes] if [expectedRevision] is current; no partial success is returned.
     *
     * @since 1.0.0-beta.1
     */
    fun change(expectedRevision: Long, changes: Map<SettingKey, String>): SettingsChangeResult
}

/**
 * Result of an atomic settings edit.
 *
 * @since 1.0.0-beta.1
 */
sealed interface SettingsChangeResult {
    /**
     * Successfully published state.
     *
     * @property overview the resulting saved and active state.
     * @since 1.0.0-beta.1
     */
    data class Applied(val overview: SettingsOverview) : SettingsChangeResult

    /**
     * Field validation failure; no write occurred.
     *
     * @property errors the rejected fields and explanations.
     * @since 1.0.0-beta.1
     */
    data class Invalid(val errors: Map<SettingKey, String>) : SettingsChangeResult

    /**
     * The form revision no longer matches; reload before editing again.
     *
     * @since 1.0.0-beta.1
     */
    data object Stale : SettingsChangeResult

    /**
     * A confirmed failure with the previous effective and saved values restored.
     *
     * @since 1.0.0-beta.1
     */
    data object Failed : SettingsChangeResult

    /**
     * The commit or compensation could not be confirmed; further writes are locked until restart.
     *
     * @since 1.0.0-beta.1
     */
    data object Uncertain : SettingsChangeResult
}
