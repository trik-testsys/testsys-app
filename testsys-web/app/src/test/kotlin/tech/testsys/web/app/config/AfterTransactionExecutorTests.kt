package tech.testsys.web.app.config

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

class AfterTransactionExecutorTests {
    private val executed = mutableListOf<String>()
    private val executor = AfterTransactionExecutor { task -> task.run() }

    @AfterEach
    fun tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization()
    }

    @Test
    fun `should run the task at once outside a transaction`() {
        executor.execute { executed += "task" }

        assertEquals(listOf("task"), executed)
    }

    @Test
    fun `should not run the task before the transaction completes`() {
        TransactionSynchronizationManager.initSynchronization()

        executor.execute { executed += "task" }

        assertEquals(emptyList<String>(), executed)
    }

    @ParameterizedTest
    @ValueSource(ints = [TransactionSynchronization.STATUS_COMMITTED, TransactionSynchronization.STATUS_ROLLED_BACK])
    fun `should run the task after the transaction completes with any outcome`(status: Int) {
        TransactionSynchronizationManager.initSynchronization()
        executor.execute { executed += "task" }

        TransactionSynchronizationManager.getSynchronizations().forEach { synchronization -> synchronization.afterCompletion(status) }

        assertEquals(listOf("task"), executed)
    }
}
