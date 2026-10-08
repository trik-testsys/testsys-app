package tech.testsys.web.devapp.config

import com.vaadin.flow.server.ServiceInitEvent
import com.vaadin.flow.server.VaadinServiceInitListener
import com.vaadin.flow.spring.annotation.SpringComponent
import tech.testsys.web.components.texts.UiTexts
import tech.testsys.web.components.texts.initializeUiLocale

/**
 * Sets the locale of every new UI and the `lang` attribute of the page from the design system texts.
 *
 * @since %CURRENT_VERSION%
 */
@SpringComponent
class UiLocaleInitializer(private val texts: UiTexts) : VaadinServiceInitListener {
    override fun serviceInit(event: ServiceInitEvent) {
        event.source.addUIInitListener { uiEvent ->
            initializeUiLocale(uiEvent.ui, texts)
        }
    }
}
