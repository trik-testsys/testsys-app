package tech.testsys.web.service.config

import com.vaadin.flow.server.VaadinServiceInitListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import tech.testsys.web.app.config.UiLocaleInitializer
import tech.testsys.web.components.layout.setFooterVersion

/**
 * Provides UI localization and the footer version within the service child.
 *
 * @since 1.0.0-beta.1
 */
@Configuration(proxyBeanMethods = false)
class ServiceUiConfiguration {
    /**
     * Binds the localization of the user's region to every service UI, as in the main interface.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun serviceUiLocale(environment: Environment): UiLocaleInitializer = UiLocaleInitializer(environment)

    /**
     * Shows the version of the system from `spring.application.version` in the footer of every service UI, as in the
     * main interface.
     *
     * @since 1.0.0-beta.2
     */
    @Bean
    fun serviceFooterVersion(environment: Environment): VaadinServiceInitListener {
        val version = environment.getProperty("spring.application.version")
        return VaadinServiceInitListener { event ->
            event.source.addUIInitListener { uiEvent -> setFooterVersion(uiEvent.ui, version) }
        }
    }
}
