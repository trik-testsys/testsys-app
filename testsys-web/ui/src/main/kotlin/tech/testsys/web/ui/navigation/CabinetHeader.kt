package tech.testsys.web.ui.navigation

import com.vaadin.flow.component.Component

/**
 * Section link of the Cabinet header.
 *
 * @property key the identifier compared with [CabinetHeader.active].
 * @property label the shown name of the section.
 * @property target the route the link opens.
 * @since %CURRENT_VERSION%
 */
data class NavItem(val key: String, val label: String, val target: Class<out Component>)

/**
 * Signed-in user shown in the Cabinet header.
 *
 * @property name the full name of the user.
 * @since %CURRENT_VERSION%
 */
data class HeaderUser(val name: String)

/**
 * Top bar of a Cabinet page: the brand, section links and the user or the sign-in link.
 *
 * @property items the section links.
 * @property active the key of the current section, or `null` if no section is current.
 * @property user the signed-in user, or `null` for a guest.
 * @property signIn the route of the sign-in page shown to a guest, or `null` to hide the link.
 * @since %CURRENT_VERSION%
 */
data class CabinetHeader(
    val items: List<NavItem> = emptyList(),
    val active: String? = null,
    val user: HeaderUser? = null,
    val signIn: Class<out Component>? = null,
)
