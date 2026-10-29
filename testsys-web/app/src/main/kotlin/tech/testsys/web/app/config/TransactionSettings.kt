package tech.testsys.web.app.config

import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import tech.testsys.infra.settings.api.Settings

/** Retains the value of [settings] captured first in a transaction until it completes; outside one, reads it on each call. */
internal class TransactionSettings<T : Any>(
    private val type: Class<T>,
    private val settings: Settings<T>,
) : Settings<T> {
    private val key = Any()

    override fun current(): T {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return settings.current()
        val existing = type.cast(TransactionSynchronizationManager.getResource(key))
        if (existing != null) return existing
        val value = settings.current()
        TransactionSynchronizationManager.bindResource(key, value)
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun suspend() {
                TransactionSynchronizationManager.unbindResource(key)
            }
            override fun resume() {
                TransactionSynchronizationManager.bindResource(key, value)
            }
            override fun afterCompletion(status: Int) {
                TransactionSynchronizationManager.unbindResourceIfPossible(key)
            }
        })
        return value
    }
}
