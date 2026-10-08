package tech.testsys.web.components.forms

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.control

internal class RadioChoicesTests : MockVaadinTests() {
    @Test
    fun `should reject values outside segmented options`() {
        lateinit var input: ValueInput<String?>
        buildTestRow {
            input = segmentedControl(
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

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 5])
    fun `should reject segmented choices outside the supported range`(count: Int) {
        assertThrows(IllegalArgumentException::class.java) {
            buildTestRow {
                segmentedControl("Choice", labelSize = 4, size = 8, items = (0 until count).toList(), itemLabel = Int::toString)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [2, 4])
    fun `should accept segmented choices at the supported boundaries`(count: Int) {
        lateinit var input: ValueInput<Int?>
        buildTestRow {
            input = segmentedControl("Choice", labelSize = 4, size = 8, items = (0 until count).toList(), itemLabel = Int::toString)
        }

        input.value = count - 1

        assertEquals(count - 1, input.value)
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
