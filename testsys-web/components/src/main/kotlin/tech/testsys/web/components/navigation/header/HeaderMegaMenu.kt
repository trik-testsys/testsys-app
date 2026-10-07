@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.navigation.header

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.popover.Popover
import com.vaadin.flow.component.popover.PopoverPosition
import com.vaadin.flow.router.RouterLink
import tech.testsys.web.components.core.AriaCurrent
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.CssTheme
import tech.testsys.web.components.core.DomEvent
import tech.testsys.web.components.core.ElementRole
import tech.testsys.web.components.core.HtmlTag
import tech.testsys.web.components.core.ICON_SIZE_SMALL
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.add
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.addEventListener
import tech.testsys.web.components.core.addThemeName
import tech.testsys.web.components.core.htmlElement
import tech.testsys.web.components.core.setAriaCurrent
import tech.testsys.web.components.core.setHighlight
import tech.testsys.web.components.core.setMenuColumns
import tech.testsys.web.components.core.setRole
import tech.testsys.web.components.core.svgIcon

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
 * @property destination the optional destination of the heading.
 * @property searchText additional searchable text of the group, without changing its caption.
 * @since %CURRENT_VERSION%
 */
data class HeaderMegaColumn(
    val title: String,
    val links: List<HeaderMegaLink>,
    val destination: HeaderDestination? = null,
    val searchText: String? = null,
)

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

private const val MAX_MENU_COLUMNS = 4

/** One menu surface shared by its navigation trigger and optional search field. */
internal class MegaMenuHandle(private val item: MegaMenuItem, interactions: HeaderInteractions) {
    val trigger = NativeButton(item.label).apply {
        addClassName(CssClass.NavItem)
        add(svgIcon(IconName.ChevronDown, ICON_SIZE_SMALL))
    }
    val popup: Popover = interactions.popup(trigger, label = item.label, theme = CssTheme.HeaderMega).apply {
        isOpenOnHover = true
        hoverDelay = 0
        hideDelay = 0
    }
    private var fullTarget: Component = trigger
    private var searchTarget: Component? = null
    private var query: String = ""
    private var groupCount: Int = item.menu.columns.size
    private val grid = Div().apply {
        addClassNames(CssClass.MegaGrid, CssClass.HeaderMegaGrid)
    }
    val component: Div = Div(trigger, popup).apply { addClassName(CssClass.HeaderMegaTrigger) }

    init {
        popup.add(grid)
        filter("")
    }

    fun fullAnchor(header: Component) {
        fullTarget = header
        position()
    }

    fun searchMode(search: Component) {
        searchTarget = search
        popup.addThemeName(CssTheme.HeaderMenuSearch)
        popup.isOpenOnClick = false
        popup.isOpenOnHover = false
        trigger.addClickListener { popup.isOpened = !popup.isOpened }
        trigger.element.addEventListener(DomEvent.MouseEnter) { popup.open() }
        grid.addClassName(CssClass.HeaderMegaGridSearch)
        position()
    }

    fun filter(query: String, emptyText: String? = null) {
        grid.removeAll()
        this.query = query
        val columns = filteredMegaColumns(item.menu.columns, query)
        groupCount = columns.size
        position()
        val visibleColumns = columns.size.coerceIn(minimumValue = 1, maximumValue = MAX_MENU_COLUMNS)
        popup.element.style.setMenuColumns(visibleColumns)
        grid.element.style.setMenuColumns(visibleColumns)
        if (columns.isEmpty() && emptyText != null) {
            grid.add(Span(emptyText).apply { element.setRole(ElementRole.Status) })
        }
        columns.forEach { column ->
            val group = Div().apply { addClassName(CssClass.MegaCol) }
            val heading = column.destination?.let { destination ->
                menuDestination(destinationLink(column.title, destination) { popup.close() }).apply {
                    element.classList.add(CssClass.HeaderMegaHeading)
                    highlight(this, column.title, query)
                }
            } ?: Div(column.title).apply { addClassName(CssClass.MegaTitle) }
            group.add(heading)
            val children = if (column.destination != null) {
                group.addClassName(CssClass.MegaColLinked)
                Div().apply { addClassName(CssClass.MegaChildren) }.also { nested -> group.add(nested) }
            } else {
                group
            }
            column.links.forEach { link ->
                val target = menuDestination(destinationLink(link.label, link.destination) { popup.close() })
                target.element.classList.add(CssClass.MegaLink)
                target.element.setText("")
                val title = Span().apply {
                    addClassName(CssClass.HeaderLinkTitle)
                    highlight(this, link.label, query)
                }
                target.element.appendChild(title.element)
                link.description?.let { description -> target.element.appendChild(Span(description).element) }
                children.add(target)
            }
            grid.add(group)
        }
        item.menu.promotion?.let { promotion -> grid.add(promotionCard(promotion) { popup.close() }) }
    }

    private fun position() {
        if (searchTarget == null) return
        val isCompact = query.isNotBlank() && groupCount < MAX_MENU_COLUMNS
        popup.target = if (isCompact) searchTarget else fullTarget
        popup.position = if (isCompact) PopoverPosition.BOTTOM_END else PopoverPosition.BOTTOM_START
    }
}

/** Preserves a matched group; otherwise narrows its existing children without changing targets. */
internal fun filteredMegaColumns(columns: List<HeaderMegaColumn>, query: String): List<HeaderMegaColumn> {
    if (query.isBlank()) return columns
    return columns.mapNotNull { column ->
        if (splitHeaderMatches("${column.title} ${column.searchText.orEmpty()}", query).any { part -> part.isMatched }) {
            column
        } else {
            val links = column.links.filter { link -> splitHeaderMatches(link.label, query).any { part -> part.isMatched } }
            column.copy(links = links).takeIf { links.isNotEmpty() }
        }
    }
}

/** Exact destinations retain their current marker when menu filtering creates fresh links. */
private fun menuDestination(target: Component): Component = target.apply {
    if (this is RouterLink) {
        configureExactRoute(this, ::markMenuCurrent)
    }
}

private fun markMenuCurrent(link: RouterLink, isCurrent: Boolean) {
    link.element.setHighlight(isCurrent)
    if (isCurrent) link.element.setAriaCurrent(AriaCurrent.Page) else link.element.setAriaCurrent(null)
}

private fun highlight(target: Component, value: String, query: String) {
    target.element.removeAllChildren()
    target.element.setText("")
    splitHeaderMatches(value, query).forEach { part ->
        val text = if (part.isMatched) htmlElement(HtmlTag.Mark).apply { this.text = part.text } else Text(part.text).element
        target.element.appendChild(text)
    }
}

private fun promotionCard(promotion: HeaderPromotion, close: () -> Unit): Div = Div().apply {
    addClassName(CssClass.MegaFeat)
    promotion.tag?.let { tag -> add(Span(tag).apply { addClassName(CssClass.HeaderPromotionTag) }) }
    add(Div(promotion.title).apply { addClassName(CssClass.HeaderPromotionTitle) })
    add(Div(promotion.description).apply { addClassName(CssClass.HeaderPromotionDescription) })
    add(
        destinationLink(promotion.actionLabel, promotion.destination, close).apply {
            element.classList.add(CssClass.Btn)
            element.classList.add(CssClass.BtnSm)
            element.classList.add(CssClass.HeaderPromotionAction)
        },
    )
}
