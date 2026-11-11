package tech.testsys.infra.settings.api.store

import java.util.UUID

/**
 * Durable overrides from one completed database transaction; an absent row has revision zero.
 *
 * @property revision the monotonically increasing committed revision.
 * @property overrides only explicitly saved values, keyed by exact catalog property names.
 * @property changeId the identity of the committed write, or null for an absent row.
 * @since 1.0.0-beta.1
 */
data class StoredSettings(val revision: Long, val overrides: Map<String, String>, val changeId: UUID?)

/**
 * Atomic storage operations, each in a separate completed transaction, independent of a caller transaction.
 * A write exception may leave its commit outcome unknown; callers must read back before reporting rollback.
 *
 * @since 1.0.0-beta.1
 */
interface SettingsStore {
    /**
     * Reads a durable set; failure aborts startup or requires consistency recovery during an update.
     *
     * @since 1.0.0-beta.1
     */
    fun read(): StoredSettings

    /**
     * Atomically replaces all overrides if [expectedRevision] matches, otherwise returns null.
     * Repeated [changeId] identifies the same write; compensation additionally requires [expectedChangeId].
     *
     * @since 1.0.0-beta.1
     */
    fun compareAndSet(
        expectedRevision: Long,
        overrides: Map<String, String>,
        changeId: UUID,
        expectedChangeId: UUID? = null,
    ): StoredSettings?
}
