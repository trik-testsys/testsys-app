package tech.testsys.web.service.error

import com.vaadin.flow.server.DefaultErrorHandler
import com.vaadin.flow.server.ErrorEvent
import com.vaadin.flow.server.ErrorHandler
import com.vaadin.flow.server.ServiceInitEvent
import com.vaadin.flow.server.VaadinServiceInitListener
import com.vaadin.flow.spring.annotation.SpringComponent
import tech.testsys.web.monitoring.WebFailures

/**
 * Error handler of every service session: counts an exception of an event handler or `UI.access` task in [failures]
 * and passes it to the default Vaadin handler.
 *
 * @since 1.0.0-beta.1
 */
@SpringComponent
class ServiceErrorHandler(private val failures: WebFailures) : VaadinServiceInitListener, ErrorHandler {
    override fun serviceInit(event: ServiceInitEvent) {
        event.source.addSessionInitListener { sessionEvent -> sessionEvent.session.errorHandler = this }
    }

    override fun error(event: ErrorEvent) {
        failures.record(WebFailures.EVENT, event.throwable)
        DefaultErrorHandler().error(event)
    }
}
