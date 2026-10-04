package tech.testsys.web.components.overlay

import com.github.mvysny.kaributesting.v10._clickItemWithCaption
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.contextmenu.MenuItem
import com.vaadin.flow.component.html.Hr
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.button
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.TableScope
import tech.testsys.web.components.data.table
import tech.testsys.web.components.find
import tech.testsys.web.components.findAllButtons
import tech.testsys.web.components.testTexts

class MenuTests : MockVaadinTests() {
    @Test
    fun `should open the menu from an ellipsis named for screen readers`() {
        buildTestPage { block(title = "Тур") { actions { menu { item("Открыть") {} } } } }

        val trigger = ellipsis()
        assertEquals(testTexts.menu.actions, trigger.element.getAttribute("aria-label"))
        assertEquals("menu", trigger.element.getAttribute("aria-haspopup"))
        assertEquals("sm", trigger.element.getAttribute("data-ts-size"))
        assertTrue(menuOf(trigger).isOpenOnClick)
        assertEquals("ts-menu", menuOf(trigger).element.getAttribute("theme"))
    }

    @Test
    fun `should open the menu from a text button`() {
        buildTestPage { block(title = "Тур") { actions { menu(label = "Ещё") { item("Открыть") {} } } } }

        assertEquals("menu", button("Ещё").element.getAttribute("aria-haspopup"))
    }

    @Test
    fun `should list items in order with a separator before destructive ones`() {
        buildTestPage {
            block(title = "Тур") {
                actions {
                    menu {
                        item("Открыть") {}
                        item("Дублировать") {}
                        destructiveItem("Удалить") {}
                        destructiveItem("Архивировать") {}
                    }
                }
            }
        }

        val children = menuOf(ellipsis()).children.toList()
        assertEquals(listOf("Открыть", "Дублировать", "-", "Удалить", "Архивировать"), children.map(::caption))
        assertTrue("ts-menu__item--danger" in children[3].element.classList)
        assertFalse("ts-menu__item--danger" in children[0].element.classList)
    }

    @Test
    fun `should mark every item of the menu as a menu item`() {
        buildTestPage {
            block(title = "Тур") {
                actions {
                    menu {
                        item("Открыть") {}
                        destructiveItem("Удалить") {}
                    }
                }
            }
        }

        val items = menuOf(ellipsis()).children.toList().filterIsInstance<MenuItem>()
        assertEquals(listOf(true, true), items.map { item -> "ts-menu__item" in item.element.classList })
    }

    @Test
    fun `should not add a separator to a menu of destructive items only`() {
        buildTestPage { block(title = "Тур") { actions { menu { destructiveItem("Удалить") {} } } } }

        assertEquals(listOf("Удалить"), menuOf(ellipsis()).children.toList().map(::caption))
    }

    @Test
    fun `should run the handler of the chosen item`() {
        val chosen = mutableListOf<String>()
        buildTestPage { block(title = "Тур") { actions { menu { item("Открыть") { chosen += "open" } } } } }

        menuOf(ellipsis())._clickItemWithCaption("Открыть")

        assertEquals(listOf("open"), chosen)
    }

    @Test
    fun `should show a disabled item`() {
        buildTestPage { block(title = "Тур") { actions { menu { item("Открыть", isEnabled = false) {} } } } }

        assertFalse((menuOf(ellipsis()).children.toList().single() as MenuItem).isEnabled)
    }

    @Test
    fun `should hide the trigger through the returned handle`() {
        buildTestPage { block(title = "Тур") { actions { menu { item("Открыть") {} }.isVisible = false } } }

        assertFalse(ellipsis().isVisible)
    }

    @Test
    fun `should reject a menu without items`() {
        assertThrows<IllegalStateException> { buildTestPage { block { actions { menu {} } } } }
    }

    @Test
    fun `should reject an item after a destructive one`() {
        assertThrows<IllegalStateException> {
            buildTestPage {
                block {
                    actions {
                        menu {
                            destructiveItem("Удалить") {}
                            item("Открыть") {}
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should reject a component that opens no menu`() {
        assertThrows<IllegalStateException> { menuOf(Button("Открыть")) }
    }

    @Test
    fun `should add a last shared grid menu column named for screen readers to a table`() {
        buildMenuTable { item("Открыть") {} }

        val header = UI.getCurrent().find("ts-table").children.toList()[0].children.toList()[0].children.toList().last()
        assertEquals("50.0%", UI.getCurrent().find("ts-table").element.getChild(0).children.toList().last().style.get("width"))
        assertEquals(testTexts.menu.actions, header.element.getAttribute("aria-label"))
    }

    @Test
    fun `should add a small menu trigger to each row of a table`() {
        buildMenuTable { item("Открыть") {} }

        assertEquals(listOf("sm", "sm", "sm"), ellipses().map { trigger -> trigger.element.getAttribute("data-ts-size") })
    }

    @Test
    fun `should run the handler of the chosen item with the row of its menu`() {
        val chosen = mutableListOf<Int>()
        buildMenuTable { row -> item("Открыть") { chosen += row } }

        menuOf(ellipses()[1])._clickItemWithCaption("Открыть")

        assertEquals(listOf(2), chosen)
    }

    @Test
    fun `should reject a text column after the menu column`() {
        assertThrows<IllegalStateException> {
            buildMenuTable(extra = { textColumn("Ещё", size = 1) { row -> "$row" } }) { item("Открыть") {} }
        }
    }

    @Test
    fun `should reject a content column after the menu column`() {
        assertThrows<IllegalStateException> {
            buildMenuTable(extra = { column("Ещё", size = 1) {} }) { item("Открыть") {} }
        }
    }

    @Test
    fun `should reject a second menu column`() {
        assertThrows<IllegalStateException> {
            buildMenuTable(extra = { menuColumn { item("Ещё") {} } }) { item("Открыть") {} }
        }
    }

    @Test
    fun `should name each row menu and keep its handler separate from row click`() {
        val chosen = mutableListOf<Int>()
        val clicked = mutableListOf<Int>()
        buildTestPage {
            block {
                table<Int>(key = { row -> row }, fetch = { Page(listOf(41, 42), 2) }) {
                    textColumn("Номер", size = 1) { row -> "$row" }
                    menuColumn(ariaLabel = { row -> "Действия с посылкой №$row" }) { row ->
                        item("Открыть") { chosen += row }
                    }
                    onRowClick { row -> clicked += row }
                }
            }
        }
        val triggers = findAllButtons(UI.getCurrent()).filter { button -> button.element.hasAttribute("aria-haspopup") }

        menuOf(triggers[1])._clickItemWithCaption("Открыть")

        assertEquals(
            listOf("Действия с посылкой №41", "Действия с посылкой №42"),
            triggers.map { trigger -> trigger.ariaLabel.orElseThrow() },
        )
        assertEquals(listOf(42), chosen)
        assertTrue(clicked.isEmpty())
    }

    private fun buildMenuTable(
        extra: TableScope<Int>.() -> Unit = {},
        content: MenuScope.(Int) -> Unit,
    ) {
        buildTestPage {
            block {
                table<Int>(key = { row -> row }, fetch = { Page(listOf(1, 2, 3), 3) }) {
                    textColumn("Номер", size = 1) { row -> "$row" }
                    menuColumn(content = content)
                    extra()
                }
            }
        }
    }

    private fun ellipsis(): Button = ellipses().single()

    private fun ellipses(): List<Button> =
        findAllButtons(UI.getCurrent()).filter { button -> button.element.getAttribute("aria-label") == testTexts.menu.actions }

    private fun caption(item: Component): String = if (item is Hr) "-" else item.element.textRecursively
}
