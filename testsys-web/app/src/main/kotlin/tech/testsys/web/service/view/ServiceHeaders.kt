package tech.testsys.web.service.view

import com.vaadin.flow.spring.annotation.SpringComponent
import com.vaadin.flow.spring.security.AuthenticationContext
import tech.testsys.web.components.navigation.header.CabinetHeader
import tech.testsys.web.components.navigation.header.HeaderDestination
import tech.testsys.web.components.navigation.header.HeaderUser
import tech.testsys.web.components.navigation.header.HeaderUserMenu
import tech.testsys.web.components.navigation.header.HeaderUserMenuItem
import tech.testsys.web.components.navigation.header.NavItem

/**
 * Builds the service header without domain-user navigation or links into the main application.
 * Its companion holds the keys of the service sections.
 *
 * @since %CURRENT_VERSION%
 */
@SpringComponent
class ServiceHeaders(private val authentication: AuthenticationContext) {
    /**
     * Returns the header of the signed-in service with the [active] section: the sections of the service cabinet and
     * the menu with signing out.
     *
     * @since %CURRENT_VERSION%
     */
    fun header(active: String? = null): CabinetHeader = CabinetHeader(
        items = listOf(
            NavItem(key = HOME_SECTION, label = "Главная", target = ServiceHomeView::class.java),
            NavItem(key = SETTINGS_SECTION, label = "Настройки", target = ServiceSettingsView::class.java),
            NavItem(key = GRADING_SECTION, label = "Проверка", target = ServiceGradingView::class.java),
            NavItem(key = USERS_SECTION, label = "Пользователи", target = ServiceUsersView::class.java),
            NavItem(key = VERSIONS_SECTION, label = "Версии TRIK Studio", target = ServiceTrikStudioVersionsView::class.java),
        ),
        active = active,
        user = HeaderUser(
            name = USER_NAME,
            menu = HeaderUserMenu(
                listOf(
                    HeaderUserMenuItem(
                        label = "Выйти",
                        destination = HeaderDestination.Action { authentication.logout() },
                        isDestructive = true,
                    ),
                ),
            ),
        ),
    )

    companion object {
        const val HOME_SECTION: String = "home"
        const val SETTINGS_SECTION: String = "settings"
        const val GRADING_SECTION: String = "grading"
        const val USERS_SECTION: String = "users"
        const val VERSIONS_SECTION: String = "trik-studio-versions"

        /** Name shown in the header: the service identity has no person behind it. */
        private const val USER_NAME: String = "Служба"
    }
}
