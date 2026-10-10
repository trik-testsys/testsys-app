package tech.testsys.web.service.error

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.ErrorParameter
import com.vaadin.flow.router.HasErrorParameter
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.server.HttpStatusCode
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.web.components.error.NotFoundPage
import tech.testsys.web.components.texts.UiTexts

/**
 * Missing-route handler backed by the shared branded page.
 *
 * @since %CURRENT_VERSION%
 */
@AnonymousAllowed
class ServiceNotFoundView(texts: UiTexts) : NotFoundPage(texts), HasErrorParameter<NotFoundException> {
    override fun setErrorParameter(event: BeforeEnterEvent, parameter: ErrorParameter<NotFoundException>): Int {
        refreshHistoryNavigation()
        return HttpStatusCode.NOT_FOUND.code
    }
}
