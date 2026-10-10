package tech.testsys.infra.database.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionOperations
import tech.testsys.infra.database.DatabaseIntegrationTests
import tech.testsys.infra.database.api.transaction.RetryingTransactionOperations

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
}
