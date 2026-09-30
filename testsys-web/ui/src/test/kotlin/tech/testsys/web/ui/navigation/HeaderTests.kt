package tech.testsys.web.ui.navigation

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.popover.Popover
import com.vaadin.flow.router.RouteParameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.ui.FirstTestView
import tech.testsys.web.ui.ItemTestView
import tech.testsys.web.ui.MockVaadinTests
import tech.testsys.web.ui.SecondTestView
import tech.testsys.web.ui.classes
import tech.testsys.web.ui.find
import tech.testsys.web.ui.findAll
import tech.testsys.web.ui.testTexts

class HeaderTests : MockVaadinTests() {
    private val items = listOf(
        NavItem(key = "first", label = "Первый", target = FirstTestView::class.java),
        NavItem(key = "second", label = "Второй", target = SecondTestView::class.java),
    )

    @Test
    fun `should render mega menu destinations with route parameters and working actions`() {
        val calls = mutableListOf<String>()
        val section = MegaMenuItem(
            key = "menu",
            label = "Menu",
            menu = HeaderMegaMenu(
                columns = listOf(
                    HeaderMegaColumn(
                        title = "Group",
                        links = listOf(
                            HeaderMegaLink("Route", HeaderDestination.Route(ItemTestView::class.java, RouteParameters("id", "7"))),
                            HeaderMegaLink("Action", HeaderDestination.Action { calls.add("action") }),
                        ),
                    ),
                ),
            ),
        )
        val header = buildHeader(CabinetHeader(items = listOf(section)), testTexts)
        UI.getCurrent().add(header)
        val targets = header.findAll("ts-mega__link")

        assertInstanceOf(NativeButton::class.java, targets[1])._click()

        assertEquals("test/item/7", targets[0].element.getAttribute("href"))
        assertEquals(listOf("action"), calls)
    }

    @Test
    fun `should keep only the latest header popup open`() {
        val header = buildHeader(
            CabinetHeader(
                items = listOf(MegaMenuItem(key = "menu", label = "Menu", menu = HeaderMegaMenu(emptyList()))),
                search = HeaderSearch { emptyList() },
            ),
            testTexts,
        )
        UI.getCurrent().add(header)
        val mega = header.find("ts-header-mega-trigger").children.toList().filterIsInstance<Popover>().single()
        val search = header.find("ts-header__search").children.toList().filterIsInstance<Popover>().single()
        mega.open()

        search.open()

        assertFalse(mega.isOpened)
        assertTrue(search.isOpened)
        assertTrue(mega.isOpenOnHover)
        assertTrue(mega.isOpenOnClick)
    }

    @Test
    fun `should render brand with its first letter as the mark`() {
        val header = buildHeader(CabinetHeader(), testTexts)

        assertEquals("TTestSys", header.find("ts-brand").element.textRecursively)
        assertEquals("T", header.find("ts-brand__mark").element.textRecursively)
    }

    @Test
    fun `should link sections to their routes and mark the active one`() {
        val header = buildHeader(CabinetHeader(items = items, active = "second"), testTexts)

        val links = header.findAll("ts-nav__item")
        assertEquals(listOf("test/first", "test/second"), links.map { link -> link.element.getAttribute("href") })
        assertFalse("ts-nav__item--active" in links[0].classes())
        assertTrue("ts-nav__item--active" in links[1].classes())
    }

    @Test
    fun `should show user initials instead of the sign-in link`() {
        val header = buildHeader(
            CabinetHeader(user = HeaderUser("Анна Петрова"), signIn = FirstTestView::class.java),
            testTexts,
        )

        assertEquals("АП", header.find("ts-avatar").element.textRecursively)
        assertTrue(header.findAll("ts-header__action").isEmpty())
    }

    @Test
    fun `should show the sign-in link to a guest`() {
        val header = buildHeader(CabinetHeader(signIn = FirstTestView::class.java), testTexts)

        val signIn = header.find("ts-header__action")
        assertEquals("Войти", signIn.element.textRecursively)
        assertEquals("test/first", signIn.element.getAttribute("href"))
    }

    @Test
    fun `should hide the sign-in link if no sign-in route is set`() {
        val header = buildHeader(CabinetHeader(), testTexts)

        assertTrue(header.findAll("ts-header__action").isEmpty())
    }
}
