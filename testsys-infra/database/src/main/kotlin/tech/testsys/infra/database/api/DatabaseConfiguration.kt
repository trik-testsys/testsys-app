package tech.testsys.infra.database.api

import org.hibernate.boot.model.naming.PhysicalNamingStrategy
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import tech.testsys.infra.database.api.persistence.FileStoragePaths
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.TestsysPhysicalNamingStrategy

/**
 * Spring configuration of the JPA layer, imported by the application: Hibernate defaults from
 * `classpath:hibernate-defaults.properties`, the [TestsysPhysicalNamingStrategy] bean and scanning of the module's
 * entities, Spring Data repositories, persistence adapters and file storage with its [FileStoragePaths].
 *
 * @since %CURRENT_VERSION%
 */
@Configuration
@EntityScan(basePackages = ["tech.testsys.infra.database.internal.jpa.entity"])
@EnableJpaRepositories(basePackages = ["tech.testsys.infra.database.internal.jpa.repository"])
@ComponentScan(basePackages = ["tech.testsys.infra.database.api"])
@PropertySource("classpath:hibernate-defaults.properties")
@EnableConfigurationProperties(FileStoragePaths::class)
class DatabaseConfiguration {

    /**
     * Naming strategy applied to every entity of the module; Spring Boot passes it to Hibernate.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    @OptIn(InternalDatabaseApi::class)
    fun physicalNamingStrategy(): PhysicalNamingStrategy = TestsysPhysicalNamingStrategy()
}
