package tech.testsys.infra.database

import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource

/**
 * Base of tests running against the database module booted by [DatabaseTestApp] on an in-memory H2 in
 * PostgreSQL-compatibility mode (Liquibase migrations applied, Hibernate `ddl-auto=validate`).
 *
 * Every test class shares one cached Spring context; adapter calls run in their own transactions exactly as in
 * production, so all `ts_*` tables are truncated after each test to keep tests independent.
 * Hibernate detects [org.hibernate.dialect.H2Dialect] from the connection, so PostgreSQL-only features are not exercised.
 */
@SpringBootTest(classes = [DatabaseTestApp::class, DatabaseFixtures::class])
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:testsys_database;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
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

    @AfterEach
    fun truncateTables() {
        val tables = jdbcTemplate.queryForList(
            "select table_name from information_schema.tables where table_type = 'BASE TABLE' and lower(table_name) like 'ts%'",
            String::class.java,
        )
        jdbcTemplate.execute("set referential_integrity false")
        try {
            tables.forEach { jdbcTemplate.execute("truncate table $it") }
        } finally {
            jdbcTemplate.execute("set referential_integrity true")
        }
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
