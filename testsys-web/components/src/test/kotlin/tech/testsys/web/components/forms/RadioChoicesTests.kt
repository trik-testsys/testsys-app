package tech.testsys.web.components.forms

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.control

internal class RadioChoicesTests : MockVaadinTests() {
    @Test
    fun `should reject values outside radio options`() {
        lateinit var input: ValueInput<String?>
        buildTestRow {
            input = radio(
                "Choice",
                labelSize = 4,
                size = 8,
                items = listOf("a", "b"),
                itemLabel = { value -> value },
            )
        }

        assertThrows(IllegalArgumentException::class.java) { input.value = "c" }
    }

    @Test
    fun `should reject duplicate radio choices`() {
        assertThrows(IllegalArgumentException::class.java) {
            buildTestRow { radio("Choice", labelSize = 4, size = 8, items = listOf("a", "a"), itemLabel = { value -> value }) }
        }
    }

    @Test
    fun `should clear a selected radio choice to null`() {
        lateinit var input: ValueInput<String?>
        buildTestRow { input = radio("Choice", labelSize = 4, size = 8, items = listOf("a", "b"), itemLabel = { value -> value }) }
        input.value = "a"

        input.clear()

        assertEquals(null, input.value)
        assertEquals(null, input.emptyValue)
    }

    @Test
    fun `should render radio labels and metadata`() {
        buildTestRow {
            radio(
                "Choice",
                labelSize = 4,
                size = 8,
                items = listOf("a", "b"),
                itemLabel = { value -> "Label $value" },
                itemMeta = { value -> "Meta $value" },
            )
        }
        val field = control<ChoiceField<String>>("Choice")

        assertEquals("Choice", field.ariaLabel.orElseThrow())
        assertEquals("Label a", field.itemLabelGenerator.apply("a"))
        assertEquals("Label aMeta a", field.itemRenderer.createComponent("a").element.textRecursively)
    }
}
