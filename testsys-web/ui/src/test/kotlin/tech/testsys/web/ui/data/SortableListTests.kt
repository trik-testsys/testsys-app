package tech.testsys.web.ui.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.ui.MockVaadinTests
import tech.testsys.web.ui.buildTestContent
import tech.testsys.web.ui.display.text

class SortableListTests : MockVaadinTests() {
    private data class Item(val key: String, val caption: String)

    @Test
    fun `should accept only exact permutation at current version while retaining item identities`() {
        val first = Item("a", "First")
        val second = Item("b", "Second")
        lateinit var handle: SortableListHandle<Item>
        buildTestContent { handle = sortableList(
                listOf(first, second),
                { item -> item.key },
                { item -> item.caption },
                { item -> text(
                        item.caption,
                    ) },
            ) }
        val adapter = requireNotNull(handle.component as? SortableListAdapter<*>)
        val version = adapter.currentVersion()

        assertTrue(adapter.reorder(listOf("b", "a"), version))

        assertSame(second, handle.items[0])
        assertSame(first, handle.items[1])
        assertFalse(adapter.reorder(listOf("a", "a"), adapter.currentVersion()))
        assertFalse(adapter.reorder(listOf("a", "b"), version))
    }

    @Test
    fun `should reject duplicate programmatic keys and preserve previous order`() {
        lateinit var handle: SortableListHandle<String>
        buildTestContent { handle = sortableList(
                listOf("a", "b"),
                { value -> value },
                { value -> value },
                { value -> text(
                        value,
                    ) },
            ) }

        assertThrows(IllegalArgumentException::class.java) { handle.items = listOf("a", "a") }

        assertEquals(listOf("a", "b"), handle.items)
    }

    @Test
    fun `should replace content data for new instance of same key without firing user callback`() {
        lateinit var handle: SortableListHandle<Item>
        var changes = 0
        buildTestContent { handle = sortableList(
                listOf(Item("a", "Before")),
                { item -> item.key },
                { item -> item.caption },
                { item -> text(
                        item.caption,
                    ) },
            ) { onChange { changes++ } } }
        val fresh = Item("a", "After")

        handle.items = listOf(fresh)

        assertSame(fresh, handle.items.single())
        assertEquals(0, changes)
    }
    @Test
    fun `should request a bound reorder while keeping signal owned order until signal changes`() {
        val source = com.vaadin.flow.signals.local.ValueSignal(listOf("a", "b"))
        lateinit var handle: SortableListHandle<String>
        var requested: List<String>? = null
        buildTestContent {
            handle = sortableList(listOf("a", "b"), { value -> value }, { value -> value }, { value -> text(value) }) {
                bindItems(source)
                onChange { values -> requested = values }
            }
        }
        val adapter = requireNotNull(handle.component as? SortableListAdapter<*>)

        adapter.reorder(listOf("b", "a"), adapter.currentVersion())

        assertEquals(listOf("b", "a"), requested)
        assertEquals(listOf("a", "b"), handle.items)
        source.set(listOf("b", "a"))
        assertEquals(listOf("b", "a"), handle.items)
    }
}
