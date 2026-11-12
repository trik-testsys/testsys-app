@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.navigation.header

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import tech.testsys.infra.localization.UiPage
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
import tech.testsys.web.components.overlay.MenuEntry
import tech.testsys.web.components.overlay.hasSeparatorBefore
import tech.testsys.web.components.overlay.menuEntries
import java.util.Locale

internal fun userMenu(user: HeaderUser, texts: UiPage, interactions: HeaderInteractions, locale: Locale): Div {
    val trigger = NativeButton().apply {
        addClassName(CssClass.HeaderUser)
        element.setAttribute(HtmlAttribute.AriaLabel, texts.headerUserMenu(name = user.name))
        add(
            userAvatar(user.name, locale),
            Span(user.name).apply { addClassName(CssClass.HeaderUserName) },
            svgIcon(IconName.ChevronDown, ICON_SIZE_SMALL),
        )
    }

    val popup = interactions.popup(
        trigger,
        label = texts.headerUserMenu(name = user.name),
        theme = CssTheme.HeaderUserPopup,
        autofocus = true,
    )
    trigger.element.setAriaHasPopup(AriaPopup.Menu)
    popup.setAriaRole(ElementRole.Menu)

    val list = Div().apply { addClassName(CssClass.HeaderUserMenu) }
    val items = menuEntries(checkNotNull(user.menu) { "Header user menu is not configured" })
    items.forEachIndexed { index, item ->
        if (items.hasSeparatorBefore(index)) {
            list.add(
                Div().apply {
                    addClassName(CssClass.HeaderUserSeparator)
                    element.setRole(ElementRole.Separator)
                },
            )
        }

        val destination = when (item) {
            is MenuEntry.Action -> HeaderDestination.Action(item.onSelect)
            is MenuEntry.Link -> HeaderDestination.Route(item.target, item.parameters)
        }
        val entry = if (item.isEnabled) {
            destinationLink(item.label, destination) { popup.close() }
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

internal fun userAvatar(name: String, locale: Locale): Span = Span(avatarInitials(name, locale)).apply {
    addClassNames(CssClass.Avatar, CssClass.AvatarT0, CssClass.HeaderAvatar)
}
