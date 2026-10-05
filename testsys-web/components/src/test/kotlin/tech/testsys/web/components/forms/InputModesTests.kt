package tech.testsys.web.components.forms

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestRow

internal class InputModesTests : MockVaadinTests() {
    @Test
    fun `should retain switch and code values while block becomes readonly`() {
        lateinit var toggle: ValueInput<Boolean>
        lateinit var code: ValueInput<String>
        buildTestRow {
            toggle = switchInput("Switch", 2, 2)
            code = codeEditor("Code", 2, 6)
        }
        toggle.value = true
        code.value = "one\ntwo"

        toggle.isEditable = false
        code.isEditable = false

        assertEquals(true, toggle.value)
        assertEquals("one\ntwo", code.value)
        assertEquals(true, code.isReadOnly)
    }
}
