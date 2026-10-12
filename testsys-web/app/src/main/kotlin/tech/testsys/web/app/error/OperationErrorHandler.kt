package tech.testsys.web.app.error

import com.vaadin.flow.server.DefaultErrorHandler
import com.vaadin.flow.server.ErrorEvent
import com.vaadin.flow.server.ErrorHandler
import com.vaadin.flow.server.ServiceInitEvent
import com.vaadin.flow.server.VaadinServiceInitListener
import com.vaadin.flow.spring.annotation.SpringComponent
import tech.testsys.operation.error.AccessDeniedError
import tech.testsys.operation.error.EntityNotExistsError
import tech.testsys.operation.error.MissedRequiredRoleError
import tech.testsys.operation.error.OperationError
import tech.testsys.operation.error.OperationException
import tech.testsys.operation.error.ResourceAccessError
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.texts.currentLocalization
import tech.testsys.web.monitoring.WebFailures

/**
 * Error handler of every session: a failed operation in an event handler shows an error toast by the kind
 * of the failure, any other exception is counted in [failures] and goes to the default Vaadin handler.
 *
 * @since %CURRENT_VERSION%
 */
@SpringComponent
class OperationErrorHandler(private val failures: WebFailures) : VaadinServiceInitListener, ErrorHandler {
    override fun serviceInit(event: ServiceInitEvent) {
        event.source.addSessionInitListener { sessionEvent -> sessionEvent.session.errorHandler = this }
    }

    override fun error(event: ErrorEvent) {
        failures.record(WebFailures.EVENT, event.throwable)
        val failure = generateSequence(event.throwable) { throwable -> throwable.cause }
            .filterIsInstance<OperationException>()
            .firstOrNull()
        if (failure == null) {
            DefaultErrorHandler().error(event)
            return
        }

        toast(FeedbackKind.Error, title(failure.error))
    }

    private fun title(error: OperationError): String = with(currentLocalization().uiFeedback) {
        when (error) {
            is EntityNotExistsError -> errorFailureNotFound()
            is AccessDeniedError -> errorFailureAccessDenied()
            is MissedRequiredRoleError -> errorFailureMissingRole()
            is ResourceAccessError -> errorFailureResourceNotInTask()
            else -> errorFailureFailed()
        }
    }
}
