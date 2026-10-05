package tech.testsys.web.components.forms

import com.vaadin.flow.component.UI
import com.vaadin.flow.component.checkbox.Switch
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.dom.Element
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap
import com.vaadin.flow.internal.nodefeature.NodeFeatureRegistry
import com.vaadin.flow.server.communication.rpc.MapSyncRpcHandler
import com.vaadin.flow.shared.JsonConstants
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.control
import tech.testsys.web.components.find
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.JsonNodeFactory

class NativeFieldsTests : MockVaadinTests() {
    private class Draft(var code: String = "", var enabled: Boolean = false, var choice: String? = null)

    @Nested
    inner class CodeEditorTests {
        // Decorative line numbers must stay outside the accessibility tree.
        @Test
        @Tag("regression")
        fun `should hide decorative code line numbers from assistive technology`() {
            val root = buildTestRow { codeEditor("Code", 4, 8) { value = "first\nsecond" } }

            val numbers = root.find("ts-code__lines")

            assertEquals("1\n2", numbers.element.text)
            assertEquals("true", numbers.element.getAttribute("aria-hidden"))
        }

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
        fun `should publish client checked changes to the handle and Binder`() {
            lateinit var input: ValueInput<Boolean>
            buildTestRow { input = switchInput("Switch", 4, 8) }
            val field = control<Switch>("Switch")
            val binder = Binder<Draft>()
            val draft = Draft()
            binder.forField(input).bind({ bean -> bean.enabled }, { bean, enabled -> bean.enabled = enabled })

            clientChange(field.element, "checked", JsonNodeFactory.instance.booleanNode(true))
            binder.writeBean(draft)

            assertEquals(true, input.value)
            assertEquals(true, draft.enabled)
            assertEquals("Switch", field.ariaLabel.orElseThrow())
        }

        @Test
        fun `should ignore client checked changes while readonly`() {
            lateinit var input: ValueInput<Boolean>
            buildTestRow { input = switchInput("Switch", 4, 8) { isEditable = false } }
            val field = control<Switch>("Switch")

            clientChange(field.element, "checked", JsonNodeFactory.instance.booleanNode(true))

            assertEquals(false, input.value)
            assertEquals(false, field.element.getProperty("checked", false))
        }

        @Test
        fun `should ignore client checked changes while disabled`() {
            lateinit var input: ValueInput<Boolean>
            buildTestRow { input = switchInput("Switch", 4, 8) { isEnabled = false } }

            clientChange(control<Switch>("Switch").element, "checked", JsonNodeFactory.instance.booleanNode(true))

            assertEquals(false, input.value)
        }

        @Test
        fun `should accept programmatic checked changes while readonly and disabled`() {
            lateinit var input: ValueInput<Boolean>
            buildTestRow { input = switchInput("Switch", 4, 8) { isEditable = false; isEnabled = false } }

            input.value = true

            assertEquals(true, input.value)
        }

        @Test
        fun `should publish client switch requests without replacing the bound signal`() {
            val source = ValueSignal(false)
            var requested = false
            lateinit var input: ValueInput<Boolean>
            buildTestRow { input = switchInput("Switch", 4, 8) { bindValue(source) { value -> requested = value } } }

            clientChange(control<Switch>("Switch").element, "checked", JsonNodeFactory.instance.booleanNode(true))

            assertEquals(true, requested)
            assertEquals(false, source.peek())
            assertEquals(false, input.value)
        }
    }

    @Nested
    inner class RadioTests {
        @Test
        fun `should publish client choice changes to the handle and Binder`() {
            lateinit var input: ValueInput<String?>
            buildTestRow { input = radio("Choice", 4, 8, listOf("a", "b"), { value -> value }) }
            val field = control<ChoiceField<String>>("Choice")
            val binder = Binder<Draft>()
            val draft = Draft()
            binder.forField(input).bind({ bean -> bean.choice }, { bean, choice -> bean.choice = choice })

            clientChoice(field)
            binder.writeBean(draft)

            assertEquals("b", input.value)
            assertEquals("b", draft.choice)
        }

        @Test
        fun `should ignore client choice changes while readonly`() {
            lateinit var input: ValueInput<String?>
            buildTestRow { input = radio("Choice", 4, 8, listOf("a", "b"), { value -> value }) { value = "a"; isEditable = false } }

            clientChoice(control("Choice"))

            assertEquals("a", input.value)
        }

        @Test
        fun `should ignore client choice changes while disabled`() {
            lateinit var input: ValueInput<String?>
            buildTestRow { input = radio("Choice", 4, 8, listOf("a", "b"), { value -> value }) { value = "a"; isEnabled = false } }

            clientChoice(control("Choice"))

            assertEquals("a", input.value)
        }

        @Test
        fun `should publish client radio requests without replacing the bound signal`() {
            val source = ValueSignal<String?>("a")
            var requested: String? = null
            lateinit var input: ValueInput<String?>
            buildTestRow {
                input = radio("Choice", 4, 8, listOf("a", "b"), { value -> value }) {
                    bindValue(source) { value -> requested = value }
                }
            }

            clientChoice(control("Choice"))

            assertEquals("b", requested)
            assertEquals("a", source.peek())
            assertEquals("a", input.value)
        }
    }

    private fun clientChoice(field: ChoiceField<String>) {
        val choice = field.children.filter { child -> child.element.tag == "vaadin-radio-button" }.toList()[1]
        clientChange(field.element, "value", JsonNodeFactory.instance.stringNode(choice.element.getProperty("value")))
    }

    private fun clientChange(element: Element, property: String, value: JsonNode) {
        val invocation = JsonNodeFactory.instance.objectNode()
            .put(JsonConstants.RPC_NODE, element.node.id)
            .put(JsonConstants.RPC_FEATURE, NodeFeatureRegistry.getId(ElementPropertyMap::class.java))
            .put(JsonConstants.RPC_PROPERTY, property)
        invocation.set(JsonConstants.RPC_PROPERTY_VALUE, value)
        MapSyncRpcHandler().handle(UI.getCurrent(), invocation).ifPresent { update -> update.run() }
    }
}
