package tech.testsys.web.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tech.testsys.web.components.texts.UiTexts
import tech.testsys.web.components.texts.buildUiTexts

/**
 * Provides the design system texts to the pages.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration
class UiTextsConfiguration {
    /**
     * Texts of the design system components in the only supported region.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun uiTexts(): UiTexts = buildUiTexts()
}
