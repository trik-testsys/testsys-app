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
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.openDialogs

internal class DrawerTests : MockVaadinTests() {
    private lateinit var field: ValueInput<String>

    @BeforeEach
    fun buildTextsOwner() {
        buildTestContent { text("Texts owner") }
    }

    @Test
    fun `should reopen drawer with retained value and inherited readonly mode`() {
        val handle = drawer("Drawer") {
            row { field = textInput("Name", labelSize = 4, size = 8) { value = "Retained" } }
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
        val handle = drawer("Drawer") { footer { opened -> action("Готово") { onClick { opened.close() } } } }
        handle.open()
        val done = openDialogs().single()._find<Button>().single { action -> action.text == "Готово" }

        done._click()

        assertFalse(handle.isOpen)
    }

    @Test
    fun `should call the latest close listener when the drawer closes`() {
        var calls = 0
        val handle = drawer("Drawer") { row { field = textInput("Name", labelSize = 4, size = 8) } }
        handle.onClose { calls += 10 }
        handle.onClose { calls++ }
        handle.open()

        handle.close()

        assertEquals(1, calls)
    }

    @Test
    fun `should apply the bound edit mode when opened`() {
        val handle = drawer("Drawer") { row { field = textInput("Name", labelSize = 4, size = 8) } }
        handle.bindEditable(ValueSignal(false))

        handle.open()

        assertTrue(field.isReadOnly)
    }

    @Test
    fun `should show the subtitle under the title`() {
        val handle = drawer("Drawer", subtitle = "Сведения о посылке") {
            row { field = textInput("Name", labelSize = 4, size = 8) }
        }

        handle.open()

        val subtitle = openDialogs().single()._find<Span> { classes = "ts-block__sub" }.single()
        assertEquals("Сведения о посылке", subtitle.text)
    }
}
