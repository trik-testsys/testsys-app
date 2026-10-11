package tech.testsys.web.app.config

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.infra.settings.api.SettingsSnapshot
import tech.testsys.infra.settings.api.SettingsValues
import java.util.concurrent.atomic.AtomicReference

class OperationSettingsSnapshotTests {
    private val initial = SettingsSnapshot(0, SettingsValues(emptyMap()), 100)
    private val next = SettingsSnapshot(1, SettingsValues(emptyMap()), 100)
    private val current = AtomicReference(initial)
    private val runtime = mockk<RuntimeSettings> { every { snapshot() } answers { current.get() } }
    private val snapshots = OperationSettingsSnapshot(runtime)

    @AfterEach
    fun cleanUp() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) finish()
    }

    @Test
    fun `should retain one revision in a transaction and capture a new one on retry`() {
        TransactionSynchronizationManager.initSynchronization()
        assertEquals(0, snapshots.current().revision)
        current.set(next)
        assertEquals(0, snapshots.current().revision)
        finish()

        TransactionSynchronizationManager.initSynchronization()

        assertEquals(1, snapshots.current().revision)
    }

    @Test
    fun `should suspend the outer snapshot and restore it after an independent transaction`() {
        TransactionSynchronizationManager.initSynchronization()
        snapshots.current()
        val outer = TransactionSynchronizationManager.getSynchronizations()
        outer.forEach(TransactionSynchronization::suspend)
        TransactionSynchronizationManager.clearSynchronization()
        current.set(next)
        TransactionSynchronizationManager.initSynchronization()
        assertEquals(1, snapshots.current().revision)
        finish()
        TransactionSynchronizationManager.initSynchronization()
        outer.forEach { synchronization ->
            synchronization.resume()
            TransactionSynchronizationManager.registerSynchronization(synchronization)
        }

        assertEquals(0, snapshots.current().revision)
    }

    @Test
    fun `should capture current values on each call outside a transaction`() {
        assertEquals(0, snapshots.current().revision)
        current.set(next)

        assertEquals(1, snapshots.current().revision)
    }

    private fun finish() {
        TransactionSynchronizationManager.getSynchronizations().forEach { synchronization ->
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)
        }
        TransactionSynchronizationManager.clearSynchronization()
    }
}
