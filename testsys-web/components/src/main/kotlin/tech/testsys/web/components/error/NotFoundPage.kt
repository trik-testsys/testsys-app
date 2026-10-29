package tech.testsys.web.components.error

import tech.testsys.web.components.texts.currentLocalization

/**
 * Branded missing-route screen available in every profile, with browser-history navigation only.
 *
 * @since 1.0.0-beta.1
 */
open class NotFoundPage : ErrorPage() {
    init {
        with(currentLocalization().uiFeedback) {
            show(title = errorNotFoundTitle(), description = errorNotFoundDescription(), pageTitle = errorNotFoundPageTitle())
        }
    }
}
