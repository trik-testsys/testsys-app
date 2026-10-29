package tech.testsys.web.app.error

import com.vaadin.flow.router.AccessDeniedException
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.ErrorParameter
import com.vaadin.flow.router.HasErrorParameter
import com.vaadin.flow.server.HttpStatusCode
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.web.components.error.ErrorPage
import tech.testsys.web.components.texts.currentLocalization

/**
 * Handler of a page closed to the signed-in user of another kind: shows the forbidden screen with 403.
 *
 * @since 1.0.0-beta.1
 */
@AnonymousAllowed
class AccessDeniedView : ErrorPage(), HasErrorParameter<AccessDeniedException> {
    override fun setErrorParameter(event: BeforeEnterEvent, parameter: ErrorParameter<AccessDeniedException>): Int {
        refreshHistoryNavigation()
        with(currentLocalization().uiFeedback) {
            show(title = errorForbiddenTitle(), description = errorForbiddenDescription(), pageTitle = errorForbiddenPageTitle())
        }
        return HttpStatusCode.FORBIDDEN.code
    }
}
