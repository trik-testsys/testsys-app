package tech.testsys.web.service.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import tech.testsys.web.app.config.UiLocaleInitializer

/**
 * Provides UI localization within the service child.
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
}
