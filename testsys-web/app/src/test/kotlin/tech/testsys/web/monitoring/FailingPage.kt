package tech.testsys.web.monitoring

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed

/**
 * Page that fails with an unexpected exception while it is opened. It lies outside the scanned page packages, so tests
 * register it explicitly.
 */
@Route("monitoring-failure")
@AnonymousAllowed
class FailingPage : Div(), BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent): Unit = throw IllegalStateException("Page failed")
}
