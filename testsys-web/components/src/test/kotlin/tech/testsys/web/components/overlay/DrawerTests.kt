package tech.testsys.web.components.overlay

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.display.text
import tech.testsys.web.components.findAll
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.openDialogs
import tech.testsys.web.components.testTexts

internal class DrawerTests : MockVaadinTests() {
    private lateinit var field: ValueInput<String>

    @BeforeEach
    fun buildTextsOwner() {
        buildTestContent { text("Texts owner") }
    }

    @Test
    fun `should render static tables including an empty table with shared text and no pagination`() {
        val handle = drawer("Testing") {
            table("Diagnostics", key = { value: Int -> value }, rows = listOf(1, 2)) {
                textColumn("Name") { value -> "Polygon $value" }
            }
            table("Submissions", key = { value: Int -> value }, rows = emptyList()) {
                textColumn("Status") { "Pending" }
            }
        }

        handle.open()

        val dialog = openDialogs().single()
        assertEquals(2, dialog.findAll("ts-drawer-table").size)
        assertEquals(2, dialog.findAll("ts-table-scroll").size)
        assertTrue("Polygon 1" in dialog.element.textRecursively)
        assertTrue("Polygon 2" in dialog.element.textRecursively)
        assertTrue(testTexts.table.empty in dialog.element.textRecursively)
        assertTrue(dialog.findAll("ts-table-pager").isEmpty())
        assertEquals("Diagnostics", dialog.findAll("ts-block__title").first().element.text)
    }

    @Test
    fun `should reopen drawer with retained value and inherited readonly mode`() {
        val handle = drawer("Drawer") {
            row { field = textInput("Name", labelSize = 8, size = 16) { value = "Retained" } }
        }
        handle.isEditable = false
        handle.open()
        handle.close()

        handle.open()

        assertTrue(handle.isOpen)
        assertTrue(field.isReadOnly)
        assertEquals("Retained", field.value)
    }

    @Test
    fun `should close the drawer from a footer action that receives its handle`() {
        val handle = drawer("Drawer") { footer { opened -> action("Завершить") { onClick { opened.close() } } } }
        handle.open()
        val done = openDialogs().single()._find<Button>().single { action -> action.text == "Завершить" }

        done._click()

        assertFalse(handle.isOpen)
    }

    @Test
    fun `should call the latest close listener when the drawer closes`() {
        var calls = 0
        val handle = drawer("Drawer") { row { field = textInput("Name", labelSize = 8, size = 16) } }
        handle.onClose { calls += 10 }
        handle.onClose { calls++ }
        handle.open()

        handle.close()

        assertEquals(1, calls)
    }

    @Test
    fun `should apply the bound edit mode when opened`() {
        val handle = drawer("Drawer") { row { field = textInput("Name", labelSize = 8, size = 16) } }
        handle.bindEditable(ValueSignal(false))

        handle.open()

        assertTrue(field.isReadOnly)
    }

    @Test
    fun `should show the subtitle under the title`() {
        val handle = drawer("Drawer", subtitle = "Сведения о посылке") {
            row { field = textInput("Name", labelSize = 8, size = 16) }
        }

        handle.open()

        val subtitle = openDialogs().single()._find<Span> { classes = "ts-block__sub" }.single()
        assertEquals("Сведения о посылке", subtitle.text)
    }
}
