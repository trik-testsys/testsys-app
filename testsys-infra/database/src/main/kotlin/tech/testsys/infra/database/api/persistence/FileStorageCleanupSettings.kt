package tech.testsys.infra.database.api.persistence

import java.time.Duration

/**
 * Retention and batch limits for maintenance cleanup, resolved by `DatabaseConfiguration` from
 * `testsys.file-storage.cleanup` or the restored shared settings.
 *
 * @property minimumAge the required time since a physical blob was last modified before removal; metadata has no age limit.
 * @property batchSize the maximum number of rows or blobs checked per batch.
 * @since %CURRENT_VERSION%
 */
data class FileStorageCleanupSettings(val minimumAge: Duration, val batchSize: Int) {

    init {
        require(!minimumAge.isNegative && !minimumAge.isZero) { "Cleanup minimumAge must be positive: $minimumAge" }
        require(batchSize > 0) { "Cleanup batchSize must be positive: $batchSize" }
    }
}
