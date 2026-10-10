package tech.testsys.infra.database

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * PostgreSQL container shared by every test of the JVM: started on first use and stopped by Testcontainers' Ryuk
 * when the JVM exits, so cached Spring contexts and standalone startups reuse one database.
 */
object PostgresTestContainer {

    val instance: PostgreSQLContainer by lazy {
        PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine")).apply { start() }
    }
}

/**
 * Connects the Spring context to [PostgresTestContainer]; closing a context does not stop the shared container.
 */
@TestConfiguration(proxyBeanMethods = false)
class PostgresTestConfiguration {

    @Bean(destroyMethod = "")
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer = PostgresTestContainer.instance
}
