package tech.testsys.infra.database.api

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.support.StaticListableBeanFactory
import org.springframework.dao.CannotAcquireLockException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionOperations
import tech.testsys.infra.database.DatabaseIntegrationTests
import tech.testsys.infra.database.api.transaction.RetryingTransactionOperations
import tech.testsys.infra.monitoring.api.TestSysMetrics

class DatabaseConfigurationTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var transactions: TransactionOperations

    @Test
    fun `should run transactions with the repeatable read isolation`() {
        val isolation = transactions.execute {
            jdbcTemplate.queryForObject("show transaction_isolation", String::class.java)
        }

        assertEquals("repeatable read", isolation)
    }

    @Test
    fun `should provide the retrying transaction operations instead of the template of spring boot`() {
        assertInstanceOf(RetryingTransactionOperations::class.java, transactions)
    }

    @Test
    fun `should count every repeat of a conflicting transaction in the metrics registry`() {
        val registry = SimpleMeterRegistry()
        val provider = StaticListableBeanFactory(mapOf("meterRegistry" to registry)).getBeanProvider(MeterRegistry::class.java)
        val retry = DatabaseConfiguration.transactionRetry(provider)

        assertThrows(CannotAcquireLockException::class.java) {
            retry.run("test") { throw CannotAcquireLockException("locked") }
        }

        assertEquals(2.0, registry.counter(TestSysMetrics.TRANSACTION_RETRIES).count())
    }
}
