package tech.testsys.infra.database.api.persistence

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.persistence.FileStorageCleanupRepository
import tech.testsys.infra.database.internal.persistence.FileSystemBlobInventory
import java.time.Clock

/**
 * Explicit maintenance cleanup of unowned file metadata and old blobs, with no automatic scheduling.
 * Call only after all writers have stopped and their transactions finished, using directories dedicated to this database.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class FileStorageCleanup(
    private val repository: FileStorageCleanupRepository,
    private val inventory: FileSystemBlobInventory,
    private val paths: FileStoragePaths,
    private val settings: FileStorageCleanupSettings,
    @param:Qualifier("fileStorageCleanupClock") private val clock: Clock,
) {

    /**
     * Removes orphan file metadata regardless of age, then orphan blobs using one fixed age cutoff.
     * Each database batch commits separately; database failures abort the pass and file I/O failures remain retryable.
     *
     * @throws IllegalStateException if called inside a transaction, whose uncommitted changes must not affect cleanup.
     * @since %CURRENT_VERSION%
     */
    fun run() {
        check(!TransactionSynchronizationManager.isActualTransactionActive()) { "File cleanup must run outside a transaction" }
        val cutoff = clock.instant().minus(settings.minimumAge)
        drain()
        val directories =
            listOf(paths.statement, paths.exercise, paths.test, paths.solution, paths.logs, paths.recording)
        inventory.forEachBatch(directories, cutoff, settings.batchSize) { files ->
            val stored = repository.findStoredKeys(files.map { file -> file.fileName.toString() })
            files.filter { file -> file.fileName.toString() !in stored }.forEach(inventory::delete)
        }
    }

    private fun drain() {
        do {
            val deleted = repository.deleteFileData(settings.batchSize)
        } while (deleted == settings.batchSize)
    }
}
