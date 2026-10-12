package tech.testsys.web.app.error

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.ErrorParameter
import com.vaadin.flow.router.HasErrorParameter
import com.vaadin.flow.server.HttpStatusCode
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.operation.error.AccessDeniedError
import tech.testsys.operation.error.ContestNotAddedToClassError
import tech.testsys.operation.error.ContestNotAddedToCompetitionError
import tech.testsys.operation.error.ContestNotEnteredError
import tech.testsys.operation.error.EntityNotExistsError
import tech.testsys.operation.error.MissedRequiredRoleError
import tech.testsys.operation.error.OperationException
import tech.testsys.operation.error.ResourceAccessError
import tech.testsys.web.components.error.ErrorPage
import tech.testsys.web.components.texts.currentLocalization

/**
 * Handler of a failed operation during navigation: a missing object or a contest outside the user's class or
 * competition shows the missing page screen with 404, missing access or role or a contest not entered yet shows 403,
 * and any other expected refusal shows the failed page screen with 400. Server faults never reach this handler.
 *
 * @since %CURRENT_VERSION%
 */
@AnonymousAllowed
class OperationErrorView : ErrorPage(), HasErrorParameter<OperationException> {
    override fun setErrorParameter(event: BeforeEnterEvent, parameter: ErrorParameter<OperationException>): Int {
        refreshHistoryNavigation()

        val texts = currentLocalization().uiFeedback
        return when (parameter.exception.error) {
            is EntityNotExistsError, is ContestNotAddedToClassError, is ContestNotAddedToCompetitionError -> {
                show(
                    title = texts.errorNotFoundTitle(),
                    description = texts.errorNotFoundDescription(),
                    pageTitle = texts.errorNotFoundPageTitle(),
                )
                HttpStatusCode.NOT_FOUND.code
            }

            is AccessDeniedError, is MissedRequiredRoleError, is ResourceAccessError, is ContestNotEnteredError -> {
                show(
                    title = texts.errorForbiddenTitle(),
                    description = texts.errorForbiddenDescription(),
                    pageTitle = texts.errorForbiddenPageTitle(),
                )
                HttpStatusCode.FORBIDDEN.code
            }

            else -> {
                show(
                    title = texts.errorPageFailedTitle(),
                    description = texts.errorPageFailedDescription(),
                    pageTitle = texts.errorPageFailedPageTitle(),
                )
                HttpStatusCode.BAD_REQUEST.code
            }
        }
    }
}
