package tech.testsys.web.app.error

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.ErrorParameter
import com.vaadin.flow.router.InternalServerError
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.web.monitoring.WebFailures

/**
 * Handler of an unexpected exception while opening a page: counts it in [failures] and shows the default Vaadin screen.
 *
 * @since %CURRENT_VERSION%
 */
@AnonymousAllowed
class InternalErrorView(private val failures: WebFailures) : InternalServerError() {
    override fun setErrorParameter(event: BeforeEnterEvent, parameter: ErrorParameter<Exception>): Int {
        failures.record(WebFailures.NAVIGATION, parameter.caughtException)
        return super.setErrorParameter(event, parameter)
    }
}
