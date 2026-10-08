package tech.testsys.web.app.error

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.ErrorParameter
import com.vaadin.flow.router.HasErrorParameter
import com.vaadin.flow.server.HttpStatusCode
import tech.testsys.operation.error.AccessDeniedError
import tech.testsys.operation.error.EntityNotExistsError
import tech.testsys.operation.error.MissedRequiredRoleError
import tech.testsys.operation.error.OperationException
import tech.testsys.operation.error.ResourceAccessError
import tech.testsys.web.components.error.ErrorPage
import tech.testsys.web.components.texts.ErrorPageTexts
import tech.testsys.web.components.texts.UiTexts

/**
 * Handler of a failed operation during navigation: a missing object shows the missing page screen with 404,
 * missing access or role shows 403, and any other failure shows the failed page screen with 500.
 *
 * @since %CURRENT_VERSION%
 */
class OperationErrorView(private val texts: UiTexts) : ErrorPage(texts), HasErrorParameter<OperationException> {
    override fun setErrorParameter(event: BeforeEnterEvent, parameter: ErrorParameter<OperationException>): Int {
        refreshHistoryNavigation()

        return when (parameter.exception.error) {
            is EntityNotExistsError -> {
                show(title = texts.notFound.title, description = texts.notFound.description, pageTitle = texts.notFound.pageTitle)
                HttpStatusCode.NOT_FOUND.code
            }

            is AccessDeniedError, is MissedRequiredRoleError, is ResourceAccessError -> {
                show(texts.forbidden)
                HttpStatusCode.FORBIDDEN.code
            }

            else -> {
                show(texts.pageFailed)
                HttpStatusCode.INTERNAL_SERVER_ERROR.code
            }
        }
    }

    private fun show(screen: ErrorPageTexts) = show(title = screen.title, description = screen.description, pageTitle = screen.pageTitle)
}
