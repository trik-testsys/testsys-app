package tech.testsys.infra.database.internal.jpa

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import tech.testsys.infra.database.DatabaseIntegrationTests

/**
 * Boots the database module against the PostgreSQL of the test container.
 *
 * On context load:
 *  1. Liquibase applies every changeset from `db/changelog/db.changelog-master.yaml`, including the
 *     PostgreSQL-only ones.
 *  2. Hibernate's `ddl-auto=validate` (configured in `hibernate-defaults.properties`)
 *     compares the resulting schema against the JPA metamodel of every entity in
 *     `tech.testsys.infra.database.internal.jpa.entity`.
 *
 * If either step fails — bad SQL in a migration, a column missing from an entity,
 * type or nullability mismatch — the Spring context fails to start and the tests
 * fail with a descriptive error.
 */
class SchemaValidationTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `should accept the schema produced by liquibase migrations when hibernate validates it`() {
        // A successful Spring context load proves that Liquibase and Hibernate agree on the schema.
    }

    @Test
    fun `should create the partial judge queue index of submissions`() {
        val definitions = jdbcTemplate.queryForList(
            "select indexdef from pg_indexes where schemaname = current_schema() and indexname = 'ix_ts_submission_judge_queue'",
            String::class.java,
        )

        val definition = definitions.single().orEmpty()
        assertTrue(definition.contains("(author_id, grading_contest_id)"), definition)
        assertTrue(definition.contains("WHERE"), definition)
    }
}
