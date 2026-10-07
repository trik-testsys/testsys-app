package tech.testsys.web.components.data

import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.display.text

class SortableListTests : MockVaadinTests() {
    private data class Item(val key: String, val caption: String)

    private val first = Item("a", "First")
    private val second = Item("b", "Second")
    private var changes = 0
    private val rendered = mutableListOf<String>()

    @Test
    fun `should accept an exact permutation at the current version and retain item identities`() {
        val handle = buildItems(listOf(first, second))
        val adapter = adapter(handle)

        val isAccepted = adapter.reorder(listOf("b", "a"), adapter.currentVersion())

        assertTrue(isAccepted)
        assertSame(second, handle.items[0])
        assertSame(first, handle.items[1])
    }

    @Test
    fun `should reject a reorder that is not a permutation of the keys`() {
        val handle = buildItems(listOf(first, second))
        val adapter = adapter(handle)

        val isAccepted = adapter.reorder(listOf("a", "a"), adapter.currentVersion())

        assertFalse(isAccepted)
        assertEquals(listOf(first, second), handle.items)
    }

    @Test
    fun `should reject a reorder of an outdated version`() {
        val handle = buildItems(listOf(first, second))
        val adapter = adapter(handle)
        val outdated = adapter.currentVersion()
        adapter.reorder(listOf("b", "a"), outdated)

        val isAccepted = adapter.reorder(listOf("a", "b"), outdated)

        assertFalse(isAccepted)
        assertEquals(listOf(second, first), handle.items)
    }

    @Test
    fun `should reject a reorder while disabled`() {
        val handle = buildItems(listOf(first, second))
        handle.isEnabled = false
        val adapter = adapter(handle)

        val isAccepted = adapter.reorder(listOf("b", "a"), adapter.currentVersion())

        assertFalse(isAccepted)
        assertEquals(0, changes)
    }

    @Test
    fun `should cancel a started reorder on detach`() {
        val handle = buildItems(listOf(first, second))
        val adapter = adapter(handle)
        val started = adapter.currentVersion()
        handle.component.element.removeFromParent()

        val isAccepted = adapter.reorder(listOf("b", "a"), started)

        assertFalse(isAccepted)
    }

    @Test
    fun `should reject duplicate programmatic keys and preserve previous order`() {
        val handle = buildItems(listOf(first, second))

        assertThrows(IllegalArgumentException::class.java) { handle.items = listOf(first, first) }

        assertEquals(listOf(first, second), handle.items)
    }

    @Test
    fun `should reject an empty key`() {
        assertThrows(IllegalArgumentException::class.java) { buildItems(listOf(Item("", "Empty"))) }
    }

    @Test
    fun `should replace content data for new instance of same key without firing user callback`() {
        val handle = buildItems(listOf(Item("a", "Before")))
        val fresh = Item("a", "After")

        handle.items = listOf(fresh)

        assertSame(fresh, handle.items.single())
        assertEquals(listOf("Before", "After"), rendered)
        assertEquals(0, changes)
    }

    @Test
    fun `should request a bound reorder while keeping the signal owned order`() {
        val source = ValueSignal(listOf(first, second))
        var requested: List<Item>? = null
        val handle = buildItems(listOf(first, second)) {
            bindItems(source)
            onChange { values -> requested = values }
        }
        val adapter = adapter(handle)

        adapter.reorder(listOf("b", "a"), adapter.currentVersion())

        assertEquals(listOf(second, first), requested)
        assertEquals(listOf(first, second), handle.items)
    }

    @Test
    fun `should show the order accepted by the bound signal`() {
        val source = ValueSignal(listOf(first, second))
        val handle = buildItems(listOf(first, second)) { bindItems(source) }

        source.set(listOf(second, first))

        assertEquals(listOf(second, first), handle.items)
    }

    private fun buildItems(items: List<Item>, configure: SortableListHandle<Item>.() -> Unit = {}): SortableListHandle<Item> {
        lateinit var handle: SortableListHandle<Item>
        buildTestContent {
            handle = sortableList(
                items,
                itemKey = { item -> item.key },
                itemLabel = { item -> item.caption },
                content = { item -> text(item.caption.also(rendered::add)) },
            ) {
                onChange { changes++ }
                configure()
            }
        }
        return handle
    }

    private fun adapter(handle: SortableListHandle<Item>): SortableListAdapter<*> =
        requireNotNull(handle.component as? SortableListAdapter<*>)
}
