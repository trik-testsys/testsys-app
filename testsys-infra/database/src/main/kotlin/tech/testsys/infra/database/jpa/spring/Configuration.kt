package tech.testsys.infra.database.jpa.spring

import org.hibernate.boot.model.naming.PhysicalNamingStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * @author Roman Shishkin
 * @since %CURRENT_VERSION%
 */
@Configuration
class Configuration {

    @Bean
    fun physicalNamingStrategy(): PhysicalNamingStrategy = TestsysPhysicalNamingStrategy()
}
