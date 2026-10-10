package tech.testsys.infra.database.api.transaction

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.dao.CannotAcquireLockException
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Duration

class TransactionRetryTests {

    private val retry = TransactionRetry(maxRetries = 2, delay = Duration.ZERO, jitter = Duration.ZERO)

    @AfterEach
    fun resetTransactionState() {
        TransactionSynchronizationManager.setActualTransactionActive(false)
    }

    @Test
    fun `should return the result of the second attempt after a conflict`() {
        var attempts = 0

        val result = retry.run("test") {
            attempts++
            if (attempts == 1) throw CannotAcquireLockException("locked")
            "done"
        }

        assertEquals("done", result)
        assertEquals(2, attempts)
    }

    @Test
    fun `should rethrow the last conflict after three attempts`() {
        val failures = mutableListOf<CannotAcquireLockException>()

        val thrown = assertThrows(CannotAcquireLockException::class.java) {
            retry.run("test") { throw CannotAcquireLockException("locked ${failures.size}").also(failures::add) }
        }

        assertEquals(3, failures.size)
        assertSame(failures.last(), thrown)
    }

    @Test
    fun `should rethrow a failure that is not a conflict after one attempt`() {
        var attempts = 0
        val failure = IllegalStateException("broken")

        val thrown = assertThrows(IllegalStateException::class.java) {
            retry.run("test") {
                attempts++
                throw failure
            }
        }

        assertSame(failure, thrown)
        assertEquals(1, attempts)
    }

    @Test
    fun `should not repeat a conflict inside an active transaction`() {
        TransactionSynchronizationManager.setActualTransactionActive(true)
        var attempts = 0

        assertThrows(CannotAcquireLockException::class.java) {
            retry.run("test") {
                attempts++
                throw CannotAcquireLockException("locked")
            }
        }

        assertEquals(1, attempts)
    }
}
