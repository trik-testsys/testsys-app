package tech.testsys.web.components.forms

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._setValue
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.popover.Popover
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.control
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll
import tech.testsys.web.components.pendingJavaScript
import tech.testsys.web.components.testTexts

internal class MultiSelectTests : MockVaadinTests() {
    private lateinit var input: ValueInput<Set<String>>

    @Test
    fun `should caption the clear action`() {
        buildMultiSelect()

        assertEquals(testTexts.lookup.clear, clearAction().text)
    }

    @Test
    fun `should clear accepted values through the clear action`() {
        buildMultiSelect()
        input.value = setOf("Анна", "Борис")

        clearAction()._click()

        assertEquals(emptySet<String>(), input.value)
        assertFalse(clearAction().isVisible)
    }

    @Test
    fun `should discard the draft on close`() {
        buildMultiSelect()
        field().open()
        field().toggle("Анна")

        field().close()

        assertEquals(emptySet<String>(), input.value)
    }

    @Test
    fun `should accept the draft on apply`() {
        buildMultiSelect()
        field().open()
        field().toggle("Борис")

        _find<NativeButton>().single { button -> button.text == testTexts.lookup.apply }._click()

        assertEquals(setOf("Борис"), input.value)
    }

    @Test
    fun `should reject stale draft after programmatic multiselect change`() {
        buildMultiSelect()
        field().open()
        field().toggle("Анна")

        input.value = setOf("Борис")
        field().applyDraft()

        assertEquals(setOf("Борис"), input.value)
    }

    @Test
    fun `should select all options that match the search`() {
        buildMultiSelect()
        field().open()
        _find<TextField>().single()._setValue("Ар")

        selectAll()._setValue(true)

        assertTrue(_find<Span>().any { count -> count.text == testTexts.lookup.selectedCount(1) })
    }

    @Test
    fun `should change only the draft on reset`() {
        buildMultiSelect()
        input.value = setOf("Анна")
        field().open()

        _find<NativeButton>().single { button -> button.text == testTexts.lookup.reset }._click()

        assertEquals(setOf("Анна"), input.value)
        assertTrue(_find<Span>().any { count -> count.text == testTexts.lookup.selectedCount(0) })
    }

    @Test
    fun `should remove an accepted value immediately through its chip`() {
        buildMultiSelect()
        input.value = setOf("Анна", "Борис")
        val remove = field().findAll("ts-chip__x").filterIsInstance<NativeButton>()
            .single { chip -> chip.element.getAttribute("aria-label") == testTexts.lookup.remove("Анна") }

        remove._click()

        assertEquals(setOf("Борис"), input.value)
    }

    @Test
    fun `should close the popup on a programmatic value`() {
        buildMultiSelect()
        field().open()

        input.value = setOf("Борис")

        assertFalse(_find<Popover>().single().isOpened)
    }

    @Test
    fun `should show the number of accepted values in the count display`() {
        buildMultiSelect(display = MultiSelectDisplay.Count)

        input.value = setOf("Анна", "Борис")

        assertEquals(testTexts.lookup.selectedCount(2), field().find("ts-counter").element.text)
    }

    @Test
    fun `should name the chip of hidden values by their number`() {
        buildMultiSelect(maxChips = 1)

        input.value = setOf("Анна", "Аркадий", "Борис")

        val more = field().find("ts-chip--more")
        assertEquals("+2", more.element.text)
        assertEquals(testTexts.components.more(2), more.element.getAttribute("aria-label"))
    }

    @Test
    fun `should ask the client to return focus to the trigger with the closed popup`() {
        buildMultiSelect()
        field().open()
        val popup = _find<Popover>().single()
        val trigger = field().find("ts-lookup__text")
        pendingJavaScript()

        field().close()

        val call = pendingJavaScript().single { call -> call.owner == trigger.element.node }
        assertTrue("window.testsysPopupFocus.restore(this, \$0)" in call.invocation.expression)
        assertEquals(popup.element, call.invocation.parameters.first())
    }

    @ParameterizedTest
    @EnumSource(Lock::class)
    fun `should close the popup when the field stops accepting choices`(lock: Lock) {
        buildMultiSelect()
        field().open()
        field().toggle("Анна")

        lock.apply(field())

        assertFalse(_find<Popover>().single().isOpened)
    }

    @Test
    fun `should open again from the accepted value after a read-only period`() {
        buildMultiSelect()
        field().open()
        field().toggle("Анна")
        field().isReadOnly = true
        field().isReadOnly = false

        field().open()

        assertTrue(_find<Span>().any { count -> count.text == testTexts.lookup.selectedCount(0) })
        assertEquals(emptySet<String>(), input.value)
    }

    /** Ways a multi-selection stops accepting user choices. */
    enum class Lock(val apply: (MultiSelectField<String>) -> Unit) {
        Disabled({ field -> field.isEnabled = false }),
        ReadOnly({ field -> field.isReadOnly = true }),
    }

    private fun buildMultiSelect(display: MultiSelectDisplay = MultiSelectDisplay.Chips, maxChips: Int = 3) {
        buildTestRow {
            input = multiSelect(
                "Участники",
                labelSize = 4,
                size = 8,
                items = listOf("Анна", "Аркадий", "Борис"),
                itemLabel = { value -> value },
                display = display,
                maxChips = maxChips,
            )
        }
    }

    private fun field(): MultiSelectField<String> = control("Участники")

    private fun clearAction(): NativeButton = field().findAll("ts-multiselect__clear").filterIsInstance<NativeButton>().single()

    private fun selectAll(): Checkbox = _find<Checkbox>().single { checkbox -> checkbox.label == testTexts.components.selectAll }
}
