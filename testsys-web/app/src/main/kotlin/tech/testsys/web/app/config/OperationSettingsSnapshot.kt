package tech.testsys.web.app.config

import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.infra.settings.api.SettingsSnapshot

internal class OperationSettingsSnapshot(private val runtime: RuntimeSettings) {
    private val key = Any()

    fun current(): SettingsSnapshot {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return runtime.snapshot()
        val existing = TransactionSynchronizationManager.getResource(key) as? SettingsSnapshot
        if (existing != null) return existing
        val snapshot = runtime.snapshot()
        TransactionSynchronizationManager.bindResource(key, snapshot)
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun suspend() {
                TransactionSynchronizationManager.unbindResource(key)
            }
            override fun resume() {
                TransactionSynchronizationManager.bindResource(key, snapshot)
            }
            override fun afterCompletion(status: Int) {
                TransactionSynchronizationManager.unbindResourceIfPossible(key)
            }
        })
        return snapshot
    }
}
