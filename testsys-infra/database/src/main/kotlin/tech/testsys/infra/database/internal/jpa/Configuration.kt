package tech.testsys.infra.database.internal.jpa

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * Spring configuration of the JPA layer: Hibernate defaults from `classpath:hibernate-defaults.properties` and scanning
 * of the module's entities, Spring Data repositories and persistence adapters.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration
@EntityScan(basePackages = ["tech.testsys.infra.database.internal.jpa.entity"])
@EnableJpaRepositories(basePackages = ["tech.testsys.infra.database.internal.jpa.repository"])
@ComponentScan(basePackages = ["tech.testsys.infra.database.api.*"])
@PropertySource("classpath:hibernate-defaults.properties")
class Configuration
