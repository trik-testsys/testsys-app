package tech.testsys.web.components.forms

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.find

internal class CodeEditorGeometryTests : MockVaadinTests() {
    @Test
    fun `should pass only the desired line count to the common code geometry`() {
        val root = tech.testsys.web.components.buildTestRow { codeEditor("Code", labelSize = 4, size = 20, minLines = 6) }
        val box = root.find("ts-code")
        assertEquals("6", box.element.style.get("--ts-code-min-lines"))
        assertEquals(null, box.element.style.get("height"))
    }
}
