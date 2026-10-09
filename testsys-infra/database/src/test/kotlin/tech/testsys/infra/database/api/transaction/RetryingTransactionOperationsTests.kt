package tech.testsys.infra.database.api.transaction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.CannotAcquireLockException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionOperations
import tech.testsys.infra.database.DatabaseIntegrationTests

class RetryingTransactionOperationsTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var transactions: TransactionOperations

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `should run every attempt in a new transaction`() {
        val transactionIds = mutableListOf<Long>()

        val result = transactions.execute {
            transactionIds.add(currentTransactionId())
            if (transactionIds.size == 1) throw CannotAcquireLockException("simulated conflict")
            "done"
        }

        assertEquals("done", result)
        assertEquals(2, transactionIds.size)
        assertNotEquals(transactionIds[0], transactionIds[1])
    }

    @Test
    fun `should repeat only the outermost transaction of nested calls`() {
        var outerAttempts = 0
        var innerAttempts = 0

        assertThrows(CannotAcquireLockException::class.java) {
            transactions.execute {
                outerAttempts++
                transactions.execute {
                    innerAttempts++
                    throw CannotAcquireLockException("simulated conflict")
                }
            }
        }

        assertEquals(3, outerAttempts)
        assertEquals(3, innerAttempts)
    }

    private fun currentTransactionId(): Long = checkNotNull(jdbcTemplate.queryForObject("select txid_current()", Long::class.java))
}
