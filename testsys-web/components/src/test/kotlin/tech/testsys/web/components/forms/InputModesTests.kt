package tech.testsys.web.components.forms

import com.vaadin.flow.component.checkbox.Switch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.control
import tech.testsys.web.components.layout.BlockHandle

internal class InputModesTests : MockVaadinTests() {
    private lateinit var block: BlockHandle
    private lateinit var toggle: ValueInput<Boolean>
    private lateinit var code: ValueInput<String>

    @Test
    fun `should retain switch and code values when their block becomes read-only`() {
        buildModes()
        toggle.value = true
        code.value = "one\ntwo"

        block.isEditable = false

        assertEquals(true, toggle.value)
        assertEquals("one\ntwo", code.value)
        assertTrue(toggle.isReadOnly)
        assertTrue(code.isReadOnly)
    }

    @Test
    fun `should mark switch and code editor as inputs that row clicks ignore`() {
        buildModes()

        assertTrue(control<Switch>("Switch").element.hasAttribute("data-ts-input"))
        assertTrue(control<CodeEditorField>("Code").element.hasAttribute("data-ts-input"))
    }

    private fun buildModes() {
        buildTestPage {
            block = block {
                row {
                    toggle = switchInput("Switch", labelSize = 2, size = 2)
                    code = codeEditor("Code", labelSize = 2, size = 6)
                }
            }
        }
    }
}
