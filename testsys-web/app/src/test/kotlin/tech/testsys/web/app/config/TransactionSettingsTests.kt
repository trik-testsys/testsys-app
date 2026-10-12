package tech.testsys.web.app.config

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import tech.testsys.infra.settings.api.Settings
import java.util.concurrent.atomic.AtomicReference

class TransactionSettingsTests {
    private val current = AtomicReference(0)
    private val settings = TransactionSettings(Int::class.javaObjectType, Settings { current.get() })

    @AfterEach
    fun cleanUp() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) finish()
    }

    @Test
    fun `should retain one value in a transaction and capture a new one on retry`() {
        TransactionSynchronizationManager.initSynchronization()
        assertEquals(0, settings.current())
        current.set(1)
        assertEquals(0, settings.current())
        finish()

        TransactionSynchronizationManager.initSynchronization()

        assertEquals(1, settings.current())
    }

    @Test
    fun `should suspend the outer value and restore it after an independent transaction`() {
        TransactionSynchronizationManager.initSynchronization()
        settings.current()
        val outer = TransactionSynchronizationManager.getSynchronizations()
        outer.forEach(TransactionSynchronization::suspend)
        TransactionSynchronizationManager.clearSynchronization()
        current.set(1)
        TransactionSynchronizationManager.initSynchronization()
        assertEquals(1, settings.current())
        finish()
        TransactionSynchronizationManager.initSynchronization()
        outer.forEach { synchronization ->
            synchronization.resume()
            TransactionSynchronizationManager.registerSynchronization(synchronization)
        }

        assertEquals(0, settings.current())
    }

    @Test
    fun `should capture the current value on each call outside a transaction`() {
        assertEquals(0, settings.current())
        current.set(1)

        assertEquals(1, settings.current())
    }

    private fun finish() {
        TransactionSynchronizationManager.getSynchronizations().forEach { synchronization ->
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)
        }
        TransactionSynchronizationManager.clearSynchronization()
    }
}
