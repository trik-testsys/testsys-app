package tech.testsys.infra.database.internal.persistence

import org.springframework.data.domain.PageRequest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository

/**
 * Removes unowned file metadata in separate transactions and checks persisted blob keys.
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
class FileStorageCleanupRepository(
    private val fileData: FileDataJpaEntityRepository,
    transactionManager: PlatformTransactionManager,
) {

    private val transactions = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    /**
     * Removes at most [limit] file metadata rows without any of the six file owners, regardless of creation time.
     *
     * @since %CURRENT_VERSION%
     */
    fun deleteFileData(limit: Int): Int = transactions.execute {
        val ids = fileData.findOrphanIds(PageRequest.of(0, limit))
        if (ids.isEmpty()) 0 else fileData.deleteOrphansByIds(ids)
    }

    /**
     * Returns the persisted subset of [keys]; empty input performs no database access.
     *
     * @since %CURRENT_VERSION%
     */
    fun findStoredKeys(keys: List<String>): Set<String> {
        if (keys.isEmpty()) return emptySet()
        return fileData.findStoredKeys(keys).toSet()
    }
}
