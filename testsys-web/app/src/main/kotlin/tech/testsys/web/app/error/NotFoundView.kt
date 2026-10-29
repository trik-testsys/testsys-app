package tech.testsys.web.app.error

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.ErrorParameter
import com.vaadin.flow.router.HasErrorParameter
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.server.HttpStatusCode
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.web.components.error.NotFoundPage

/**
 * Missing-route handler backed by the shared branded page.
 *
 * @since 1.0.0-beta.1
 */
@AnonymousAllowed
class NotFoundView : NotFoundPage(), HasErrorParameter<NotFoundException> {
    override fun setErrorParameter(event: BeforeEnterEvent, parameter: ErrorParameter<NotFoundException>): Int {
        refreshHistoryNavigation()
        return HttpStatusCode.NOT_FOUND.code
    }
}
