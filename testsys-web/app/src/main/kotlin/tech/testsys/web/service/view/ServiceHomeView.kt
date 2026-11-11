package tech.testsys.web.service.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.bootstrap.ServiceWebSettings
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.display.text

/**
 * Protected initial service page: the header leads to the service sections, and the monitoring block makes a plain
 * transition to Grafana; signing out is in the header menu.
 *
 * @since 1.0.0-beta.1
 */
@Route("")
@PageTitle("Служебный кабинет")
@RolesAllowed("SERVICE")
class ServiceHomeView(headers: ServiceHeaders, settings: ServiceWebSettings) : TestSysView() {
    init {
        page(headers.header(active = ServiceHeaders.HOME_SECTION)) {
            head("Служебный кабинет")
            row {
                block(title = "Мониторинг") {
                    row { text("Метрики Системы открываются в Grafana. В Grafana входят её собственными учётными записями.") }
                    actions {
                        action("Grafana") { onClick { UI.getCurrent().page.setLocation(settings.grafanaUrl) } }
                    }
                }
            }
        }
    }
}
