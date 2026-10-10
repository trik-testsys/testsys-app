package tech.testsys.web.app

import com.vaadin.flow.spring.annotation.EnableVaadin
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.TypeExcludeFilter
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.EnableTransactionManagement
import tech.testsys.bootstrap.TestSysRuntime
import tech.testsys.bootstrap.WebAutoConfiguration
import tech.testsys.web.app.config.InfraConfiguration
import tech.testsys.web.app.config.OperationsConfiguration

/**
 * Main web child using its parent's infrastructure and explicit auto-configuration imports.
 * The application annotation also lets development tools discover this child.
 *
 * @since %CURRENT_VERSION%
 */
@SpringBootApplication
@ComponentScan(
    basePackages = ["tech.testsys.web.app"],
    excludeFilters = [
        ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [InfraConfiguration::class, OperationsConfiguration::class]),
        ComponentScan.Filter(type = FilterType.CUSTOM, classes = [TypeExcludeFilter::class]),
    ],
)
@EnableVaadin("tech.testsys.web.app", "tech.testsys.web.shell")
@EnableTransactionManagement
@Import(WebAutoConfiguration::class)
class TestSysApplication

/**
 * Starts the infrastructure and both web applications with one shutdown hook.
 *
 * @since %CURRENT_VERSION%
 */
fun main(args: Array<String>) {
    TestSysRuntime.start(args).installShutdownHook()
}
