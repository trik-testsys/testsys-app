package tech.testsys.web.components.navigation.header

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.popover.Popover
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.components.FirstTestView
import tech.testsys.web.components.ItemTestView
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.SecondTestView
import tech.testsys.web.components.TestSysBrand
import tech.testsys.web.components.classes
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll
import tech.testsys.web.components.testTexts

class HeaderTests : MockVaadinTests() {
    private val items = listOf(
        NavItem(key = "first", label = "Первый", target = FirstTestView::class.java),
        NavItem(key = "second", label = "Второй", target = SecondTestView::class.java),
    )

    @Test
    fun `should keep the shared header sticky`() {
        val header = buildHeader(CabinetHeader(user = HeaderUser("istanbul izmir")), testTexts)

        assertTrue("ts-header--sticky" in header.classes())
    }

    @Test
    fun `should derive avatar initials using the locale of the texts`() {
        val texts = tech.testsys.web.components.texts.UiTexts(
            locale = java.util.Locale.forLanguageTag("tr"),
            brand = testTexts.brand,
            signIn = testTexts.signIn,
            calendar = testTexts.calendar,
            fieldErrors = testTexts.fieldErrors,
            dateRangeReversed = testTexts.dateRangeReversed,
            editing = testTexts.editing,
            table = testTexts.table,
            pagination = testTexts.pagination,
            load = testTexts.load,
            dialog = testTexts.dialog,
            lookup = testTexts.lookup,
            navigation = testTexts.navigation,
            menu = testTexts.menu,
            dateFields = testTexts.dateFields,
            notFound = testTexts.notFound,
            components = testTexts.components,
            header = testTexts.header,
            footer = testTexts.footer,
            tableFilters = testTexts.tableFilters,
        )

        val header = buildHeader(CabinetHeader(user = HeaderUser("istanbul izmir")), texts)

        assertEquals("İİ", header.find("ts-header__avatar").element.text)
    }

    @Test
    fun `should route guest sign in using explicit parameters`() {
        val header = buildHeader(
            CabinetHeader(signIn = ItemTestView::class.java, signInParameters = RouteParameters("id", "7")),
            testTexts,
        )
        assertTrue(header.findAll("ts-header__action").single().element.getAttribute("href").endsWith("/7"))
    }

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
                user = HeaderUser("Анна Петрова"),
                notifications = HeaderNotifications(ValueSignal(emptyList()), onRead = {}, onReadAll = {}),
            ),
            testTexts,
        )
        UI.getCurrent().add(header)
        val mega = header.find("ts-header-mega-trigger").children.toList().filterIsInstance<Popover>().single()
        val bell = header.find("ts-bell").parent.orElseThrow().children.toList().filterIsInstance<Popover>().single()
        mega.open()

        bell.open()

        assertFalse(mega.isOpened)
        assertTrue(bell.isOpened)
        assertTrue(mega.isOpenOnHover)
        assertTrue(mega.isOpenOnClick)
    }

    @Test
    fun `should render the decorative horizontal logo with the localized accessible name`() {
        val header = buildHeader(CabinetHeader(), testTexts)

        assertEquals(testTexts.brand, header.find("ts-brand").element.getAttribute("aria-label"))
        assertEquals(".", header.find("ts-brand").element.getAttribute("href"))
        assertEquals(1L, header.find("ts-brand").children.count())
        assertEquals(TestSysBrand.HEADER, header.find("ts-brand__logo").element.getAttribute("src"))
        assertEquals("", header.find("ts-brand__logo").element.getAttribute("alt"))
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
