package tech.testsys.infra.database.api.registry

import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.infra.database.api.transaction.TransactionRetry
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.settings.GradingNodeJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.settings.GradingNodeJpaEntityRepository
import tech.testsys.infra.settings.api.registry.GradingNodeStore
import tech.testsys.infra.settings.api.registry.StoredGradingNode

/**
 * Checking node registry storage in independent transactions; idempotent writes repeat only database conflicts.
 * A commit exception is propagated so the registry can read back the outcome.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class DatabaseGradingNodeStore(
    private val repository: GradingNodeJpaEntityRepository,
    transactionManager: PlatformTransactionManager,
    private val retry: TransactionRetry,
) : GradingNodeStore {
    private val transaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    override fun read(): List<StoredGradingNode> = transaction.execute {
        repository.findAllByOrderByAddressAsc().map { row -> row.stored() }
    }

    override fun add(node: StoredGradingNode): Boolean = retry.run(RETRY_NAME) {
        transaction.execute {
            val current = repository.findByAddress(node.address.target)
            if (current != null) return@execute current.stored() == node
            repository.saveAndFlush(GradingNodeJpaEntity(address = node.address.target, isActive = node.isActive))
            true
        }
    }

    override fun setActive(address: GradingNodeAddress, isActive: Boolean): Boolean = retry.run(RETRY_NAME) {
        transaction.execute {
            val current = repository.findByAddress(address.target) ?: return@execute false
            current.isActive = isActive
            repository.saveAndFlush(current)
            true
        }
    }

    override fun remove(address: GradingNodeAddress) = retry.run(RETRY_NAME) {
        transaction.executeWithoutResult {
            repository.findByAddress(address.target)?.let { row ->
                repository.delete(row)
                repository.flush()
            }
        }
    }

    private fun GradingNodeJpaEntity.stored(): StoredGradingNode = StoredGradingNode(GradingNodeAddress(address), isActive)

    private companion object {
        const val RETRY_NAME = "grading-nodes"
    }
}
