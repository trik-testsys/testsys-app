package tech.testsys.infra.database.api.persistence

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.persistence.FileStorageCleanupRepository
import tech.testsys.infra.database.internal.persistence.FileSystemBlobInventory
import tech.testsys.infra.settings.api.Settings
import java.time.Clock

/**
 * Explicit maintenance cleanup of unowned file metadata and old blobs, with no automatic scheduling.
 * Call only after all writers have stopped and their transactions finished, using directories dedicated to this database.
 *
 * @since 1.0.0-beta.1
 */
@Component
@OptIn(InternalDatabaseApi::class)
class FileStorageCleanup(
    private val repository: FileStorageCleanupRepository,
    private val inventory: FileSystemBlobInventory,
    private val paths: FileStoragePaths,
    private val settings: Settings<FileStorageCleanupProperties>,
    @param:Qualifier("fileStorageCleanupClock") private val clock: Clock,
) {

    /**
     * Removes orphan file metadata regardless of age, then orphan blobs using one fixed age cutoff.
     * Each database batch commits separately; database failures abort the pass and file I/O failures remain retryable.
     *
     * @throws IllegalStateException if called inside a transaction, whose uncommitted changes must not affect cleanup.
     * @since 1.0.0-beta.1
     */
    fun run() {
        check(!TransactionSynchronizationManager.isActualTransactionActive()) { "File cleanup must run outside a transaction" }
        val pass = settings.current()
        val cutoff = clock.instant().minus(pass.minimumAge)
        drain(pass.batchSize)
        val directories =
            listOf(paths.statement, paths.exercise, paths.test, paths.solution, paths.logs, paths.recording)
        inventory.forEachBatch(directories, cutoff, pass.batchSize) { files ->
            val stored = repository.findStoredKeys(files.map { file -> file.fileName.toString() })
            files.filter { file -> file.fileName.toString() !in stored }.forEach(inventory::delete)
        }
    }

    private fun drain(batchSize: Int) {
        do {
            val deleted = repository.deleteFileData(batchSize)
        } while (deleted == batchSize)
    }
}
