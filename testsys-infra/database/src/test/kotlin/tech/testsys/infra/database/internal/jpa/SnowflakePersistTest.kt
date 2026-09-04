package tech.testsys.infra.database.internal.jpa

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.TrikStudioVersionJpaEntity
import tech.testsys.infra.database.internal.jpa.id.SnowflakeIdGenerator
import tech.testsys.infra.database.internal.jpa.repository.task.TrikStudioVersionJpaEntityRepository
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Saves entities through a Spring Data repository on H2 and checks that Hibernate assigned Snowflake-shaped ids
 * with the default node id from `hibernate-defaults.properties`.
 */
@OptIn(InternalDatabaseApi::class)
@SpringBootTest(classes = [SchemaValidationTestApp::class])
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:testsys_snowflake_persist;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    ]
)
class SnowflakePersistTest {

    @Autowired
    private lateinit var repository: TrikStudioVersionJpaEntityRepository

    @Test
    fun `persisted entities get increasing snowflake ids with the default node id`() {
        val before = Instant.now()
        val first = repository.save(TrikStudioVersionJpaEntity(tag = "snowflake-persist-1"))
        val second = repository.save(TrikStudioVersionJpaEntity(tag = "snowflake-persist-2"))

        val firstId = assertNotNull(first.id)
        val secondId = assertNotNull(second.id)
        assertTrue(secondId > firstId)
        assertEquals(0, SnowflakeIdGenerator.nodeIdOf(firstId))
        val issuedAt = SnowflakeIdGenerator.instantOf(firstId)
        assertTrue(!issuedAt.isBefore(before.minus(TOLERANCE)) && !issuedAt.isAfter(Instant.now().plus(TOLERANCE)))
        assertEquals(firstId, repository.findById(firstId).orElseThrow().id)
    }

    private companion object {
        val TOLERANCE: Duration = Duration.ofSeconds(2)
    }
}
