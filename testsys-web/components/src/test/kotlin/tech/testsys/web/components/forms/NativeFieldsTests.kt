package tech.testsys.web.components.forms

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.control
import tech.testsys.web.components.find

class NativeFieldsTests : MockVaadinTests() {
    private class Draft(var code: String = "")

    @Nested
    inner class CodeEditorTests {
        @Test
        fun `should synchronize actual client value into Binder and presentation`() {
            lateinit var input: ValueInput<String>
            buildTestRow { input = codeEditor("Code", 4, 8, minLines = 3, hint = "Hint") }
            val binder = Binder<Draft>()
            val draft = Draft()
            binder.forField(input).bind({ bean -> bean.code }, { bean, code -> bean.code = code })
            val area = control<CodeEditorField>("Code").find("ts-code__area").element

            area.node.getFeature(ElementPropertyMap::class.java).deferredUpdateFromClient("value", "typed\nsecond").run()
            binder.writeBean(draft)

            assertEquals("typed\nsecond", input.value)
            assertEquals("typed\nsecond", draft.code)
            assertEquals("typed\nsecond", area.getProperty("value"))
            assertEquals("Code", area.getAttribute("aria-label"))
        }

        @Test
        fun `should reject client changes while readonly and restore visible value`() {
            lateinit var input: ValueInput<String>
            buildTestRow { input = codeEditor("Code", 4, 8) { value = "original"; isEditable = false } }
            val area = control<CodeEditorField>("Code").find("ts-code__area").element

            area.node.getFeature(ElementPropertyMap::class.java).deferredUpdateFromClient("value", "changed").run()

            assertEquals("original", input.value)
            assertEquals("original", area.getProperty("value"))
            assertTrue(area.getProperty("readOnly", false))
        }

        @Test
        fun `should keep bound signal authoritative and publish client write request`() {
            val source = ValueSignal("original")
            var requested = ""
            lateinit var input: ValueInput<String>
            buildTestRow { input = codeEditor("Code", 4, 8) { bindValue(source) { code -> requested = code } } }
            val area = control<CodeEditorField>("Code").find("ts-code__area").element

            area.node.getFeature(ElementPropertyMap::class.java).deferredUpdateFromClient("value", "request").run()

            assertEquals("request", requested)
            assertEquals("original", source.peek())
            assertEquals("original", input.value)
        }
    }

    @Nested
    inner class SwitchTests {
        @Test
        fun `should expose real switch semantics and toggle through a client click`() {
            lateinit var input: ValueInput<Boolean>
            buildTestRow { input = switchInput("Switch", 4, 8) }
            val button = requireNotNull(control<SwitchField>("Switch").find("ts-switch") as? NativeButton)

            button._click()

            assertEquals(true, input.value)
            assertEquals("switch", button.element.getAttribute("role"))
            assertEquals("true", button.element.getAttribute("aria-checked"))
        }
    }
}
