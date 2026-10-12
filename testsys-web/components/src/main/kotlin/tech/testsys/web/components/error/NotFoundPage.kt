package tech.testsys.web.components.error

import tech.testsys.web.components.texts.currentLocalization

/**
 * Branded missing-route screen available in every profile, with browser-history navigation only.
 *
 * @since %CURRENT_VERSION%
 */
open class NotFoundPage : ErrorPage() {
    init {
        with(currentLocalization().uiFeedback) {
            show(title = errorNotFoundTitle(), description = errorNotFoundDescription(), pageTitle = errorNotFoundPageTitle())
        }
    }
}
