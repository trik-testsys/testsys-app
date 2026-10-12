package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.web.app.security.CabinetSignIn
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.display.text
import tech.testsys.web.components.texts.currentLocalization

/**
 * Start page of the system for a guest (testsys.web.page.main); a signed-in user goes to their start page.
 *
 * @since %CURRENT_VERSION%
 */
@Route("")
@AnonymousAllowed
class MainView(headers: CabinetHeaders) : TestSysView(), BeforeEnterObserver {
    init {
        val localization = currentLocalization()
        val texts = localization.pageGuest
        page(headers.guest()) {
            head(localization.uiBrand.name())
            guide(texts.startGuide())
            guestBlock {
                guide(texts.startBlockGuide(), title = texts.startBlockGuideTitle())
                row { text(texts.startText()) }
                footer {
                    mainAction(texts.signInButton()) { onClick { UI.getCurrent().navigate(AuthenticationView::class.java) } }
                    action(texts.register()) { onClick { UI.getCurrent().navigate(RegistrationView::class.java) } }
                }
            }
        }
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        val principal = CabinetSignIn.principal() ?: return
        event.forwardTo(startPageOf(principal.kind))
    }
}
