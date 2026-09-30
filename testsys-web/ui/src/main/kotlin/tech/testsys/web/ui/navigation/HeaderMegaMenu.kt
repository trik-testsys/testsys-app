package tech.testsys.web.ui.navigation

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import tech.testsys.web.ui.core.ICON_SIZE_SMALL
import tech.testsys.web.ui.core.IconName
import tech.testsys.web.ui.core.svgIcon

/**
 * Columns and an optional promotion in a header section.
 *
 * @property columns the groups of links, in their displayed order.
 * @property promotion the optional highlighted card.
 * @since %CURRENT_VERSION%
 */
data class HeaderMegaMenu(val columns: List<HeaderMegaColumn>, val promotion: HeaderPromotion? = null)

/**
 * Named group of header links.
 *
 * @property title the heading of the group.
 * @property links the application-provided links.
 * @since %CURRENT_VERSION%
 */
data class HeaderMegaColumn(val title: String, val links: List<HeaderMegaLink>)

/**
 * Link with optional explanatory text.
 *
 * @property label the title of the link.
 * @property destination the route or action to open.
 * @property description the optional explanation.
 * @since %CURRENT_VERSION%
 */
data class HeaderMegaLink(val label: String, val destination: HeaderDestination, val description: String? = null)

/**
 * Optional highlighted card of a mega-menu.
 *
 * @property title the heading of the promotion.
 * @property description the explanatory text.
 * @property actionLabel the label of the call to action.
 * @property destination the destination of the call to action.
 * @property tag the optional short note above the heading.
 * @since %CURRENT_VERSION%
 */
data class HeaderPromotion(
    val title: String,
    val description: String,
    val actionLabel: String,
    val destination: HeaderDestination,
    val tag: String? = null,
)

internal fun megaMenu(item: MegaMenuItem, interactions: HeaderInteractions): Div {
    val trigger = NativeButton(item.label).apply {
        addClassName("ts-nav__item")
        add(svgIcon(IconName.ChevronDown, ICON_SIZE_SMALL))
    }
    val popup = interactions.popup(trigger, label = item.label, theme = "ts-header-mega")
    popup.isOpenOnHover = true
    popup.hoverDelay = 0
    popup.hideDelay = 0
    val grid = Div().apply { addClassNames("ts-mega__grid", "ts-header-mega__grid") }
    item.menu.columns.forEach { column ->
        val group = Div(Div(column.title).apply { addClassName("ts-mega__title") }).apply { addClassName("ts-mega__col") }
        column.links.forEach { link ->
            val target = destinationLink(link.label, link.destination) { popup.close() }
            target.element.classList.add("ts-mega__link")
            target.element.setText("")
            val title = Span(link.label).apply { addClassName("ts-header-link__title") }
            target.element.appendChild(title.element)
            link.description?.let { description -> target.element.appendChild(Span(description).element) }
            group.add(target)
        }
        grid.add(group)
    }
    item.menu.promotion?.let { promotion -> grid.add(promotionCard(promotion) { popup.close() }) }
    popup.add(grid)
    return Div(trigger, popup).apply { addClassName("ts-header-mega-trigger") }
}

private fun promotionCard(promotion: HeaderPromotion, close: () -> Unit): Div = Div().apply {
    addClassName("ts-mega__feat")
    promotion.tag?.let { tag -> add(Span(tag).apply { addClassName("ts-header-promotion__tag") }) }
    add(Div(promotion.title).apply { addClassName("ts-header-promotion__title") })
    add(Div(promotion.description).apply { addClassName("ts-header-promotion__description") })
    add(
        destinationLink(promotion.actionLabel, promotion.destination, close).apply {
            element.classList.addAll(listOf("ts-btn", "ts-btn--sm", "ts-header-promotion__action"))
        },
    )
}
