package tech.testsys.web.service.error

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.ErrorParameter
import com.vaadin.flow.router.InternalServerError
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.web.monitoring.WebFailures

/**
 * Handles an unexpected exception while opening a service page: counts it in [failures] and shows the default
 * Vaadin screen.
 *
 * @since 1.0.0-beta.1
 */
@AnonymousAllowed
class ServiceInternalErrorView(private val failures: WebFailures) : InternalServerError() {
    override fun setErrorParameter(event: BeforeEnterEvent, parameter: ErrorParameter<Exception>): Int {
        failures.record(WebFailures.NAVIGATION, parameter.caughtException)
        return super.setErrorParameter(event, parameter)
    }
}
