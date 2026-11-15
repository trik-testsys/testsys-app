package tech.testsys.web.service.view

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.spring.annotation.SpringComponent
import com.vaadin.flow.spring.security.AuthenticationContext
import tech.testsys.bootstrap.ServiceWebSettings
import tech.testsys.web.components.navigation.header.CabinetHeader
import tech.testsys.web.components.navigation.header.HeaderDestination
import tech.testsys.web.components.navigation.header.HeaderMegaColumn
import tech.testsys.web.components.navigation.header.HeaderMegaLink
import tech.testsys.web.components.navigation.header.HeaderMegaMenu
import tech.testsys.web.components.navigation.header.HeaderUser
import tech.testsys.web.components.navigation.header.MegaMenuItem
import tech.testsys.web.components.navigation.header.NavItem
import tech.testsys.web.components.texts.currentLocalization

/**
 * Builds the service header without domain-user navigation and lists the service sections grouped as in its menu;
 * Grafana is the only section outside the service cabinet. Its companion holds the keys of the header sections.
 *
 * @since 1.0.0-beta.1
 */
@SpringComponent
class ServiceHeaders(private val authentication: AuthenticationContext, private val settings: ServiceWebSettings) {
    /**
     * Returns the header of the signed-in service with the [active] section: the home page, the menu of the service
     * sections grouped into columns and the menu with signing out.
     *
     * @since 1.0.0-beta.1
     */
    fun header(active: String? = null): CabinetHeader {
        val texts = currentLocalization().pageService
        val columns = groups().map { group ->
            HeaderMegaColumn(
                title = group.title,
                links = group.sections.map { section -> HeaderMegaLink(label = section.label, destination = section.destination) },
            )
        }
        return CabinetHeader(
            items = listOf(
                NavItem(key = HOME_SECTION, label = texts.headerHome(), destination = HeaderDestination.Route(ServiceHomeView::class.java)),
                MegaMenuItem(key = MENU_SECTION, label = texts.headerMenu(), menu = HeaderMegaMenu(columns), isSearchable = true),
            ),
            active = active,
            // The service identity has no person behind it, so the header names the service.
            user = HeaderUser(name = texts.headerUser()) { destructiveItem(texts.headerSignOut()) { authentication.logout() } },
        )
    }

    /** Returns the service sections in the groups and order of the header menu. */
    internal fun groups(): List<ServiceGroup> {
        val texts = currentLocalization().pageService
        return listOf(
            ServiceGroup(
                texts.headerSystem(),
                listOf(
                    section(texts.headerSettings(), texts.homeDescriptionSettings(), ServiceSettingsView::class.java),
                    section(texts.headerGrading(), texts.homeDescriptionGrading(), ServiceGradingView::class.java),
                    section(
                        texts.headerTrikStudioVersions(),
                        texts.homeDescriptionTrikStudioVersions(),
                        ServiceTrikStudioVersionsView::class.java,
                    ),
                ),
            ),
            ServiceGroup(
                texts.headerData(),
                listOf(
                    section(texts.headerUsers(), texts.homeDescriptionUsers(), ServiceUsersView::class.java),
                    section(texts.headerAnnouncements(), texts.homeDescriptionAnnouncements(), ServiceAnnouncementsView::class.java),
                ),
            ),
            ServiceGroup(
                texts.headerTools(),
                listOf(
                    section(texts.headerIdDecoding(), texts.homeDescriptionIdDecoding(), ServiceIdDecodingView::class.java),
                    // Grafana is entered with its own accounts, so the plain transition passes no service credentials.
                    ServiceSection(
                        label = texts.headerGrafana(),
                        description = texts.homeDescriptionGrafana(),
                        destination = HeaderDestination.Action { UI.getCurrent().page.setLocation(settings.grafanaUrl) },
                    ),
                ),
            ),
        )
    }

    companion object {
        const val HOME_SECTION: String = "home"
        const val MENU_SECTION: String = "menu"

        private fun section(label: String, description: String, page: Class<out Component>): ServiceSection =
            ServiceSection(label = label, description = description, destination = HeaderDestination.Route(page))
    }
}

/** Group of service sections titled [title], shown as a column of the header menu and a block of the home page. */
internal data class ServiceGroup(val title: String, val sections: List<ServiceSection>)

/** Service section named [label] with its [description] on the home page, opened through [destination]. */
internal data class ServiceSection(val label: String, val description: String, val destination: HeaderDestination)
