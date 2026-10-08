package tech.testsys.web.components.navigation.header

import com.vaadin.flow.component.Component
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.web.components.FirstTestView
import tech.testsys.web.components.ItemTestView
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll
import tech.testsys.web.components.testTexts

class HeaderMenuSearchTests : MockVaadinTests() {
    private val destination = HeaderDestination.Route(FirstTestView::class.java)
    private val columns = listOf(
        HeaderMegaColumn(
            title = "Ученик",
            links = listOf(HeaderMegaLink("Решения", destination)),
            destination = destination,
            searchText = "Кабинет ученика",
        ),
        HeaderMegaColumn(
            title = "Организатор",
            links = listOf(HeaderMegaLink("Соревнования", destination), HeaderMegaLink("Участники", destination)),
        ),
    )

    @Test
    fun `should preserve search attributes without native value synchronization`() {
        val search = HeaderMenuSearchController(menu(), testTexts.header)

        val field = search.field.element

        assertEquals("search", field.getAttribute("type"))
        assertEquals(testTexts.header.search, field.getAttribute("placeholder"))
        assertEquals(testTexts.header.search, field.getAttribute("aria-label"))
        assertEquals("off", field.getAttribute("autocomplete"))
        assertNull(field.node.getFeature(ElementListenerMap::class.java).getPropertySynchronizationMode("value"))
    }

    @Test
    fun `should anchor the full menu to the header edge`() {
        val menu = menu()
        val search = HeaderMenuSearchController(menu, testTexts.header)
        val header = Div()
        menu.fullAnchor(header)

        search.inputChanged("")

        assertEquals(header, menu.popup.target)
        assertEquals(PopoverPosition.BOTTOM_START, menu.popup.position)
    }

    @Test
    fun `should anchor compact results to the right edge of the search`() {
        val menu = menu()
        val search = HeaderMenuSearchController(menu, testTexts.header)
        menu.fullAnchor(Div())

        search.inputChanged("Ученик")

        assertEquals(search.component, menu.popup.target)
        assertEquals(PopoverPosition.BOTTOM_END, menu.popup.position)
    }

    @Test
    fun `should return the full menu to the header edge after the query is cleared`() {
        val menu = menu()
        val search = HeaderMenuSearchController(menu, testTexts.header)
        val header = Div()
        menu.fullAnchor(header)
        search.inputChanged("Ученик")

        search.inputChanged("")

        assertEquals(header, menu.popup.target)
    }

    @Test
    fun `should place menu search beside navigation`() {
        val item = MegaMenuItem(key = "menu", label = "Меню", menu = HeaderMegaMenu(columns))

        val header = buildHeader(CabinetHeader(items = listOf(item), menuSearchKey = "menu"), testTexts)

        assertEquals(listOf("ts-nav", "ts-header__search", "ts-header__spacer"), barParts(header))
    }

    @Test
    fun `should group child destinations beneath their linked heading`() {
        val menu = menu()

        val group = menu.component.findAll("ts-mega__col").first()

        assertEquals(1, group.find("ts-mega__children").findAll("ts-mega__link").size)
    }

    @Test
    fun `should mark only the link with exact route parameters`() {
        val menu = routeMenu()

        val links = menu.component.findAll("ts-mega__link")

        assertEquals(listOf("page", null), links.map { link -> link.element.getAttribute("aria-current") })
        assertEquals(listOf(true, false), links.map { link -> link.element.hasAttribute("highlight") })
    }

    @Test
    fun `should keep the current link marked after filtering`() {
        val menu = routeMenu()

        menu.filter("Седьмой")

        assertEquals("page", menu.component.findAll("ts-mega__link").single().element.getAttribute("aria-current"))
    }

    @Test
    fun `should restore the current link after the filter is cleared`() {
        val menu = routeMenu()
        menu.filter("Седьмой")

        menu.filter("")

        val current = menu.component.findAll("ts-mega__link").map { link -> link.element.getAttribute("aria-current") }
        assertEquals(listOf("page", null), current)
    }

    @Test
    fun `should keep a matched heading with all children`() {
        assertEquals(listOf(columns.first()), filteredMegaColumns(columns, " КАБИНЕТ УЧЕНИКА "))
    }

    @Test
    fun `should narrow other groups to links that match literally`() {
        assertEquals(listOf("Соревнования"), filteredMegaColumns(columns, "соревн").single().links.map { link -> link.label })
    }

    @Test
    fun `should keep all groups for a blank query`() {
        assertEquals(columns, filteredMegaColumns(columns, " "))
    }

    @Test
    fun `should drop all groups for an unknown query`() {
        assertTrue(filteredMegaColumns(columns, "missing").isEmpty())
    }

    @Test
    fun `should reuse one menu popup for typing and keep headings as navigation links`() {
        val menu = menu()
        val search = HeaderMenuSearchController(menu, testTexts.header)

        search.inputChanged("Ученик")

        assertTrue(menu.popup.isOpened)
        assertEquals("1", menu.popup.element.style.get("--ts-menu-columns"))
        assertTrue(menu.popup.themeNames.contains("ts-header-menu-search"))
        assertEquals(1, menu.component.findAll("ts-header-mega__grid").size)
        assertEquals(1L, menu.component.children.filter { child -> child is Popover }.count())
        assertEquals("searchbox", search.field.element.getAttribute("role"))
        assertEquals(1, menu.component.findAll("ts-header-mega__heading").size)
    }

    @Test
    fun `should show the empty status if no menu item matches the query`() {
        val menu = menu()
        val search = HeaderMenuSearchController(menu, testTexts.header)

        search.inputChanged("missing")

        val status = menu.component.find("ts-header-search-status").element
        assertEquals(testTexts.header.searchEmpty, status.text)
        assertEquals("status", status.getAttribute("role"))
    }

    @Test
    fun `should show all groups again when the query is cleared`() {
        val menu = menu()
        val search = HeaderMenuSearchController(menu, testTexts.header)
        search.inputChanged("Ученик")

        search.inputChanged("")

        assertEquals(2, menu.component.findAll("ts-mega__col").size)
        assertEquals("2", menu.popup.element.style.get("--ts-menu-columns"))
    }

    @Test
    fun `should close the menu popup with the search`() {
        val menu = menu()
        val search = HeaderMenuSearchController(menu, testTexts.header)
        search.inputChanged("Ученик")

        search.close()

        assertFalse(menu.popup.isOpened)
    }

    @Test
    fun `should reject a missing menu search key`() {
        assertThrows(IllegalArgumentException::class.java) { buildHeader(CabinetHeader(menuSearchKey = "missing"), testTexts) }
    }

    @Test
    fun `should merge overlapping literal highlights`() {
        assertEquals(listOf(HeaderTextPart("b", false), HeaderTextPart("anana", true)), splitHeaderMatches("banana", "ana"))
    }

    @ParameterizedTest
    @CsvSource("a+b,a+b,true", "text,'',false")
    fun `should match regular expression characters literally and keep text whole for an empty query`(
        text: String,
        query: String,
        isMatched: Boolean,
    ) {
        assertEquals(listOf(HeaderTextPart(text, isMatched)), splitHeaderMatches(text, query))
    }

    @Test
    fun `should match a dotted capital I case insensitively`() {
        assertEquals(listOf(HeaderTextPart("İ", true), HeaderTextPart("x", false)), splitHeaderMatches("İx", "i"))
    }

    private fun menu(): MegaMenuHandle =
        MegaMenuHandle(MegaMenuItem(key = "menu", label = "Меню", menu = HeaderMegaMenu(columns)), HeaderInteractions())

    private fun routeMenu(): MegaMenuHandle {
        UI.getCurrent().navigate(ItemTestView::class.java, RouteParameters("id", "7"))
        val routes = listOf(
            HeaderMegaLink("Седьмой", HeaderDestination.Route(ItemTestView::class.java, RouteParameters("id", "7"))),
            HeaderMegaLink("Восьмой", HeaderDestination.Route(ItemTestView::class.java, RouteParameters("id", "8"))),
        )
        val menu = MegaMenuHandle(
            MegaMenuItem(key = "menu", label = "Меню", menu = HeaderMegaMenu(listOf(HeaderMegaColumn(title = "Группа", links = routes)))),
            HeaderInteractions(),
        )
        UI.getCurrent().add(menu.component)
        return menu
    }

    private fun barParts(header: Component): List<String> =
        header.find("ts-header__bar").children.toList().drop(1).map { part -> part.element.classList.toList().single() }
}
