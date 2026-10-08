package tech.testsys.web.app.config

import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.Executor

/**
 * Hands tasks to [delegate] after the current transaction completes, whatever its outcome, or at once outside
 * a transaction. A task of a rolled back transaction still runs: the dispatcher has already marked its request
 * as scheduled, and processing a request that was never saved finds nothing to do.
 *
 * @since %CURRENT_VERSION%
 */
class AfterTransactionExecutor(private val delegate: Executor) : Executor {
    override fun execute(task: Runnable) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            delegate.execute(task)
            return
        }

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCompletion(status: Int) = delegate.execute(task)
            },
        )
    }
}
