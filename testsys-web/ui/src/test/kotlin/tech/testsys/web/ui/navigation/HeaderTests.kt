package tech.testsys.web.ui.navigation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.ui.FirstTestView
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
