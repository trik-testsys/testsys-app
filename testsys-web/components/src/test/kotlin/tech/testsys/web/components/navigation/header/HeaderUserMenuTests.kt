package tech.testsys.web.components.navigation.header

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.RouterLink
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.FirstTestView
import tech.testsys.web.components.ItemTestView
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.findAll
import tech.testsys.web.components.overlay.MenuScope
import tech.testsys.web.components.testTexts

class HeaderUserMenuTests : MockVaadinTests() {
    private val calls = mutableListOf<String>()

    @Test
    fun `should run a configured user action`() {
        val header = buildMenuHeader()

        menuItems(header).first()._click()

        assertEquals(listOf("profile"), calls)
    }

    @Test
    fun `should keep a disabled user action unavailable`() {
        val header = buildMenuHeader()

        assertFalse(menuItems(header)[1].isEnabled)
    }

    @Test
    fun `should name the user menu by the user`() {
        val header = buildMenuHeader()

        assertEquals("Меню пользователя: Anna", header.findAll("ts-header__user").single().element.getAttribute("aria-label"))
    }

    @Test
    fun `should separate destructive actions from ordinary ones`() {
        val header = buildMenuHeader()

        val list = header.findAll("ts-header-user-menu").single().children.toList()
        assertEquals(listOf(false, false, true, false), list.map { entry -> "ts-header-user-separator" in entry.element.classList })
        assertEquals("separator", list[2].element.getAttribute("role"))
    }

    @Test
    fun `should render a user menu link as a router link`() {
        val header = buildMenuHeader { link("Profile", FirstTestView::class.java) }

        val link = header.findAll("ts-header-user-item").single()
        assertEquals("test/first", assertInstanceOf(RouterLink::class.java, link).href)
        assertEquals("menuitem", link.element.getAttribute("role"))
    }

    @Test
    fun `should render a user menu link with its route parameters`() {
        val header = buildMenuHeader { link("Profile", ItemTestView::class.java, RouteParameters("id", "7")) }

        val link = header.findAll("ts-header-user-item").single()
        assertEquals("test/item/7", assertInstanceOf(RouterLink::class.java, link).href)
    }

    @Test
    fun `should not add a separator to a user menu of destructive actions only`() {
        val header = buildMenuHeader { destructiveItem("Exit") {} }

        val list = header.findAll("ts-header-user-menu").single().children.toList()
        assertEquals(listOf(false), list.map { entry -> "ts-header-user-separator" in entry.element.classList })
    }

    @Test
    fun `should reject an ordinary action after a destructive one`() {
        assertThrows<IllegalStateException> {
            buildMenuHeader {
                destructiveItem("Exit") {}
                item("Profile") {}
            }
        }
    }

    @Test
    fun `should reject a user menu without items`() {
        assertThrows<IllegalStateException> { buildMenuHeader {} }
    }

    private fun buildMenuHeader(
        menu: MenuScope.() -> Unit = {
            item("Profile") { calls.add("profile") }
            item("Settings", isEnabled = false) {}
            destructiveItem("Exit") { calls.add("exit") }
        },
    ): Component {
        val header = buildHeader(CabinetHeader(user = HeaderUser(name = "Anna", menu = menu)), testTexts)
        UI.getCurrent().add(header)
        return header
    }

    private fun menuItems(header: Component): List<NativeButton> = header.findAll("ts-header-user-item").filterIsInstance<NativeButton>()
}
