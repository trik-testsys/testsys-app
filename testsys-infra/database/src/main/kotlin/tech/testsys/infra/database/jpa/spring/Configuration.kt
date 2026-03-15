package tech.testsys.infra.database.jpa.spring

import org.hibernate.boot.model.naming.PhysicalNamingStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import tech.testsys.infra.database.jpa.spring.component.TestsysPhysicalNamingStrategy

/**
 * @since %CURRENT_VERSION%
 */
@Configuration
@PropertySource("classpath:hibernate-defaults.properties")
class Configuration {

    /**
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun physicalNamingStrategy(): PhysicalNamingStrategy = TestsysPhysicalNamingStrategy()
}
