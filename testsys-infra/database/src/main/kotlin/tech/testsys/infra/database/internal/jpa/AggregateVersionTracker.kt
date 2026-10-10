package tech.testsys.infra.database.internal.jpa

import org.hibernate.Hibernate
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity
import tech.testsys.infra.database.internal.utils.requireId

/** Keeps aggregate version state in the current transaction, independently of the persistence context. */
@OptIn(InternalDatabaseApi::class)
internal object AggregateVersionTracker {

    internal class State(val loadedVersion: Long?, var hasDataChanges: Boolean = false)

    fun loaded(root: SnowflakeJpaEntity) {
        current()?.remember(root, root.version)
    }

    fun created(root: SnowflakeJpaEntity) {
        current()?.remember(root, null)
    }

    fun state(root: SnowflakeJpaEntity): State =
        checkNotNull(current()) { "Aggregate version tracking requires an active transaction for ${root.javaClass.name} id=${root.id}" }
            .remember(root, root.version)

    private fun current(): Versions? {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return null

        return TransactionSynchronizationManager.getSynchronizations().filterIsInstance<Versions>().singleOrNull()
            ?: Versions().also(TransactionSynchronizationManager::registerSynchronization)
    }

    private class Versions : TransactionSynchronization {

        private val roots = mutableMapOf<Pair<Class<*>, Long>, State>()

        fun remember(root: SnowflakeJpaEntity, loadedVersion: Long?): State =
            roots.getOrPut(Hibernate.getClass(root) to root.requireId()) { State(loadedVersion) }
    }
}
