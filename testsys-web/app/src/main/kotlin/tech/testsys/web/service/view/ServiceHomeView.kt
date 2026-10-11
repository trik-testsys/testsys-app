package tech.testsys.web.service.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.spring.security.AuthenticationContext
import jakarta.annotation.security.RolesAllowed
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.display.text
import tech.testsys.web.components.texts.UiTexts

/**
 * Protected initial service page with an explicit action to close the service session.
 *
 * @since %CURRENT_VERSION%
 */
@Route("")
@PageTitle("Служебный кабинет")
@RolesAllowed("SERVICE")
class ServiceHomeView(texts: UiTexts, authentication: AuthenticationContext) : TestSysView(texts) {
    init {
        page(ServiceHeaders.header()) {
            head("Служебный кабинет")
            row {
                block {
                    row { text("Вход выполнен") }
                    actions { action("Настройки") { onClick { UI.getCurrent().navigate(ServiceSettingsView::class.java) } } }
                    footer { mainAction("Выйти") { onClick { authentication.logout() } } }
                }
            }
        }
    }
}
