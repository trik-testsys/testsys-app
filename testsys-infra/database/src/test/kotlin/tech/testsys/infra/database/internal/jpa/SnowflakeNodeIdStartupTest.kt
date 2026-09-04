package tech.testsys.infra.database.internal.jpa

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.BeanCreationException
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.id.HibernateSnowflakeIdGenerator
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Boots the module like [SchemaValidationTest] but with an invalid `testsys.id.node-id` and checks that the failure
 * names the setting: the generator reads it in `configure`, so the message survives Spring's bean container.
 */
@OptIn(InternalDatabaseApi::class)
class SnowflakeNodeIdStartupTest {

    @Test
    fun `invalid node id fails context startup with a message naming the setting`() {
        val failure = assertFailsWith<BeanCreationException> {
            // Command-line arguments outrank `hibernate-defaults.properties` (a `@PropertySource`), unlike `properties(...)`.
            SpringApplicationBuilder(SchemaValidationTestApp::class.java)
                .web(WebApplicationType.NONE)
                .run(
                    "--spring.datasource.url=jdbc:h2:mem:testsys_node_id_startup;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                    "--spring.datasource.driver-class-name=org.h2.Driver",
                    "--spring.datasource.username=sa",
                    "--spring.datasource.password=",
                    "--spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
                    "--spring.jpa.properties.${HibernateSnowflakeIdGenerator.NODE_ID_SETTING}=node-1",
                )
                .close()
        }

        val messages = generateSequence<Throwable>(failure) { it.cause }.mapNotNull { it.message }.toList()
        assertTrue(
            messages.any { HibernateSnowflakeIdGenerator.NODE_ID_SETTING in it },
            "expected a cause naming ${HibernateSnowflakeIdGenerator.NODE_ID_SETTING}, got: $messages",
        )
    }
}
