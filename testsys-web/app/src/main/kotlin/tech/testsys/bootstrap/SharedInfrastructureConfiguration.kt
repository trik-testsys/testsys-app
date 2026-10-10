package tech.testsys.bootstrap

import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration
import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizationAutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import tech.testsys.web.app.config.InfraConfiguration
import tech.testsys.web.app.config.OperationsConfiguration

/**
 * Owns the database, operations, grader and background infrastructure shared by both web children.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration(proxyBeanMethods = false)
@Import(InfraConfiguration::class, OperationsConfiguration::class)
@ImportAutoConfiguration(
    ConfigurationPropertiesAutoConfiguration::class,
    PropertyPlaceholderAutoConfiguration::class,
    DataSourceAutoConfiguration::class,
    HibernateJpaAutoConfiguration::class,
    LiquibaseAutoConfiguration::class,
    TransactionAutoConfiguration::class,
    TransactionManagerCustomizationAutoConfiguration::class,
)
class SharedInfrastructureConfiguration
