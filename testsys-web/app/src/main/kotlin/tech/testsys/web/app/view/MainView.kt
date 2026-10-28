package tech.testsys.web.app.view

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.web.app.security.CabinetSignIn
import tech.testsys.web.components.TestSysView

/**
 * Address of the system without a path (testsys.web.page.main): a guest goes to signing in, a signed-in user goes to
 * their start page.
 *
 * @since %CURRENT_VERSION%
 */
@Route("")
@AnonymousAllowed
class MainView : TestSysView(), BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        val principal = CabinetSignIn.principal()
        event.forwardTo(if (principal == null) AuthenticationView::class.java else startPageOf(principal.kind))
    }
}
