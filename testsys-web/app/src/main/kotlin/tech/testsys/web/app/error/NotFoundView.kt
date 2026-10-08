package tech.testsys.web.app.error

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.ErrorParameter
import com.vaadin.flow.router.HasErrorParameter
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.server.HttpStatusCode
import tech.testsys.web.components.error.NotFoundPage
import tech.testsys.web.components.texts.UiTexts

/**
 * Missing-route handler backed by the shared branded page.
 *
 * @since %CURRENT_VERSION%
 */
class NotFoundView(texts: UiTexts) : NotFoundPage(texts), HasErrorParameter<NotFoundException> {
    override fun setErrorParameter(event: BeforeEnterEvent, parameter: ErrorParameter<NotFoundException>): Int {
        refreshHistoryNavigation()
        return HttpStatusCode.NOT_FOUND.code
    }
}
