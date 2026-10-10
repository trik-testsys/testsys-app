package tech.testsys.web.service.error

import com.vaadin.flow.router.AccessDeniedException
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.ErrorParameter
import com.vaadin.flow.router.HasErrorParameter
import com.vaadin.flow.server.HttpStatusCode
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.web.components.error.ErrorPage
import tech.testsys.web.components.texts.UiTexts

/**
 * Handles navigation denied by service access rules with the shared forbidden screen and status 403.
 *
 * @since %CURRENT_VERSION%
 */
@AnonymousAllowed
class ServiceAccessDeniedView(private val texts: UiTexts) : ErrorPage(texts), HasErrorParameter<AccessDeniedException> {
    override fun setErrorParameter(event: BeforeEnterEvent, parameter: ErrorParameter<AccessDeniedException>): Int {
        refreshHistoryNavigation()
        show(title = texts.forbidden.title, description = texts.forbidden.description, pageTitle = texts.forbidden.pageTitle)
        return HttpStatusCode.FORBIDDEN.code
    }
}
