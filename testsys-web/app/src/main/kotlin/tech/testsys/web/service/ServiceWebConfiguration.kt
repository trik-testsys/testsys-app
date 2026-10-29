package tech.testsys.web.service

import com.vaadin.flow.spring.annotation.EnableVaadin
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.TypeExcludeFilter
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import tech.testsys.bootstrap.WebAutoConfiguration

/**
 * Isolated service web child using explicit auto-configuration imports and scanning only service pages and the common shell.
 * The application annotation also lets development tools discover this child.
 *
 * @since 1.0.0-beta.1
 */
@SpringBootApplication(proxyBeanMethods = false)
@ComponentScan(
    basePackages = ["tech.testsys.web.service"],
    excludeFilters = [ComponentScan.Filter(type = FilterType.CUSTOM, classes = [TypeExcludeFilter::class])],
)
@EnableVaadin("tech.testsys.web.service", "tech.testsys.web.shell")
@Import(WebAutoConfiguration::class)
class ServiceWebConfiguration
