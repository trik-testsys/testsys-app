package tech.testsys.infra.database.api.transaction

import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionOperations
import org.springframework.transaction.support.TransactionTemplate

/**
 * [TransactionOperations] for programmatic transactions: every attempt runs the callback in a new transaction with
 * default settings, and [retry] repeats it on a conflict.
 *
 * @param transactionManager the manager that opens the transactions.
 * @property retry the policy that repeats conflicting transactions.
 * @since %CURRENT_VERSION%
 */
class RetryingTransactionOperations(
    transactionManager: PlatformTransactionManager,
    private val retry: TransactionRetry,
) : TransactionOperations {

    private val template = TransactionTemplate(transactionManager)

    override fun <T> execute(action: TransactionCallback<T>): T = retry.run(action.javaClass.name) { template.execute(action) }
}
