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
import tech.testsys.web.components.texts.UiTexts

/**
 * Start page of the system for a guest (testsys.web.page.main); a signed-in user goes to their start page.
 *
 * @since %CURRENT_VERSION%
 */
@Route("")
@AnonymousAllowed
class MainView(texts: UiTexts, headers: CabinetHeaders) : TestSysView(texts), BeforeEnterObserver {
    init {
        page(headers.guest()) {
            guestBlock(title = "TestSys") {
                row { text("Войдите по Коду-доступа или зарегистрируйтесь как Ученик или Организатор.") }
                footer {
                    mainAction("Войти") { onClick { UI.getCurrent().navigate(AuthenticationView::class.java) } }
                    action("Зарегистрироваться") { onClick { UI.getCurrent().navigate(RegistrationView::class.java) } }
                }
            }
        }
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        val principal = CabinetSignIn.principal() ?: return
        event.forwardTo(startPageOf(principal.kind))
    }
}
