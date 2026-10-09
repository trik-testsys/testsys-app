package tech.testsys.infra.database.api.transaction

import jakarta.persistence.OptimisticLockException
import org.hibernate.StaleStateException
import org.springframework.dao.ConcurrencyFailureException
import java.sql.SQLException
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Classifies failures of a transaction: a conflict with a concurrent transaction is worth repeating in a new
 * transaction, any other failure is not. Conflicts are optimistic lock failures, serialization failures (SQLSTATE
 * `40001`), deadlocks (`40P01`) and unique key violations (`23505`), found anywhere in the cause chain.
 *
 * @since %CURRENT_VERSION%
 */
object TransactionConflicts {

    private val conflictSqlStates = setOf("40001", "40P01", "23505")

    /**
     * Returns whether [throwable] or one of its causes reports a conflict with a concurrent transaction.
     *
     * @since %CURRENT_VERSION%
     */
    fun isConflict(throwable: Throwable): Boolean {
        val visited = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        return generateSequence(throwable) { current -> current.cause }
            .takeWhile(visited::add)
            .any(::isConflictItself)
    }

    private fun isConflictItself(throwable: Throwable): Boolean = when (throwable) {
        is ConcurrencyFailureException, is OptimisticLockException, is StaleStateException -> true
        is SQLException -> throwable.sqlState in conflictSqlStates
        else -> false
    }
}
