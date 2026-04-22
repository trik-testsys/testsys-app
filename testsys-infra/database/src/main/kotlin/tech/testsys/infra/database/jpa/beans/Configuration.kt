package tech.testsys.infra.database.jpa.beans

import org.hibernate.boot.model.naming.PhysicalNamingStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import tech.testsys.infra.database.jpa.beans.component.TestsysPhysicalNamingStrategy

/**
 * Spring configuration for the JPA/Hibernate infrastructure layer.
 *
 * Loads default Hibernate properties from `classpath:hibernate-defaults.properties`
 * and registers project-wide beans such as the [PhysicalNamingStrategy].
 *
 * @since %CURRENT_VERSION%
 */
@Configuration
@PropertySource("classpath:hibernate-defaults.properties")
class Configuration {

    /**
     * Registers [TestsysPhysicalNamingStrategy] as the physical naming strategy,
     * which converts entity and field names to snake_case table/column names
     * with a `t_` table prefix.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun physicalNamingStrategy(): PhysicalNamingStrategy = TestsysPhysicalNamingStrategy()
}
