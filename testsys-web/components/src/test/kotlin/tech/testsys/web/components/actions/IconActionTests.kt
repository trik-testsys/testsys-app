package tech.testsys.web.components.actions

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.button.Button
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.table
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.Placement

internal class IconActionTests : MockVaadinTests() {
    @ParameterizedTest
    @CsvSource(
        "Main,Body,md",
        "Neutral,Body,md",
        "Destructive,Body,md",
        "Main,Head,sm",
        "Neutral,Head,sm",
        "Destructive,Head,sm",
        "Main,Cell,sm",
        "Neutral,Cell,sm",
        "Destructive,Cell,sm",
    )
    fun `should preserve icon role name tooltip and placement`(role: ActionRole, placement: Placement, size: String) {
        buildInPlacement(role, placement)

        val button = _get<Button>()
        assertEquals(role.attribute, button.element.getAttribute("data-ts-role"))
        assertEquals(size, button.element.getAttribute("data-ts-size"))
        assertEquals("Action name", button.ariaLabel.orElseThrow())
        assertEquals("Action name", button.tooltip.text)
        assertTrue(button.text.isEmpty())
        assertTrue(button.element.hasAttribute("data-ts-icon-only"))
        assertTrue("ts-icon" in button.icon.element.classList)
    }

    @ParameterizedTest
    @EnumSource(value = ActionRole::class, names = ["Main", "Neutral", "Destructive"])
    fun `should ignore clicks while loading`(role: ActionRole) {
        var clicks = 0
        lateinit var handle: ActionHandle
        buildTestContent { handle = iconOnly(role).apply { onClick { clicks++ } } }
        handle.isLoading = true

        _get<Button>()._click()

        assertEquals(0, clicks)
        assertEquals("true", _get<Button>().element.getAttribute("aria-busy"))
    }

    @ParameterizedTest
    @EnumSource(value = ActionRole::class, names = ["Main", "Neutral", "Destructive"])
    fun `should run the click callback after loading ends`(role: ActionRole) {
        var clicks = 0
        lateinit var handle: ActionHandle
        buildTestContent { handle = iconOnly(role).apply { onClick { clicks++ } } }
        handle.isLoading = true
        handle.isLoading = false

        _get<Button>()._click()

        assertEquals(1, clicks)
    }

    @ParameterizedTest
    @EnumSource(value = ActionRole::class, names = ["Main", "Neutral", "Destructive"])
    fun `should disable the button through the handle`(role: ActionRole) {
        lateinit var handle: ActionHandle
        buildTestContent { handle = iconOnly(role) }

        handle.isEnabled = false

        assertFalse(_get<Button>().isEnabled)
    }

    @ParameterizedTest
    @EnumSource(value = ActionRole::class, names = ["Main", "Neutral", "Destructive"])
    fun `should reject an empty accessible name`(role: ActionRole) {
        assertThrows<IllegalArgumentException> { buildTestContent { iconOnly(role, label = " ") } }
    }

    private fun buildInPlacement(role: ActionRole, placement: Placement) {
        buildTestPage {
            row {
                block {
                    when (placement) {
                        Placement.Head -> actions { iconOnly(role) }
                        Placement.Cell -> table(key = { id: Int -> id }, fetch = { Page(listOf(1), 1) }) {
                            column("Action") { iconOnly(role) }
                        }
                        else -> row { horizontal { iconOnly(role) } }
                    }
                }
            }
        }
    }

    private fun ContentScope.iconOnly(role: ActionRole, label: String = "Action name"): ActionHandle = when (role) {
        ActionRole.Main -> mainIconAction(IconName.Upload, label)
        ActionRole.Destructive -> destructiveIconAction(IconName.Trash, label)
        ActionRole.Neutral -> iconAction(IconName.Settings, label)
        else -> error("Unsupported icon role $role")
    }
}
