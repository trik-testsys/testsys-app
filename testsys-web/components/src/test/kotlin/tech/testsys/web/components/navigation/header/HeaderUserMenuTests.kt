package tech.testsys.web.components.navigation.header

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.NativeButton
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.findAll
import tech.testsys.web.components.testTexts

class HeaderUserMenuTests : MockVaadinTests() {
    private val calls = mutableListOf<String>()
    private val items = listOf(
        HeaderUserMenuItem("Profile", HeaderDestination.Action { calls.add("profile") }),
        HeaderUserMenuItem("Settings", HeaderDestination.Action {}, isEnabled = false),
        HeaderUserMenuItem("Exit", HeaderDestination.Action { calls.add("exit") }, isDestructive = true),
    )

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

        assertEquals("Меню: Anna", header.findAll("ts-header__user").single().element.getAttribute("aria-label"))
    }

    @Test
    fun `should separate destructive actions from ordinary ones`() {
        val header = buildMenuHeader()

        val list = header.findAll("ts-header-user-menu").single().children.toList()
        assertEquals(listOf(false, false, true, false), list.map { entry -> "ts-header-user-separator" in entry.element.classList })
        assertEquals("separator", list[2].element.getAttribute("role"))
    }

    @Test
    fun `should reject a destructive action before an ordinary one`() {
        assertThrows<IllegalArgumentException> { HeaderUserMenu(items.reversed()) }
    }

    private fun buildMenuHeader(): Component {
        val user = HeaderUser(name = "Anna", menu = HeaderUserMenu(items))
        val header = buildHeader(CabinetHeader(user = user), testTexts)
        UI.getCurrent().add(header)
        return header
    }

    private fun menuItems(header: Component): List<NativeButton> = header.findAll("ts-header-user-item").filterIsInstance<NativeButton>()
}
