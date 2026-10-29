package tech.testsys.web.components.navigation

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.classes
import tech.testsys.web.components.display.CounterKind
import tech.testsys.web.components.display.text
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll

private const val UPDATED_COUNT = 7
private const val DEFAULT_KIND_COUNT = 5

class ChoiceGroupTests : MockVaadinTests() {
    private enum class Filter { All, Accepted, Failed }

    @Test
    fun `should put tabs in place of the title of a block without one`() {
        buildTabs(title = null)

        val head = ui().find("ts-block__head")
        assertEquals(head.find("ts-block__titles"), ui().find("ts-tabs").parent.orElseThrow())
        assertFalse("ts-block__head--tabs" in head.classes())
        assertNull(ui().find("ts-tabs").element.getAttribute("aria-label"))
    }

    @Test
    fun `should put tabs on a second line of the head of a titled block`() {
        buildTabs(title = "Посылки")

        val head = ui().find("ts-block__head")
        assertTrue("ts-block__head--tabs" in head.classes())
        assertEquals(ui().find("ts-tabs"), head.children.toList().last())
        assertEquals("Посылки", ui().find("ts-tabs").element.getAttribute("aria-label"))
    }

    @Test
    fun `should mark the initial tab as pressed`() {
        buildTabs(initial = Filter.Accepted)

        assertEquals(listOf("false", "true", "false"), tabs().map { tab -> tab.element.getAttribute("aria-pressed") })
        assertTrue("ts-tab--active" in tabs()[1].classes())
        assertEquals("group", ui().find("ts-tabs").element.getAttribute("role"))
    }

    @Test
    fun `should change the value and tell the listener on a click`() {
        val handle = buildTabs()
        val chosen = mutableListOf<Filter>()
        handle.onChange { value -> chosen += value }

        tabs()[2]._click()

        assertEquals(Filter.Failed, handle.value)
        assertEquals(listOf(Filter.Failed), chosen)
        assertTrue("ts-tab--active" in tabs()[2].classes())
        assertFalse("ts-tab--active" in tabs()[0].classes())
    }

    @Test
    fun `should not tell the listener on a click on the chosen tab`() {
        val handle = buildTabs()
        val chosen = mutableListOf<Filter>()
        handle.onChange { value -> chosen += value }

        tabs()[0]._click()

        assertTrue(chosen.isEmpty())
    }

    @Test
    fun `should not tell the listener when the page sets the value`() {
        val handle = buildTabs()
        val chosen = mutableListOf<Filter>()
        handle.onChange { value -> chosen += value }

        handle.value = Filter.Failed

        assertTrue(chosen.isEmpty())
        assertEquals("true", tabs()[2].element.getAttribute("aria-pressed"))
    }

    @Test
    fun `should reject a value that is not among the tabs`() {
        val handle = buildTabs(values = listOf(Filter.All, Filter.Accepted))

        assertThrows<IllegalArgumentException> { handle.value = Filter.Failed }
    }

    @Test
    fun `should show counters above zero only`() {
        buildTabs(counts = mapOf(Filter.Accepted to 0, Filter.Failed to 3), countKind = CounterKind.Attention)

        val counter = tabs()[2].find("ts-counter")
        assertTrue(counter.isVisible)
        assertEquals("3", counter.element.text)
        assertTrue("ts-counter--danger" in counter.classes())
        assertFalse(tabs()[1].find("ts-counter").isVisible)
    }

    @Test
    fun `should render the counter with the default kind if none is given`() {
        buildTestPage {
            row {
                block {
                    tabs(initial = 1) {
                        tab(1, "Один")
                        tab(2, "Два", count = DEFAULT_KIND_COUNT)
                    }
                }
            }
        }

        assertTrue("ts-counter--muted" in tabs()[1].find("ts-counter").classes())
    }

    @Test
    fun `should change a counter through the handle`() {
        val handle =
            buildTabs(counts = mapOf(Filter.Accepted to 0, Filter.Failed to 3), countKind = CounterKind.Attention)

        handle.setCount(Filter.Accepted, UPDATED_COUNT)
        handle.setCount(Filter.Failed, null)

        assertEquals(UPDATED_COUNT.toString(), tabs()[1].find("ts-counter").element.text)
        assertTrue(tabs()[1].find("ts-counter").isVisible)
        assertFalse(tabs()[2].find("ts-counter").isVisible)
    }

    @Test
    fun `should reject a negative count through the handle`() {
        val handle = buildTabs()

        assertThrows<IllegalArgumentException> { handle.setCount(Filter.Accepted, -1) }
    }

    @Test
    fun `should reject a count for a value that is not among the tabs`() {
        val handle = buildTabs(values = listOf(Filter.All, Filter.Accepted))

        assertThrows<IllegalArgumentException> { handle.setCount(Filter.Failed, 1) }
    }

    @Test
    fun `should hide the tabs through the handle`() {
        val handle = buildTabs()

        handle.isVisible = false

        assertFalse(ui().find("ts-tabs").isVisible)
    }

    @Test
    fun `should reject a single tab`() {
        assertThrows<IllegalArgumentException> { buildTabs(values = listOf(Filter.All)) }
    }

    @Test
    fun `should reject repeated tab values`() {
        assertThrows<IllegalArgumentException> { buildTabs(values = listOf(Filter.All, Filter.All)) }
    }

    @Test
    fun `should reject an initial value that is not among the tabs`() {
        assertThrows<IllegalArgumentException> { buildTabs(initial = Filter.Failed, values = listOf(Filter.All, Filter.Accepted)) }
    }

    @Test
    fun `should reject a negative count`() {
        assertThrows<IllegalArgumentException> {
            buildTestPage {
                row {
                    block {
                        tabs(initial = 1) {
                            tab(1, "Один", count = -1)
                            tab(2, "Два")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should reject a second tab group in a block`() {
        assertThrows<IllegalStateException> {
            buildTestPage {
                row {
                    block {
                        tabs(initial = 1) {
                            tab(1, "Один")
                            tab(2, "Два")
                        }
                        tabs(initial = 1) {
                            tab(1, "Один")
                            tab(2, "Два")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should put pills in the block head actions`() {
        buildTestPage {
            row {
                block(title = "Каталог") {
                    actions {
                        pills(initial = 1) {
                            pill(1, "Все")
                            pill(2, "Квизы")
                        }
                    }
                }
            }
        }

        val pills = ui().findAll("ts-pill")
        assertEquals(ui().find("ts-block__actions"), ui().find("ts-pills").parent.orElseThrow())
        assertTrue("ts-pill--active" in pills[0].classes())
        assertTrue(ui().findAll("ts-counter").isEmpty())
    }

    @Test
    fun `should change the value on a pill click`() {
        lateinit var handle: ChoiceHandle<Int>
        buildTestPage {
            row {
                block(title = "Каталог") {
                    actions {
                        handle = pills(initial = 1) {
                            pill(1, "Все")
                            pill(2, "Квизы")
                        }
                    }
                }
            }
        }

        (ui().findAll("ts-pill")[1] as NativeButton)._click()

        assertEquals(2, handle.value)
    }

    @Test
    fun `should place pills on the columns of a block row`() {
        buildTestPage {
            row {
                block {
                    row {
                        pills(initial = 1, size = 12) {
                            pill(1, "Все")
                            pill(2, "Квизы")
                        }
                        text("x")
                    }
                }
            }
        }

        assertEquals("span 12", ui().find("ts-pills").element.style.get("grid-column"))
    }

    @Nested
    inner class BindCountTests {
        @Test
        fun `should show the counter of the signal`() {
            val handle = buildTabs()
            val signal = ValueSignal<Int?>(3)
            handle.bindCount(Filter.Accepted, signal)

            signal.set(UPDATED_COUNT)

            assertEquals(UPDATED_COUNT.toString(), tabs()[1].find("ts-counter").element.text)
            assertTrue(tabs()[1].find("ts-counter").isVisible)
        }

        @Test
        fun `should hide the counter when the signal has no count`() {
            val handle = buildTabs(counts = mapOf(Filter.Accepted to 3))
            val signal = ValueSignal<Int?>(3)
            handle.bindCount(Filter.Accepted, signal)

            signal.set(null)

            assertFalse(tabs()[1].find("ts-counter").isVisible)
        }

        @Test
        fun `should hide the counter when the signal has zero`() {
            val handle = buildTabs(counts = mapOf(Filter.Accepted to 3))
            val signal = ValueSignal<Int?>(3)
            handle.bindCount(Filter.Accepted, signal)

            signal.set(0)

            assertFalse(tabs()[1].find("ts-counter").isVisible)
        }

        @Test
        fun `should reject a value that is not among the tabs`() {
            val handle = buildTabs(values = listOf(Filter.All, Filter.Accepted))

            assertThrows<IllegalArgumentException> { handle.bindCount(Filter.Failed, ValueSignal<Int?>(1)) }
        }

        @Test
        fun `should reject a manual count of the bound tab`() {
            val handle = buildTabs()
            handle.bindCount(Filter.Accepted, ValueSignal<Int?>(1))

            assertThrows<BindingActiveException> { handle.setCount(Filter.Accepted, 2) }
        }

        @Test
        fun `should reject a second binding of a tab`() {
            val handle = buildTabs()
            handle.bindCount(Filter.Accepted, ValueSignal<Int?>(1))

            assertThrows<BindingActiveException> { handle.bindCount(Filter.Accepted, ValueSignal<Int?>(2)) }
        }

        @Test
        fun `should let the page set the count of another tab while one is bound`() {
            val handle = buildTabs()
            handle.bindCount(Filter.Accepted, ValueSignal<Int?>(1))

            handle.setCount(Filter.Failed, UPDATED_COUNT)

            assertEquals(UPDATED_COUNT.toString(), tabs()[2].find("ts-counter").element.text)
        }
    }

    private fun buildTabs(
        title: String? = null,
        initial: Filter = Filter.All,
        values: List<Filter> = Filter.entries,
        counts: Map<Filter, Int?> = emptyMap(),
        countKind: CounterKind = CounterKind.Neutral,
    ): TabsHandle<Filter> {
        lateinit var handle: TabsHandle<Filter>
        buildTestPage {
            row {
                block(title = title) {
                    handle = tabs(initial) {
                        values.forEach { value -> tab(value, value.name, count = counts[value], countKind = countKind) }
                    }
                    row { text("Содержимое") }
                }
            }
        }
        return handle
    }

    private fun ui(): Component = UI.getCurrent()

    private fun tabs(): List<NativeButton> = ui().findAll("ts-tab").map { tab -> tab as NativeButton }
}
