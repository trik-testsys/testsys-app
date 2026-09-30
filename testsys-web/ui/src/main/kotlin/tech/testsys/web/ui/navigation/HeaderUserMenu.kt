package tech.testsys.web.ui.navigation

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import tech.testsys.web.ui.HeaderTexts
import tech.testsys.web.ui.core.ICON_SIZE_SMALL
import tech.testsys.web.ui.core.IconName
import tech.testsys.web.ui.core.svgIcon

/**
 * User menu supplied by the application; destructive items follow ordinary items.
 *
 * @property items the links and actions in display order.
 * @since %CURRENT_VERSION%
 */
data class HeaderUserMenu(val items: List<HeaderUserMenuItem>) {
    init {
        require(items.isNotEmpty()) { "Header user menu must have at least one item" }
        require(items.dropWhile { item -> !item.isDestructive }.all { item -> item.isDestructive }) {
            "Header user menu destructive items must come last"
        }
    }
}

/**
 * Configurable link or action of the signed-in user.
 *
 * @property label the text of the item.
 * @property destination the route or handler.
 * @property isEnabled whether the item can be selected.
 * @property isDestructive whether the action is visually separated and marked as destructive.
 * @since %CURRENT_VERSION%
 */
data class HeaderUserMenuItem(
    val label: String,
    val destination: HeaderDestination,
    val isEnabled: Boolean = true,
    val isDestructive: Boolean = false,
)

internal fun userMenu(user: HeaderUser, texts: HeaderTexts, interactions: HeaderInteractions): Div {
    val trigger = NativeButton().apply {
        addClassName("ts-header__user")
        element.setAttribute("aria-label", texts.userMenu(user.name))
        add(
            userAvatar(user.name),
            Span(user.name).apply { addClassName("ts-header-user-name") },
            svgIcon(IconName.ChevronDown, ICON_SIZE_SMALL),
        )
    }
    val popup = interactions.popup(trigger, label = texts.userMenu(user.name), theme = "ts-header-user-popup", autofocus = true)
    trigger.element.setAttribute("aria-haspopup", "menu")
    popup.setAriaRole("menu")
    val list = Div().apply { addClassName("ts-header-user-menu") }
    val items = checkNotNull(user.menu) { "Header user menu is not configured" }.items
    items.forEachIndexed { index, item ->
        if (item.isDestructive && (index == 0 || !items[index - 1].isDestructive)) {
            list.add(
                Div().apply {
                    addClassName("ts-header-user-separator")
                    element.setAttribute("role", "separator")
                },
            )
        }
        val entry = if (item.isEnabled) {
            destinationLink(item.label, item.destination) { popup.close() }
        } else {
            NativeButton(item.label).apply { isEnabled = false }
        }
        entry.element.classList.add("ts-header-user-item")
        entry.element.classList.set("ts-menu__item--danger", item.isDestructive)
        entry.element.setAttribute("role", "menuitem")
        entry.element.setAttribute("aria-disabled", (!item.isEnabled).toString())
        list.add(entry)
    }
    popup.add(list)
    return Div(trigger, popup)
}

internal fun userAvatar(name: String): Span = Span(initials(name)).apply {
    addClassNames("ts-avatar", "ts-avatar--t0", "ts-header__avatar")
}
