package tech.testsys.web.components.overlay

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.display.text
import tech.testsys.web.components.forms.textInput

internal class DrawerTests : MockVaadinTests() {
    @Test
    fun `should reopen drawer with retained value and inherited readonly mode`() {
        buildTestContent { text("Texts owner") }
        lateinit var field: tech.testsys.web.components.forms.ValueInput<String>
        val handle = drawer("Drawer") { row { field = textInput("Name", 4, 8) { value = "Retained" } } }
        handle.isEditable = false
        handle.open()
        handle.close()

        handle.open()

        assertTrue(handle.isOpen)
        assertTrue(field.isReadOnly)
        org.junit.jupiter.api.Assertions.assertEquals("Retained", field.value)
    }
}
