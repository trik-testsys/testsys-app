@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.navigation.header

import com.vaadin.flow.component.Text
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Nav
import com.vaadin.flow.router.RouterLink
import tech.testsys.web.components.buildBrand
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.add
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.texts.UiLocalization
import java.util.Locale

/** Builds the `.ts-header` markup of [header]. */
internal fun buildHeader(header: CabinetHeader, texts: UiLocalization): Div {
    checkConfiguration(header)

    val interactions = HeaderInteractions()
    val menus = header.items.filterIsInstance<MegaMenuItem>().associate { it.key to MegaMenuHandle(it, interactions) }
    val searchable = header.items.filterIsInstance<MegaMenuItem>().singleOrNull { item -> item.isSearchable }
        ?.let { item -> menus.getValue(item.key) }

    val bar = Div().apply { addClassName(CssClass.HeaderBar) }
    bar.add(buildBrand(texts.messages.uiPage.brandName(), href = "."), navigation(header, texts, menus))

    // The search looks through the section items, so the field follows the navigation.
    if (searchable != null) bar.add(HeaderMenuSearchController(searchable, texts.messages.uiPage).component)

    bar.add(Div().apply { addClassName(CssClass.HeaderSpacer) })

    val user = header.user
    val signIn = header.signIn
    if (user != null) {
        header.notifications?.let { notifications ->
            bar.add(HeaderNotificationsController(notifications, texts.messages.uiPage, interactions).component)
        }
        val userControl = if (user.menu == null) {
            userChip(user, texts.locale)
        } else {
            userMenu(user, texts.messages.uiPage, interactions, texts.locale)
        }
        bar.add(userControl)
    } else if (signIn != null) {
        bar.add(signInLink(texts.messages.uiPage.headerSignIn(), signIn))
    }

    return HeaderRoot().apply {
        add(bar)
        searchable?.fullAnchor(this)
    }
}

private fun checkConfiguration(header: CabinetHeader) {
    val keys = header.items.map { item -> item.key }
    val duplicates = keys.groupingBy { key -> key }.eachCount().filterValues { count -> count > 1 }.keys
    require(duplicates.isEmpty()) { "Header section keys must be unique, duplicated: $duplicates" }
    require(header.active == null || header.active in keys) { "Active header section '${header.active}' must be one of $keys" }
    require(header.user == null || header.signIn == null) { "Header cannot show both the user '${header.user?.name}' and the sign-in link" }
    require(header.notifications == null || header.user != null) { "Header notifications require a signed-in user" }

    val searchable = header.items.filter { item -> item is MegaMenuItem && item.isSearchable }.map { item -> item.key }
    require(searchable.size <= 1) { "At most one header mega-menu can be searchable, got $searchable" }
}

private fun navigation(header: CabinetHeader, texts: UiLocalization, menus: Map<String, MegaMenuHandle>): Nav = Nav().apply {
    addClassName(CssClass.Nav)
    element.setAttribute(HtmlAttribute.AriaLabel, texts.messages.uiPage.navigationSections())

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

private fun navLink(item: NavItem, active: Boolean): RouterLink =
    RouterLink(item.label, item.destination.target, item.destination.parameters).apply {
        addClassName(CssClass.NavItem)
        if (active) addClassName(CssClass.NavItemActive)
    }

private fun userChip(user: HeaderUser, locale: Locale): Div {
    val avatar = userAvatar(user.name, locale)
    return Div(avatar, Text(user.name)).apply { addClassName(CssClass.HeaderUser) }
}

private fun signInLink(label: String, signIn: HeaderDestination.Route): RouterLink =
    RouterLink(label, signIn.target, signIn.parameters).apply {
        addClassNames(CssClass.Btn, CssClass.BtnSecondary, CssClass.HeaderAction)
    }
