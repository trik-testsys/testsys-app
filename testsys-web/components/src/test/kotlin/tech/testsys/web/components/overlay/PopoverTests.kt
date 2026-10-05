package tech.testsys.web.components.overlay

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.display.text

internal class PopoverTests : MockVaadinTests() {
    @ParameterizedTest
    @CsvSource("Body,md", "PageHead,md", "Head,sm", "Cell,sm", "Empty,sm")
    fun `should size popup triggers by placement`(name: String, size: String) {
        val placement = tech.testsys.web.components.layout.Placement.valueOf(name)
        lateinit var handle: PopoverHandle
        val root = buildTestContent {
            handle = tech.testsys.web.components.layout.ContentScope(container, texts, placement, gridColumns).popover("Popup") { text("Body") }
        }
        val trigger = root.children.toList().single().children.toList().filterIsInstance<com.vaadin.flow.component.button.Button>().single()

        handle.open()

        org.junit.jupiter.api.Assertions.assertEquals(size, trigger.element.getAttribute("data-ts-size"))
        assertTrue(handle.isOpen)
        handle.isEnabled = false
        assertFalse(handle.isOpen)
        handle.open()
        assertFalse(handle.isOpen)
    }

    @Test
    fun `should close popup and replace close callback`() {
        lateinit var handle: PopoverHandle
        var calls = 0
        buildTestContent { handle = popover("Popup") { text("Body") } }
        handle.onClose { calls += 10 }
        handle.onClose { calls++ }
        handle.open()

        handle.close()

        assertFalse(handle.isOpen)
        org.junit.jupiter.api.Assertions.assertEquals(1, calls)
    }
}
