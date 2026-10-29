package tech.testsys.web.components.actions

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.button.Button
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.pendingJavaScript

class CopyActionTests : MockVaadinTests() {
    @Test
    fun `should be a neutral action with the label`() {
        buildTestContent { copyAction("Скопировать", value = { "code" }) }

        val button = _get<Button>()
        assertEquals("Скопировать", button.text)
        assertEquals("neutral", button.element.getAttribute("data-ts-role"))
    }

    @Test
    fun `should write the value read at the click to the clipboard`() {
        var value = "first"
        buildTestContent { copyAction("Скопировать", value = { value }) }
        pendingJavaScript()
        value = "second"

        _get<Button>()._click()

        val calls = pendingJavaScript().filter { call -> "navigator.clipboard.writeText" in call.invocation.expression }
        assertEquals(listOf("second"), calls.map { call -> call.invocation.parameters.first() })
    }

    @Test
    fun `should not copy anything before the click`() {
        buildTestContent { copyAction("Скопировать", value = { "code" }) }

        val calls = pendingJavaScript()

        assertTrue(calls.none { call -> "navigator.clipboard" in call.invocation.expression })
    }
}
