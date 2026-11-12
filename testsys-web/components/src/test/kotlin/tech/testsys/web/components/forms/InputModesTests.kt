package tech.testsys.web.components.forms

import com.vaadin.flow.component.checkbox.Checkbox
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
    fun `should retain checkbox and code values when their block becomes read-only`() {
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
    fun `should mark checkbox and code editor as inputs that row clicks ignore`() {
        buildModes()

        assertTrue(control<Checkbox>("Checkbox").element.hasAttribute("data-ts-input"))
        assertTrue(control<CodeEditorField>("Code").element.hasAttribute("data-ts-input"))
    }

    private fun buildModes() {
        buildTestPage {
            row {
                block = block {
                    row {
                        toggle = checkbox("Checkbox", labelSize = 2, size = 2)
                        code = codeEditor("Code", labelSize = 2, size = 6)
                    }
                }
            }
        }
    }
}
