package tech.testsys.web.components.navigation.header

import tech.testsys.web.components.overlay.MenuScope

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
 * @property destination the route the link opens.
 * @since %CURRENT_VERSION%
 */
data class NavItem(override val key: String, override val label: String, val destination: HeaderDestination.Route) : HeaderItem

/**
 * Section that opens a menu without requiring a route.
 *
 * @property menu the columns of the section.
 * @property isSearchable whether the search field after the sections filters this menu, also for guests.
 * @since %CURRENT_VERSION%
 */
data class MegaMenuItem(
    override val key: String,
    override val label: String,
    val menu: HeaderMegaMenu,
    val isSearchable: Boolean = false,
) : HeaderItem

/**
 * Signed-in user shown in the Cabinet header.
 *
 * @property name the full name of the user.
 * @property menu the optional links and actions of the user, or `null` to show the user without a menu.
 * @since %CURRENT_VERSION%
 */
data class HeaderUser(val name: String, val menu: (MenuScope.() -> Unit)? = null)

/**
 * Top bar of a Cabinet page: the brand, sections and optional menu search, notifications and user menu.
 *
 * @property items the sections with unique keys; at most one of them is searchable.
 * @property active the key of the current section, or `null` if no section is current.
 * @property user the signed-in user, or `null` for a guest.
 * @property signIn the sign-in page shown to a guest, or `null` to hide the link; it cannot be set together with [user].
 * @property notifications the optional application-owned notifications; they require [user].
 * @since %CURRENT_VERSION%
 */
data class CabinetHeader(
    val items: List<HeaderItem> = emptyList(),
    val active: String? = null,
    val user: HeaderUser? = null,
    val signIn: HeaderDestination.Route? = null,
    val notifications: HeaderNotifications? = null,
)
