package tech.testsys.web.ui.navigation

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Nav
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.RouterLink
import tech.testsys.web.ui.UiTexts

private val WORD_SEPARATOR = Regex("\\s+")
private const val INITIALS_LENGTH = 2

/** Builds the `.ts-header` markup of [header]. */
internal fun buildHeader(header: CabinetHeader, texts: UiTexts): Div {
    val bar = Div().apply { addClassName("ts-header__bar") }
    bar.add(brand(texts.brand), navigation(header), Div().apply { addClassName("ts-header__spacer") })
    val user = header.user
    val signIn = header.signIn
    if (user != null) {
        bar.add(userChip(user))
    } else if (signIn != null) {
        bar.add(signInLink(texts.signIn, signIn))
    }
    return Div(bar).apply { addClassName("ts-header") }
}

/** Initials of [name]: the first letters of its first two words, upper-cased. */
internal fun initials(name: String): String = name.split(WORD_SEPARATOR)
    .filter { word -> word.isNotBlank() }
    .take(INITIALS_LENGTH)
    .joinToString("") { word -> word.take(1) }
    .uppercase()

private fun brand(name: String): Anchor = Anchor(".", Span(name.take(1)).apply { addClassName("ts-brand__mark") }, Text(name)).apply {
    addClassName("ts-brand")
}

private fun navigation(header: CabinetHeader): Nav = Nav().apply {
    addClassName("ts-nav")
    header.items.forEach { item -> add(navLink(item, active = item.key == header.active)) }
}

private fun navLink(item: NavItem, active: Boolean): RouterLink = RouterLink(item.label, item.target).apply {
    addClassName("ts-nav__item")
    if (active) addClassName("ts-nav__item--active")
}

private fun userChip(user: HeaderUser): Div {
    val avatar = Span(initials(user.name)).apply { addClassNames("ts-avatar", "ts-avatar--t0", "ts-header__avatar") }
    return Div(avatar, Text(user.name)).apply { addClassName("ts-header__user") }
}

private fun signInLink(label: String, target: Class<out Component>): RouterLink = RouterLink(label, target).apply {
    addClassNames("ts-btn", "ts-btn--secondary", "ts-header__action")
}
