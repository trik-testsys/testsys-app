package tech.testsys.web.components.navigation

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Nav
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.RouterLink
import tech.testsys.web.components.UiTexts
import tech.testsys.web.components.buildBrand

private val WORD_SEPARATOR = Regex("\\s+")
private const val INITIALS_LENGTH = 2

/** Builds the `.ts-header` markup of [header]. */
internal fun buildHeader(header: CabinetHeader, texts: UiTexts): Div {
    val interactions = HeaderInteractions()
    val menus = header.items.filterIsInstance<MegaMenuItem>().associate { it.key to MegaMenuHandle(it, interactions) }
    require(header.menuSearchKey == null || (header.search == null && header.menuSearchKey in menus)) {
        "Menu search requires an existing mega-menu key and excludes provider search"
    }
    val bar = Div().apply { addClassName("ts-header__bar") }
    bar.add(buildBrand(texts.brand, href = "."), navigation(header, texts, menus))
    if (header.menuSearchKey != null) {
        bar.add(HeaderMenuSearchController(menus.getValue(header.menuSearchKey), texts.header).component)
    }
    bar.add(Div().apply { addClassName("ts-header__spacer") })
    if (header.menuSearchKey == null) {
        header.search?.let { search -> bar.add(HeaderSearchController(search, texts.header, interactions).component) }
    }
    val user = header.user
    val signIn = header.signIn
    if (user != null) {
        header.notifications?.let { notifications ->
            bar.add(HeaderNotificationsController(notifications, texts.header, interactions).component)
        }
        bar.add(if (user.menu == null) userChip(user) else userMenu(user, texts.header, interactions))
    } else if (signIn != null) {
        bar.add(signInLink(texts.signIn, signIn, header.signInParameters))
    }
    return HeaderRoot().apply {
        add(bar)
        header.menuSearchKey?.let { key -> menus.getValue(key).fullAnchor(this) }
    }
}

/** Initials of [name]: the first letters of its first two words, upper-cased. */
internal fun initials(name: String): String = name.split(WORD_SEPARATOR)
    .filter { word -> word.isNotBlank() }
    .take(INITIALS_LENGTH)
    .joinToString("") { word -> word.take(1) }
    .uppercase()

private fun navigation(header: CabinetHeader, texts: UiTexts, menus: Map<String, MegaMenuHandle>): Nav = Nav().apply {
    addClassName("ts-nav")
    element.setAttribute("aria-label", texts.navigation.sections)
    header.items.forEach { item ->
        val entry = when (item) {
            is NavItem -> navLink(item, active = item.key == header.active)
            is MegaMenuItem -> menus.getValue(item.key).component.apply {
                if (item.key == header.active) children.findFirst().orElseThrow().element.classList.add("ts-nav__item--active")
            }
        }
        add(entry)
    }
}

private fun navLink(item: NavItem, active: Boolean): RouterLink = RouterLink(item.label, item.target).apply {
    addClassName("ts-nav__item")
    if (active) addClassName("ts-nav__item--active")
}

private fun userChip(user: HeaderUser): Div {
    val avatar = userAvatar(user.name)
    return Div(avatar, Text(user.name)).apply { addClassName("ts-header__user") }
}

private fun signInLink(label: String, target: Class<out Component>, parameters: RouteParameters): RouterLink =
    RouterLink(label, target, parameters).apply {
        addClassNames("ts-btn", "ts-btn--secondary", "ts-header__action")
    }
