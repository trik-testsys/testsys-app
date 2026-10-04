package tech.testsys.web.components.navigation

import com.vaadin.flow.component.Component
import com.vaadin.flow.router.RouteParameters

/**
 * Section of the Cabinet header.
 *
 * @property key the identifier compared with the active section.
 * @property label the shown name of the section.
 * @since %CURRENT_VERSION%
 */
sealed interface HeaderItem {
    val key: String
    val label: String
}

/**
 * Section link of the Cabinet header.
 *
 * @property target the route the link opens.
 * @since %CURRENT_VERSION%
 */
data class NavItem(override val key: String, override val label: String, val target: Class<out Component>) : HeaderItem

/**
 * Section that opens a menu without requiring a route.
 *
 * @property menu the columns and optional promotion of the section.
 * @since %CURRENT_VERSION%
 */
data class MegaMenuItem(override val key: String, override val label: String, val menu: HeaderMegaMenu) : HeaderItem

/**
 * Signed-in user shown in the Cabinet header.
 *
 * @property name the full name of the user.
 * @property menu the optional links and actions of the user.
 * @since %CURRENT_VERSION%
 */
data class HeaderUser(val name: String, val menu: HeaderUserMenu? = null)

/**
 * Top bar of a Cabinet page: the brand, sections and optional search, notifications and user menu.
 *
 * @property items the sections.
 * @property active the key of the current section, or `null` if no section is current.
 * @property user the signed-in user, or `null` for a guest.
 * @property signIn the route of the sign-in page shown to a guest, or `null` to hide the link.
 * @property search the optional background search, also available to guests.
 * @property notifications the optional application-owned notifications, shown only to signed-in users.
 * @property menuSearchKey the optional mega-menu key filtered by the search field instead of a provider search.
 * @property signInParameters route parameters of the guest sign-in link, empty by default.
 * @since %CURRENT_VERSION%
 */
data class CabinetHeader(
    val items: List<HeaderItem> = emptyList(),
    val active: String? = null,
    val user: HeaderUser? = null,
    val signIn: Class<out Component>? = null,
    val search: HeaderSearch? = null,
    val notifications: HeaderNotifications? = null,
    val menuSearchKey: String? = null,
    val signInParameters: RouteParameters = RouteParameters.empty(),
)
