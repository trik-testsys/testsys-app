package tech.testsys.infra.database

import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource

/**
 * Base of tests running against the database module booted by [DatabaseTestApp] on the PostgreSQL of
 * [PostgresTestContainer] (Liquibase migrations applied, Hibernate `ddl-auto=validate`).
 *
 * Every test class shares one cached Spring context; adapter calls run in their own transactions exactly as in
 * production, so all `ts_*` tables are truncated after each test to keep tests independent.
 * Hibernate statistics are enabled, so [withStatementCount] can count the statements prepared by a block.
 */
@SpringBootTest(classes = [DatabaseTestApp::class, DatabaseFixtures::class, PostgresTestConfiguration::class])
@TestPropertySource(
    properties = [
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "testsys.file-storage.paths.statement=${DatabaseIntegrationTests.STATEMENT_PATH}",
        "testsys.file-storage.paths.exercise=${DatabaseIntegrationTests.EXERCISE_PATH}",
        "testsys.file-storage.paths.test=${DatabaseIntegrationTests.TEST_PATH}",
        "testsys.file-storage.paths.solution=${DatabaseIntegrationTests.SOLUTION_PATH}",
        "testsys.file-storage.paths.recording=${DatabaseIntegrationTests.RECORDING_PATH}",
        "testsys.file-storage.paths.logs=${DatabaseIntegrationTests.LOGS_PATH}",
    ],
)
abstract class DatabaseIntegrationTests {

    @Autowired
    protected lateinit var fixtures: DatabaseFixtures

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @AfterEach
    fun truncateTables() {
        val tables = jdbcTemplate.queryForList(
            "select table_name from information_schema.tables " +
                "where table_schema = current_schema() and table_type = 'BASE TABLE' and table_name like 'ts\\_%'",
            String::class.java,
        )
        if (tables.isNotEmpty()) jdbcTemplate.execute("truncate table ${tables.joinToString()} cascade")
    }

    /**
     * Runs [block] and returns its result with the number of statements Hibernate prepared while it ran.
     * Compare counts of calls with different input sizes rather than asserting absolute values.
     */
    protected fun <T> withStatementCount(block: () -> T): Pair<T, Long> {
        val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        check(statistics.isStatisticsEnabled) { "Hibernate statistics are disabled: set hibernate.generate_statistics=true" }
        statistics.clear()

        val result = block()
        return result to statistics.prepareStatementCount
    }

    companion object {

        const val STATEMENT_PATH = "/testsys/statement"
        const val EXERCISE_PATH = "/testsys/exercise"
        const val TEST_PATH = "/testsys/test"
        const val SOLUTION_PATH = "/testsys/solution"
        const val RECORDING_PATH = "/testsys/recording"
        const val LOGS_PATH = "/testsys/logs"
    }
}
