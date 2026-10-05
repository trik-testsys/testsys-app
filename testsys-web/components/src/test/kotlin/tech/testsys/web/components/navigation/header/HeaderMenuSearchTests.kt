package tech.testsys.web.components.navigation.header

import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.popover.Popover
import com.vaadin.flow.component.popover.PopoverPosition
import com.vaadin.flow.internal.nodefeature.ElementListenerMap
import com.vaadin.flow.router.RouteParameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.components.FirstTestView
import tech.testsys.web.components.ItemTestView
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll
import tech.testsys.web.components.testTexts

class HeaderMenuSearchTests : MockVaadinTests() {
    private val destination = HeaderDestination.Route(FirstTestView::class.java)
    private val columns = listOf(
        HeaderMegaColumn("Ученик", listOf(HeaderMegaLink("Решения", destination)), destination, "Кабинет ученика"),
        HeaderMegaColumn("Организатор", listOf(HeaderMegaLink("Соревнования", destination), HeaderMegaLink("Участники", destination))),
    )

    @Test
    fun `should preserve search attributes without native value synchronization`() {
        val menu = MegaMenuHandle(MegaMenuItem("menu", "Меню", HeaderMegaMenu(columns)), HeaderInteractions())
        val search = HeaderMenuSearchController(menu, testTexts.header)
        val field = search.field.element

        assertEquals("search", field.getAttribute("type"))
        assertEquals(testTexts.header.search, field.getAttribute("placeholder"))
        assertEquals(testTexts.header.search, field.getAttribute("aria-label"))
        assertEquals("off", field.getAttribute("autocomplete"))
        assertNull(field.node.getFeature(ElementListenerMap::class.java).getPropertySynchronizationMode("value"))
    }

    @Test
    fun `should anchor full menu to the header edge and compact results to the search right edge`() {
        val menu = MegaMenuHandle(MegaMenuItem("menu", "Меню", HeaderMegaMenu(columns)), HeaderInteractions())
        val search = HeaderMenuSearchController(menu, testTexts.header)
        val header = Div()
        menu.fullAnchor(header)
        search.inputChanged("")
        assertEquals(header, menu.popup.target)
        assertEquals(PopoverPosition.BOTTOM_START, menu.popup.position)
        search.inputChanged("Ученик")
        assertEquals(search.component, menu.popup.target)
        assertEquals(PopoverPosition.BOTTOM_END, menu.popup.position)
        search.inputChanged("")
        assertEquals(header, menu.popup.target)
    }

    @Test
    fun `should place menu search beside navigation and retain provider search after spacer`() {
        val item = MegaMenuItem("menu", "Меню", HeaderMegaMenu(columns))
        val menuHeader = buildHeader(CabinetHeader(items = listOf(item), menuSearchKey = "menu"), testTexts)
        val menuParts = menuHeader.find("ts-header__bar").children.toList()
        assertEquals(listOf("ts-nav", "ts-header__search", "ts-header__spacer"), menuParts.drop(1).map { it.element.classList.toList().single() })
        val provider = buildHeader(CabinetHeader(search = HeaderSearch { emptyList() }), testTexts)
        val providerParts = provider.find("ts-header__bar").children.toList()
        assertEquals(listOf("ts-nav", "ts-header__spacer", "ts-header__search"), providerParts.drop(1).map { it.element.classList.toList().single() })
    }

    @Test
    fun `should group child destinations beneath their linked heading`() {
        val menu = MegaMenuHandle(MegaMenuItem("menu", "Меню", HeaderMegaMenu(columns)), HeaderInteractions())
        val group = menu.component.findAll("ts-mega__col").first()
        val children = group.find("ts-mega__children")
        assertEquals(1, children.findAll("ts-mega__link").size)
    }

    @Test
    fun `should mark only exact route parameters and restore current link after filtering`() {
        UI.getCurrent().navigate(ItemTestView::class.java, RouteParameters("id", "7"))
        val routes = listOf(
            HeaderMegaLink("Седьмой", HeaderDestination.Route(ItemTestView::class.java, RouteParameters("id", "7"))),
            HeaderMegaLink("Восьмой", HeaderDestination.Route(ItemTestView::class.java, RouteParameters("id", "8"))),
        )
        val menu = MegaMenuHandle(
            MegaMenuItem("menu", "Меню", HeaderMegaMenu(listOf(HeaderMegaColumn("Группа", routes)))),
            HeaderInteractions(),
        )
        UI.getCurrent().add(menu.component)
        val initial = menu.component.findAll("ts-mega__link")
        assertEquals(listOf("page", null), initial.map { it.element.getAttribute("aria-current") })
        assertEquals(listOf(true, false), initial.map { it.element.hasAttribute("highlight") })
        menu.filter("Седьмой")
        assertEquals("page", menu.component.findAll("ts-mega__link").single().element.getAttribute("aria-current"))
        menu.filter("")
        assertEquals(listOf("page", null), menu.component.findAll("ts-mega__link").map { it.element.getAttribute("aria-current") })
    }

    @Test
    fun `should keep a matched heading with all children and filter other groups literally`() {
        assertEquals(listOf(columns.first()), filteredMegaColumns(columns, " КАБИНЕТ УЧЕНИКА "))
        assertEquals(listOf("Соревнования"), filteredMegaColumns(columns, "соревн").single().links.map { it.label })
        assertEquals(columns, filteredMegaColumns(columns, " "))
        assertTrue(filteredMegaColumns(columns, "missing").isEmpty())
    }

    @Test
    fun `should reuse one menu popup for typing and keep headings as navigation links`() {
        val section = MegaMenuItem("menu", "Меню", HeaderMegaMenu(columns))
        val menu = MegaMenuHandle(section, HeaderInteractions())
        val search = HeaderMenuSearchController(menu, testTexts.header)

        search.inputChanged("Ученик")

        assertTrue(menu.popup.isOpened)
        assertEquals("1", menu.popup.element.style.get("--ts-menu-columns"))
        assertTrue(menu.popup.themeNames.contains("ts-header-menu-search"))
        assertEquals(1, menu.component.findAll("ts-header-mega__grid").size)
        assertEquals(1L, menu.component.children.filter { it is Popover }.count())
        assertEquals("searchbox", search.field.element.getAttribute("role"))
        assertEquals(1, menu.component.findAll("ts-header-mega__heading").size)
        search.inputChanged("")
        assertEquals(2, menu.component.findAll("ts-mega__col").size)
        assertEquals("2", menu.popup.element.style.get("--ts-menu-columns"))
        search.close()
        assertFalse(menu.popup.isOpened)
    }

    @Test
    fun `should reject missing menu keys and simultaneous provider and menu search`() {
        assertThrows(IllegalArgumentException::class.java) {
            buildHeader(CabinetHeader(menuSearchKey = "missing"), testTexts)
        }
        assertThrows(IllegalArgumentException::class.java) {
            buildHeader(CabinetHeader(search = HeaderSearch { emptyList() }, menuSearchKey = "menu"), testTexts)
        }
    }

    @Test
    fun `should merge overlapping literal highlights without interpreting regular expressions`() {
        assertEquals(listOf(HeaderTextPart("b", false), HeaderTextPart("anana", true)), splitHeaderMatches("banana", "ana"))
        assertEquals(listOf(HeaderTextPart("a+b", true)), splitHeaderMatches("a+b", "a+b"))
        assertEquals(listOf(HeaderTextPart("İ", true), HeaderTextPart("x", false)), splitHeaderMatches("İx", "i"))
        assertEquals(listOf(HeaderTextPart("text", false)), splitHeaderMatches("text", ""))
    }
}
