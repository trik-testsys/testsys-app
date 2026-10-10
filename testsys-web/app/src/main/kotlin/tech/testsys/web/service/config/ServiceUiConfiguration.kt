package tech.testsys.web.service.config

import com.vaadin.flow.server.VaadinServiceInitListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tech.testsys.web.components.texts.UiTexts
import tech.testsys.web.components.texts.buildUiTexts
import tech.testsys.web.components.texts.initializeUiLocale

/**
 * Provides texts and locale initialization within the service child.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration(proxyBeanMethods = false)
class ServiceUiConfiguration {
    /**
     * Returns the shared design system texts for service pages.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun serviceUiTexts(): UiTexts = buildUiTexts()

    /**
     * Initializes every service UI in the locale of its texts.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun serviceUiLocale(texts: UiTexts): VaadinServiceInitListener = VaadinServiceInitListener { event ->
        event.source.addUIInitListener { uiEvent -> initializeUiLocale(uiEvent.ui, texts) }
    }
}
