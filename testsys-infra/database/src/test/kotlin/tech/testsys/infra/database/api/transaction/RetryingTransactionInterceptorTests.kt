package tech.testsys.infra.database.api.transaction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.aop.framework.Advised
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.dao.CannotAcquireLockException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.interceptor.TransactionInterceptor
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.infra.database.DatabaseIntegrationTests
import java.util.concurrent.CopyOnWriteArrayList

@Import(RetryingTransactionInterceptorTests.ConflictingService::class)
class RetryingTransactionInterceptorTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var service: ConflictingService

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Test
    fun `should repeat a conflicting transactional method in a new transaction`() {
        val transactionId = service.conflictOnce()

        assertEquals(2, service.transactionIds.size)
        assertNotEquals(service.transactionIds[0], service.transactionIds[1])
        assertEquals(service.transactionIds[1], transactionId)
    }

    @Test
    fun `should place the retrying advisor of an adapter before its transaction advisor`() {
        val advisors = assertInstanceOf(Advised::class.java, taskRepository).advisors.map { advisor -> advisor.advice }

        val retryIndex = advisors.indexOfFirst { advice -> advice is RetryingTransactionInterceptor }
        val transactionIndex = advisors.indexOfFirst { advice -> advice is TransactionInterceptor }

        assertTrue(retryIndex in 0 until transactionIndex, "advisors: $advisors")
    }

    /**
     * Fails its first transaction with a conflict and records the id of every transaction it runs in.
     */
    @Transactional
    class ConflictingService(private val jdbcTemplate: JdbcTemplate) {

        val transactionIds = CopyOnWriteArrayList<Long>()

        fun conflictOnce(): Long {
            val transactionId = checkNotNull(jdbcTemplate.queryForObject("select txid_current()", Long::class.java))
            transactionIds.add(transactionId)
            if (transactionIds.size == 1) throw CannotAcquireLockException("simulated conflict")
            return transactionId
        }
    }
}
