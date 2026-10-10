package tech.testsys.web.app.error

import com.vaadin.flow.router.AccessDeniedException
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.ErrorParameter
import com.vaadin.flow.router.HasErrorParameter
import com.vaadin.flow.server.HttpStatusCode
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.web.components.error.ErrorPage
import tech.testsys.web.components.texts.UiTexts

/**
 * Handler of a page closed to the signed-in user of another kind: shows the forbidden screen with 403.
 *
 * @since %CURRENT_VERSION%
 */
@AnonymousAllowed
class AccessDeniedView(private val texts: UiTexts) : ErrorPage(texts), HasErrorParameter<AccessDeniedException> {
    override fun setErrorParameter(event: BeforeEnterEvent, parameter: ErrorParameter<AccessDeniedException>): Int {
        refreshHistoryNavigation()
        show(title = texts.forbidden.title, description = texts.forbidden.description, pageTitle = texts.forbidden.pageTitle)
        return HttpStatusCode.FORBIDDEN.code
    }
}
