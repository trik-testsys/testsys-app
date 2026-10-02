package tech.testsys.web.components.navigation

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.classes
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll

class FilterChipTests : MockVaadinTests() {
    @Nested
    inner class MarkupTests {
        @Test
        fun `should render a native button with the label`() {
            buildChip()

            val chip = chip()
            assertEquals("button", chip.element.tag)
            assertEquals("Только мои", chip.text)
            assertEquals("button", chip.element.getAttribute("type"))
        }

        @Test
        fun `should render an unselected chip without the check icon`() {
            buildChip()

            assertEquals("false", chip().element.getAttribute("aria-pressed"))
            assertFalse("ts-filter--on" in chip().classes())
            assertTrue(chip().findAll("ts-icon").isEmpty())
        }

        @Test
        fun `should render a chip created selected with a small bold check icon`() {
            buildChip(isSelected = true)

            assertEquals("true", chip().element.getAttribute("aria-pressed"))
            assertTrue("ts-filter--on" in chip().classes())
            val svg = chip().find("ts-icon").element.getProperty("innerHTML")
            assertTrue(svg.contains("""<path d="M20 6 9 17l-5-5"/>"""))
            assertTrue(svg.contains("""width="12""""))
            assertTrue(svg.contains("""stroke-width="3""""))
        }

        @Test
        fun `should put the check icon before the label`() {
            buildChip(isSelected = true)

            assertEquals(chip().find("ts-icon").element, chip().element.getChild(0))
        }

        @Test
        fun `should render the chip in the block head`() {
            buildTestPage { block(title = "Посылки") { actions { filterChip("Только мои") } } }

            assertEquals(UI.getCurrent().find("ts-block__actions"), chip().parent.orElseThrow())
        }

        @Test
        fun `should hide the chip through the handle`() {
            val handle = buildChip()

            handle.isVisible = false

            assertFalse(chip().isVisible)
        }
    }

    @Nested
    inner class ClickTests {
        @Test
        fun `should select the chip and tell the listener on a click`() {
            val handle = buildChip()
            val changes = mutableListOf<Boolean>()
            handle.onChange { selected -> changes += selected }

            chip()._click()

            assertEquals(listOf(true), changes)
            assertTrue(handle.isSelected)
            assertEquals("true", chip().element.getAttribute("aria-pressed"))
            assertTrue("ts-filter--on" in chip().classes())
            assertEquals(1, chip().findAll("ts-icon").size)
        }

        @Test
        fun `should unselect the chip and remove the check icon on a second click`() {
            val handle = buildChip()
            val changes = mutableListOf<Boolean>()
            handle.onChange { selected -> changes += selected }
            chip()._click()

            chip()._click()

            assertEquals(listOf(true, false), changes)
            assertFalse(handle.isSelected)
            assertEquals("false", chip().element.getAttribute("aria-pressed"))
            assertFalse("ts-filter--on" in chip().classes())
            assertTrue(chip().findAll("ts-icon").isEmpty())
        }

        @Test
        fun `should tell only the latest listener`() {
            val handle = buildChip()
            val first = mutableListOf<Boolean>()
            val second = mutableListOf<Boolean>()
            handle.onChange { selected -> first += selected }
            handle.onChange { selected -> second += selected }

            chip()._click()

            assertTrue(first.isEmpty())
            assertEquals(listOf(true), second)
        }

        @Test
        fun `should apply the configuration to the handle`() {
            val changes = mutableListOf<Boolean>()
            buildTestContent { filterChip("Только мои") { onChange { selected -> changes += selected } } }

            chip()._click()

            assertEquals(listOf(true), changes)
        }
    }

    @Nested
    inner class IsSelectedTests {
        @Test
        fun `should show a selection set from code without telling the listener`() {
            val handle = buildChip()
            val changes = mutableListOf<Boolean>()
            handle.onChange { selected -> changes += selected }

            handle.isSelected = true

            assertEquals("true", chip().element.getAttribute("aria-pressed"))
            assertTrue("ts-filter--on" in chip().classes())
            assertEquals(1, chip().findAll("ts-icon").size)
            assertTrue(changes.isEmpty())
        }

        @Test
        fun `should keep a single check icon if the selection is set twice`() {
            val handle = buildChip()
            handle.isSelected = true

            handle.isSelected = true

            assertEquals(1, chip().findAll("ts-icon").size)
        }
    }

    @Nested
    inner class BindSelectedTests {
        @Test
        fun `should show the selection of the signal`() {
            val handle = buildChip()
            val signal = ValueSignal(false)
            handle.bindSelected(signal)

            signal.set(true)

            assertTrue(handle.isSelected)
            assertEquals("true", chip().element.getAttribute("aria-pressed"))
            assertEquals(1, chip().findAll("ts-icon").size)
        }

        @Test
        fun `should tell the listener on a click while bound and keep the selection of the signal`() {
            val handle = buildChip()
            val changes = mutableListOf<Boolean>()
            handle.onChange { selected -> changes += selected }
            handle.bindSelected(ValueSignal(false))

            chip()._click()

            assertEquals(listOf(true), changes)
            assertFalse(handle.isSelected)
            assertEquals("false", chip().element.getAttribute("aria-pressed"))
        }

        @Test
        fun `should show the selection chosen by a click once the signal takes it`() {
            val handle = buildChip()
            val signal = ValueSignal(false)
            handle.bindSelected(signal)
            handle.onChange { selected -> signal.set(selected) }

            chip()._click()

            assertTrue(handle.isSelected)
            assertEquals("true", chip().element.getAttribute("aria-pressed"))
        }

        @Test
        fun `should reject a manual selection while bound`() {
            val handle = buildChip()
            handle.bindSelected(ValueSignal(false))

            assertThrows<BindingActiveException> { handle.isSelected = true }
        }

        @Test
        fun `should reject a second binding`() {
            val handle = buildChip()
            handle.bindSelected(ValueSignal(false))

            assertThrows<BindingActiveException> { handle.bindSelected(ValueSignal(true)) }
        }
    }

    private fun buildChip(isSelected: Boolean = false): FilterChipHandle {
        lateinit var handle: FilterChipHandle
        buildTestContent { handle = filterChip("Только мои", isSelected) }
        return handle
    }

    private fun chip(): NativeButton = UI.getCurrent().find("ts-filter") as NativeButton
}
