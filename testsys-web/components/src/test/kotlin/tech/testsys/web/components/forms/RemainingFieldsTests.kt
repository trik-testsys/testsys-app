package tech.testsys.web.components.forms

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.html.NativeButton
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.control
import tech.testsys.web.components.find
import tech.testsys.web.components.testTexts

class RemainingFieldsTests : MockVaadinTests() {
    @Test
    fun `should pass only the desired line count to the common code geometry`() {
        val root = tech.testsys.web.components.buildTestRow { codeEditor("Code", 4, 20, minLines = 6) }
        val box = root.find("ts-code")
        assertEquals("6", box.element.style.get("--ts-code-min-lines"))
        assertEquals(null, box.element.style.get("height"))
    }

    @Test
    fun `should clear accepted multiselect values through visible clear caption`() {
        lateinit var input: ValueInput<Set<String>>
        buildTestRow { input = multiSelect("Items", 4, 8, listOf("a", "b"), { value -> value }) }
        input.value = setOf("a", "b")
        val field = control<MultiSelectField<String>>("Items")
        val clear = field.find("ts-multiselect__clear") as NativeButton
        assertEquals(testTexts.lookup.clear, clear.text)

        clear._click()

        assertEquals(emptySet<String>(), input.value)
        assertEquals(false, clear.isVisible)
    }

    @Test
    fun `should discard multiselect draft on close and accept only apply`() {
        lateinit var input: ValueInput<Set<String>>
        buildTestRow { input = multiSelect("Items", 4, 8, listOf("a", "b"), { value -> value }) }
        val field = control<MultiSelectField<String>>("Items")
        field.open()
        field.toggle("a")
        field.close()
        assertEquals(emptySet<String>(), input.value)
        field.open()
        field.toggle("b")
        field.applyDraft()
        assertEquals(setOf("b"), input.value)
    }

    @Test
    fun `should reject stale draft after programmatic multiselect change`() {
        lateinit var input: ValueInput<Set<String>>
        buildTestRow { input = multiSelect("Items", 4, 8, listOf("a", "b"), { value -> value }) }
        val field = control<MultiSelectField<String>>("Items")
        field.open()
        field.toggle("a")

        input.value = setOf("b")
        field.applyDraft()

        assertEquals(setOf("b"), input.value)
    }

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

    @Test
    fun `should reject values outside segmented options`() {
        lateinit var input: ValueInput<String?>
        buildTestRow { input = segmentedControl(
                "Choice",
                4,
                8,
                listOf("a", "b"),
                { value -> value },
            ) }

        assertThrows(IllegalArgumentException::class.java) { input.value = "c" }
    }
}
