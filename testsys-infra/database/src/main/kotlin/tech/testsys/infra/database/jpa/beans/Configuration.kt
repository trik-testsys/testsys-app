package tech.testsys.infra.database.jpa.beans

import org.hibernate.boot.model.naming.PhysicalNamingStrategy
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * Spring configuration for the JPA/Hibernate infrastructure layer.
 *
 * Loads default Hibernate properties from `classpath:hibernate-defaults.properties`,
 * registers project-wide beans such as the [PhysicalNamingStrategy], and enables
 * scanning for JPA entities, Spring Data repositories, and domain-facing
 * persistence adapters within this module.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration
@EntityScan(basePackages = ["tech.testsys.infra.database.jpa.entity"])
@EnableJpaRepositories(basePackages = ["tech.testsys.infra.database.jpa.beans.repository"])
@ComponentScan(basePackages = ["tech.testsys.infra.database.jpa.beans.*"])
@PropertySource("classpath:hibernate-defaults.properties")
class Configuration
