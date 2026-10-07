@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.navigation.header

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.core.AriaPopup
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.CssTheme
import tech.testsys.web.components.core.ElementRole
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.ICON_SIZE_SMALL
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.add
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.set
import tech.testsys.web.components.core.setAriaDisabled
import tech.testsys.web.components.core.setAriaHasPopup
import tech.testsys.web.components.core.setAriaRole
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setRole
import tech.testsys.web.components.core.svgIcon
import tech.testsys.web.components.display.avatarInitials
import tech.testsys.web.components.texts.HeaderTexts

/**
 * User menu supplied by the application; destructive items follow ordinary items.
 *
 * @property items the links and actions in display order.
 * @since %CURRENT_VERSION%
 */
data class HeaderUserMenu(val items: List<HeaderUserMenuItem>) {
    init {
        require(items.isNotEmpty()) { "Header user menu must have at least one item" }
        val misplaced = items.dropWhile { item -> !item.isDestructive }.firstOrNull { item -> !item.isDestructive }
        require(misplaced == null) {
            "Header user menu item '${misplaced?.label}' follows a destructive item; destructive items come last"
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

internal fun userMenu(user: HeaderUser, texts: HeaderTexts, interactions: HeaderInteractions, locale: java.util.Locale): Div {
    val trigger = NativeButton().apply {
        addClassName(CssClass.HeaderUser)
        element.setAttribute(HtmlAttribute.AriaLabel, texts.userMenu(user.name))
        add(
            userAvatar(user.name, locale),
            Span(user.name).apply { addClassName(CssClass.HeaderUserName) },
            svgIcon(IconName.ChevronDown, ICON_SIZE_SMALL),
        )
    }
    val popup = interactions.popup(trigger, label = texts.userMenu(user.name), theme = CssTheme.HeaderUserPopup, autofocus = true)
    trigger.element.setAriaHasPopup(AriaPopup.Menu)
    popup.setAriaRole(ElementRole.Menu)
    val list = Div().apply { addClassName(CssClass.HeaderUserMenu) }
    val items = checkNotNull(user.menu) { "Header user menu is not configured" }.items
    items.forEachIndexed { index, item ->
        if (item.isDestructive && (index == 0 || !items[index - 1].isDestructive)) {
            list.add(
                Div().apply {
                    addClassName(CssClass.HeaderUserSeparator)
                    element.setRole(ElementRole.Separator)
                },
            )
        }
        val entry = if (item.isEnabled) {
            destinationLink(item.label, item.destination) { popup.close() }
        } else {
            NativeButton(item.label).apply { isEnabled = false }
        }
        entry.element.classList.add(CssClass.HeaderUserItem)
        entry.element.classList.set(CssClass.MenuItemDanger, item.isDestructive)
        entry.element.setRole(ElementRole.MenuItem)
        entry.element.setAriaDisabled(!item.isEnabled)
        list.add(entry)
    }
    popup.add(list)
    return Div(trigger, popup)
}

internal fun userAvatar(name: String, locale: java.util.Locale): Span = Span(avatarInitials(name, locale)).apply {
    addClassNames(CssClass.Avatar, CssClass.AvatarT0, CssClass.HeaderAvatar)
}
