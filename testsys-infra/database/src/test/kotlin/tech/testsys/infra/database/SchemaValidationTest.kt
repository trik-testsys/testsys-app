package tech.testsys.infra.database

import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

/**
 * Boots the database module against an in-memory H2 in PostgreSQL-compatibility mode.
 *
 * On context load:
 *  1. Liquibase applies every changeset from `db/changelog/db.changelog-master.yaml`.
 *  2. Hibernate's `ddl-auto=validate` (configured in `hibernate-defaults.properties`)
 *     compares the resulting schema against the JPA metamodel of every entity in
 *     `tech.testsys.infra.database.jpa.entity`.
 *
 * If either step fails — bad SQL in a migration, a column missing from an entity,
 * type or nullability mismatch — the Spring context fails to start and this test
 * fails with a descriptive error. The test body is intentionally empty: a successful
 * context load is the assertion.
 *
 * The dialect is overridden to [org.hibernate.dialect.H2Dialect] so Hibernate
 * compares JDBC metadata against H2's type system rather than against
 * PostgreSQL's. PostgreSQL-only features (custom enum types, etc.) would not
 * be exercised here — that is the deliberate trade-off vs. Testcontainers.
 */
@SpringBootTest(classes = [SchemaValidationTestApp::class])
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:testsys_schema_validation;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    ]
)
class SchemaValidationTest {

    @Test
    fun `liquibase migrations produce a schema that hibernate validate accepts`() {
        // Empty: the assertion is the successful Spring context load above.
    }
}

@SpringBootApplication(scanBasePackages = ["tech.testsys.infra.database"])
class SchemaValidationTestApp
