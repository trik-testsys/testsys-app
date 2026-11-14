package tech.testsys.web.service.view

import com.vaadin.flow.component.Component
import com.vaadin.flow.spring.annotation.SpringComponent
import com.vaadin.flow.spring.security.AuthenticationContext
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
 * Builds the service header without domain-user navigation or links into the main application.
 * Its companion holds the keys of the service sections.
 *
 * @since 1.0.0-beta.1
 */
@SpringComponent
class ServiceHeaders(private val authentication: AuthenticationContext) {
    /**
     * Returns the header of the signed-in service with the [active] section: the home page, the menu of the other
     * service pages grouped into columns and the menu with signing out.
     *
     * @since 1.0.0-beta.1
     */
    fun header(active: String? = null): CabinetHeader {
        val texts = currentLocalization().pageService
        val columns = listOf(
            menuColumn(
                texts.headerSystem(),
                texts.headerSettings() to ServiceSettingsView::class.java,
                texts.headerGrading() to ServiceGradingView::class.java,
                texts.headerTrikStudioVersions() to ServiceTrikStudioVersionsView::class.java,
            ),
            menuColumn(
                texts.headerData(),
                texts.headerUsers() to ServiceUsersView::class.java,
                texts.headerAnnouncements() to ServiceAnnouncementsView::class.java,
            ),
            menuColumn(texts.headerTools(), texts.headerIdDecoding() to ServiceIdDecodingView::class.java),
        )
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

    companion object {
        const val HOME_SECTION: String = "home"
        const val MENU_SECTION: String = "menu"

        private fun menuColumn(title: String, vararg links: Pair<String, Class<out Component>>): HeaderMegaColumn = HeaderMegaColumn(
            title = title,
            links = links.map { (label, page) -> HeaderMegaLink(label = label, destination = HeaderDestination.Route(page)) },
        )
    }
}
