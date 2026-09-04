package tech.testsys.infra.database.internal.jpa

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.infra.database.DatabaseIntegrationTest
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
class SnowflakePersistTest : DatabaseIntegrationTest() {

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
