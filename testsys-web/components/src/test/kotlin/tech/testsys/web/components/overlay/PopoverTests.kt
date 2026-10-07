package tech.testsys.web.components.overlay

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.display.text
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.Placement
import tech.testsys.web.components.pendingJavaScript

internal class PopoverTests : MockVaadinTests() {
    @ParameterizedTest
    @CsvSource("Body,md", "PageHead,md", "Head,sm", "Cell,sm", "Empty,sm")
    fun `should size popup triggers by placement`(placement: Placement, size: String) {
        val root = buildTestContent {
            ContentScope(container, texts, placement, gridColumns).popover("Popup") { text("Body") }
        }

        assertEquals(size, trigger(root).element.getAttribute("data-ts-size"))
    }

    @Test
    fun `should open the popup`() {
        lateinit var handle: PopoverHandle
        buildTestContent { handle = popover("Popup") { text("Body") } }

        handle.open()

        assertTrue(handle.isOpen)
    }

    @Test
    fun `should close an open popup when the trigger is disabled`() {
        lateinit var handle: PopoverHandle
        buildTestContent { handle = popover("Popup") { text("Body") } }
        handle.open()

        handle.isEnabled = false

        assertFalse(handle.isOpen)
    }

    @Test
    fun `should not open the popup of a disabled trigger`() {
        lateinit var handle: PopoverHandle
        buildTestContent { handle = popover("Popup") { text("Body") } }
        handle.isEnabled = false

        handle.open()

        assertFalse(handle.isOpen)
    }

    @Test
    fun `should close an open popup`() {
        lateinit var handle: PopoverHandle
        buildTestContent { handle = popover("Popup") { text("Body") } }
        handle.open()

        handle.close()

        assertFalse(handle.isOpen)
    }

    @Test
    fun `should run only the last close callback`() {
        lateinit var handle: PopoverHandle
        var calls = 0
        buildTestContent { handle = popover("Popup") { text("Body") } }
        handle.onClose { calls += 10 }
        handle.onClose { calls++ }
        handle.open()

        handle.close()

        assertEquals(1, calls)
    }

    @Test
    fun `should leave focus restoration to Vaadin after closing`() {
        lateinit var handle: PopoverHandle
        val root = buildTestContent { handle = popover("Popup") { text("Body") } }
        handle.open()
        pendingJavaScript()

        handle.close()

        val trigger = trigger(root)
        assertTrue(pendingJavaScript().none { call -> call.owner == trigger.element.node && ".focus(" in call.invocation.expression })
    }

    private fun trigger(root: Component): Button = root.children.toList().single().children.toList().filterIsInstance<Button>().single()
}
