package tech.testsys.web.ui.navigation

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.NativeButton
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import tech.testsys.web.ui.MockVaadinTests
import tech.testsys.web.ui.findAll
import tech.testsys.web.ui.testTexts

class HeaderUserMenuTests : MockVaadinTests() {
    @Test
    fun `should run configured user actions and keep disabled actions unavailable`() {
        val calls = mutableListOf<String>()
        val header = buildHeader(
            CabinetHeader(
                user = HeaderUser(
                    name = "Anna",
                    menu = HeaderUserMenu(
                        listOf(
                            HeaderUserMenuItem("Profile", HeaderDestination.Action { calls.add("profile") }),
                            HeaderUserMenuItem("Settings", HeaderDestination.Action {}, isEnabled = false),
                            HeaderUserMenuItem("Exit", HeaderDestination.Action { calls.add("exit") }, isDestructive = true),
                        ),
                    ),
                ),
            ),
            testTexts,
        )
        UI.getCurrent().add(header)
        val buttons = header.findAll("ts-header-user-item").filterIsInstance<NativeButton>()

        buttons.first()._click()

        assertEquals(listOf("profile"), calls)
        assertFalse(buttons[1].isEnabled)
        assertEquals("Меню: Anna", header.findAll("ts-header__user").single().element.getAttribute("aria-label"))
    }
}
