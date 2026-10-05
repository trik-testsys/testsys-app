@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.navigation

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Nav
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.RouterLink
import tech.testsys.web.components.UiTexts
import tech.testsys.web.components.buildBrand
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.add
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.setAttribute

/** Builds the `.ts-header` markup of [header]. */
internal fun buildHeader(header: CabinetHeader, texts: UiTexts): Div {
    val interactions = HeaderInteractions()
    val menus = header.items.filterIsInstance<MegaMenuItem>().associate { it.key to MegaMenuHandle(it, interactions) }
    require(header.menuSearchKey == null || (header.search == null && header.menuSearchKey in menus)) {
        "Menu search requires an existing mega-menu key and excludes provider search"
    }
    val bar = Div().apply { addClassName(CssClass.HeaderBar) }
    bar.add(buildBrand(texts.brand, href = "."), navigation(header, texts, menus))
    if (header.menuSearchKey != null) {
        bar.add(HeaderMenuSearchController(menus.getValue(header.menuSearchKey), texts.header).component)
    }
    bar.add(Div().apply { addClassName(CssClass.HeaderSpacer) })
    if (header.menuSearchKey == null) {
        header.search?.let { search -> bar.add(HeaderSearchController(search, texts.header, interactions).component) }
    }
    val user = header.user
    val signIn = header.signIn
    if (user != null) {
        header.notifications?.let { notifications ->
            bar.add(HeaderNotificationsController(notifications, texts.header, interactions).component)
        }
        bar.add(if (user.menu == null) userChip(user, texts.locale) else userMenu(user, texts.header, interactions, texts.locale))
    } else if (signIn != null) {
        bar.add(signInLink(texts.signIn, signIn, header.signInParameters))
    }
    return HeaderRoot().apply {
        add(bar)
        header.menuSearchKey?.let { key -> menus.getValue(key).fullAnchor(this) }
    }
}

private fun navigation(header: CabinetHeader, texts: UiTexts, menus: Map<String, MegaMenuHandle>): Nav = Nav().apply {
    addClassName(CssClass.Nav)
    element.setAttribute(HtmlAttribute.AriaLabel, texts.navigation.sections)
    header.items.forEach { item ->
        val entry = when (item) {
            is NavItem -> navLink(item, active = item.key == header.active)
            is MegaMenuItem -> menus.getValue(item.key).component.apply {
                if (item.key == header.active) children.findFirst().orElseThrow().element.classList.add(CssClass.NavItemActive)
            }
        }
        add(entry)
    }
}

private fun navLink(item: NavItem, active: Boolean): RouterLink = RouterLink(item.label, item.target).apply {
    addClassName(CssClass.NavItem)
    if (active) addClassName(CssClass.NavItemActive)
}

private fun userChip(user: HeaderUser, locale: java.util.Locale): Div {
    val avatar = userAvatar(user.name, locale)
    return Div(avatar, Text(user.name)).apply { addClassName(CssClass.HeaderUser) }
}

private fun signInLink(label: String, target: Class<out Component>, parameters: RouteParameters): RouterLink =
    RouterLink(label, target, parameters).apply {
        addClassNames(CssClass.Btn, CssClass.BtnSecondary, CssClass.HeaderAction)
    }
