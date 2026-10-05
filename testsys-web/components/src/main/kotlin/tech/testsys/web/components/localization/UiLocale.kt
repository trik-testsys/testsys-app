package tech.testsys.web.components.localization

import com.vaadin.flow.component.UI
import com.vaadin.flow.component.page.Page
import tech.testsys.web.components.UiTexts

/**
 * Initializes a UI locale and the browser document language from [texts].
 *
 * @since %CURRENT_VERSION%
 */
fun initializeUiLocale(ui: UI, texts: UiTexts) {
    ui.locale = texts.locale
    ui.page.setDocumentLanguage(texts.locale.toLanguageTag())
}

private fun Page.setDocumentLanguage(languageTag: String) = executeJs("document.documentElement.lang = \$0", languageTag)
