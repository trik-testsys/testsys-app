package tech.testsys.web.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tech.testsys.infra.localization.bundle.SupportedRegion
import tech.testsys.web.ui.UiTexts

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
    fun uiTexts(): UiTexts = buildUiTexts(SupportedRegion.RU)
}
