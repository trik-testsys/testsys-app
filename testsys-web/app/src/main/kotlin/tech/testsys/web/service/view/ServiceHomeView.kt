package tech.testsys.web.service.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.web.app.view.pageOf
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.data.table
import tech.testsys.web.components.navigation.header.HeaderDestination
import tech.testsys.web.components.texts.currentLocalization

/**
 * Protected initial service page: a block for every group of the header menu lists its sections with their purpose,
 * and a row opens its section; signing out is in the header menu.
 *
 * @since 1.0.0-beta.1
 */
@Route("")
@RolesAllowed("SERVICE")
class ServiceHomeView(headers: ServiceHeaders) :
    TestSysView(),
    HasDynamicTitle {
    init {
        val texts = currentLocalization().pageService
        page(headers.header(active = ServiceHeaders.HOME_SECTION)) {
            head(texts.homeTitle())
            row {
                headers.groups().forEach { group ->
                    block(title = group.title, size = 8) {
                        table(key = { section: ServiceSection -> section.label }, fetch = { request -> pageOf(group.sections, request) }) {
                            textColumn(texts.homeSection(), size = 9) { section -> section.label }
                            textColumn(texts.homePurpose()) { section -> section.description }
                            onRowClick(isNavigation = true) { section -> open(section.destination) }
                        }
                    }
                }
            }
        }
    }

    override fun getPageTitle(): String = currentLocalization().pageService.homeTitle()

    private fun open(destination: HeaderDestination) {
        when (destination) {
            is HeaderDestination.Route -> UI.getCurrent().navigate(destination.target, destination.parameters)
            is HeaderDestination.Action -> destination.onSelect()
        }
    }
}
