package tech.testsys.web.service.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.bootstrap.ServiceWebSettings
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.display.text
import tech.testsys.web.components.texts.currentLocalization

/**
 * Protected initial service page: the header leads to the service sections, and the monitoring block makes a plain
 * transition to Grafana; signing out is in the header menu.
 *
 * @since 1.0.0-beta.1
 */
@Route("")
@RolesAllowed("SERVICE")
class ServiceHomeView(headers: ServiceHeaders, settings: ServiceWebSettings) :
    TestSysView(),
    HasDynamicTitle {
    init {
        val texts = currentLocalization().pageService
        page(headers.header(active = ServiceHeaders.HOME_SECTION)) {
            head(texts.homeTitle())
            row {
                block(title = texts.homeMonitoring()) {
                    row { text(texts.homeMonitoringText()) }
                    actions {
                        action(texts.homeGrafana()) { onClick { UI.getCurrent().page.setLocation(settings.grafanaUrl) } }
                    }
                }
            }
        }
    }

    override fun getPageTitle(): String = currentLocalization().pageService.homeTitle()
}
