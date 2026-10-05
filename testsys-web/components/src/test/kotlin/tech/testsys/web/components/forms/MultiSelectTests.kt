package tech.testsys.web.components.forms

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.html.NativeButton
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.control
import tech.testsys.web.components.find
import tech.testsys.web.components.testTexts

internal class MultiSelectTests : MockVaadinTests() {
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
}
