package tech.testsys.web.service.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.spring.security.AuthenticationContext
import jakarta.annotation.security.RolesAllowed
import tech.testsys.bootstrap.ServiceWebSettings
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.display.text

/**
 * Protected initial service page with service sections, a plain transition to Grafana and an explicit sign-out.
 *
 * @since %CURRENT_VERSION%
 */
@Route("")
@PageTitle("Служебный кабинет")
@RolesAllowed("SERVICE")
class ServiceHomeView(authentication: AuthenticationContext, settings: ServiceWebSettings) : TestSysView() {
    init {
        page(ServiceHeaders.header()) {
            head("Служебный кабинет")
            row {
                block {
                    row { text("Вход выполнен") }
                    actions {
                        action("Настройки") { onClick { UI.getCurrent().navigate(ServiceSettingsView::class.java) } }
                        action("Проверка") { onClick { UI.getCurrent().navigate(ServiceGradingView::class.java) } }
                        action("Пользователи") { onClick { UI.getCurrent().navigate(ServiceUsersView::class.java) } }
                        action("Версии TRIK Studio") {
                            onClick { UI.getCurrent().navigate(ServiceTrikStudioVersionsView::class.java) }
                        }
                        action("Grafana") { onClick { UI.getCurrent().page.setLocation(settings.grafanaUrl) } }
                    }
                    footer { mainAction("Выйти") { onClick { authentication.logout() } } }
                }
            }
        }
    }
}
