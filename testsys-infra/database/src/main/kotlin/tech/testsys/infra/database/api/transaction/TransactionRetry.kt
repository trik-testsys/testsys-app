package tech.testsys.infra.database.api.transaction

import org.apache.commons.logging.LogFactory
import org.springframework.core.retry.RetryException
import org.springframework.core.retry.RetryListener
import org.springframework.core.retry.RetryPolicy
import org.springframework.core.retry.RetryState
import org.springframework.core.retry.RetryTemplate
import org.springframework.core.retry.Retryable
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Duration

/**
 * Repeats a whole transaction that failed with a conflict detected by [TransactionConflicts], pausing exponentially
 * between attempts, and rethrows other failures and the last conflict unchanged. Inside an active transaction the
 * attempt runs once, because only the outermost transaction can be repeated.
 *
 * @param maxRetries how many times a conflicting transaction is repeated after the first attempt.
 * @param delay the base pause before the first repeat, doubled for each next one.
 * @param jitter the random deviation of the first pause, doubled together with it.
 * @since %CURRENT_VERSION%
 */
class TransactionRetry(
    maxRetries: Long = DEFAULT_MAX_RETRIES,
    delay: Duration = DEFAULT_DELAY,
    jitter: Duration = DEFAULT_JITTER,
) {

    private val template = RetryTemplate(
        RetryPolicy.builder()
            .maxRetries(maxRetries)
            .delay(delay)
            .jitter(jitter)
            .multiplier(MULTIPLIER)
            .predicate(TransactionConflicts::isConflict)
            .build(),
    ).apply { retryListener = ConflictLogger }

    /**
     * Runs [attempt] and repeats it while it fails with a transaction conflict; [name] identifies the transaction
     * in the log. Each call of [attempt] must open its own transaction.
     *
     * @param T the result type.
     * @since %CURRENT_VERSION%
     */
    fun <T> run(name: String, attempt: () -> T): T {
        if (TransactionSynchronizationManager.isActualTransactionActive()) return attempt()

        val retryable = object : Retryable<T> {
            override fun execute(): T = attempt()

            override fun getName(): String = name
        }
        try {
            return template.execute(retryable)
        } catch (failure: RetryException) {
            throw failure.cause
        }
    }

    /**
     * Logs every repeat at DEBUG and a conflict that remains after the last repeat at WARN.
     */
    private object ConflictLogger : RetryListener {

        private val log = LogFactory.getLog(TransactionRetry::class.java)

        override fun beforeRetry(retryPolicy: RetryPolicy, retryable: Retryable<*>, retryState: RetryState) {
            if (log.isDebugEnabled) {
                log.debug("Repeating transaction ${retryable.name} after a conflict: ${retryState.lastException}")
            }
        }

        override fun onRetryPolicyExhaustion(retryPolicy: RetryPolicy, retryable: Retryable<*>, exception: RetryException) {
            if (exception.retryCount > 0 && TransactionConflicts.isConflict(exception.cause)) {
                log.warn("Transaction ${retryable.name} still conflicts after ${exception.retryCount} repeats", exception.cause)
            }
        }
    }

    private companion object {
        const val DEFAULT_MAX_RETRIES = 2L
        const val MULTIPLIER = 2.0
        val DEFAULT_DELAY: Duration = Duration.ofMillis(10)
        val DEFAULT_JITTER: Duration = Duration.ofMillis(40)
    }
}
