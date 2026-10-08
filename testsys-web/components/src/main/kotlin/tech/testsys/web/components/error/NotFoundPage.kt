package tech.testsys.web.components.error

import tech.testsys.web.components.texts.UiTexts

/**
 * Branded missing-route screen available in every profile, with browser-history navigation only.
 *
 * @since %CURRENT_VERSION%
 */
open class NotFoundPage(texts: UiTexts) : ErrorPage(texts) {
    init {
        show(title = texts.notFound.title, description = texts.notFound.description, pageTitle = texts.notFound.pageTitle)
    }
}
