package tech.testsys.web.ui.data

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._fireDomEvent
import com.github.mvysny.kaributesting.v10._setValue
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.html.NativeButton
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.ui.MockVaadinTests
import tech.testsys.web.ui.buildTestPage
import tech.testsys.web.ui.child
import tech.testsys.web.ui.classes
import tech.testsys.web.ui.display.text
import tech.testsys.web.ui.find
import tech.testsys.web.ui.findAll
import tech.testsys.web.ui.testTexts

class TableSelectionTests : MockVaadinTests() {
    @Test
    fun `should add a checkbox column with a header checkbox`() {
        buildTable(Source(size = 12))

        assertTrue(headers()[0].child(0) is Checkbox)
        assertTrue(rows()[0].child(0).child(0) is Checkbox)
        assertEquals(listOf("", "ID", "Участник"), headers().map { header -> header.element.textRecursively })
    }

    @Test
    fun `should leave the checkbox header cell not sortable`() {
        buildTable(Source(size = 12))

        assertFalse("ts-sortable" in headers()[0].classes())
        assertNull(headers()[0].element.getAttribute("tabindex"))
    }

    @Test
    fun `should keep the header checkbox when the sort changes`() {
        buildTable(Source(size = 12))

        headers()[1]._fireDomEvent("click")

        assertTrue(headers()[0].child(0) is Checkbox)
    }

    @Test
    fun `should name the header and row checkboxes for screen readers`() {
        buildTable(Source(size = 12))

        assertEquals(testTexts.table.selectAll, headerCheckbox().ariaLabel.orElseThrow())
        assertEquals(testTexts.table.selectRow, rowCheckbox(0).ariaLabel.orElseThrow())
    }

    @Test
    fun `should disable the header checkbox on a page without rows`() {
        val source = Source(size = 12).apply { isEmpty = true }

        buildTable(source)

        assertFalse(headerCheckbox().isEnabled)
    }

    @Test
    fun `should enable the header checkbox when rows appear`() {
        val source = Source(size = 12).apply { isEmpty = true }
        val handle = buildTable(source)
        source.isEmpty = false

        handle.refresh()

        assertTrue(headerCheckbox().isEnabled)
    }

    @Test
    fun `should select a row by its checkbox`() {
        val handle = buildTable(Source(size = 12))

        rowCheckbox(1)._setValue(true)

        assertEquals(setOf<Any>(2), handle.selected)
        assertTrue("ts-row-selected" in rows()[1].classes())
    }

    @Test
    fun `should unselect a row by its checkbox`() {
        val handle = buildTable(Source(size = 12))
        rowCheckbox(1)._setValue(true)

        rowCheckbox(1)._setValue(false)

        assertEquals(emptySet<Any>(), handle.selected)
        assertFalse("ts-row-selected" in rows()[1].classes())
    }

    @Test
    fun `should keep the selection across pages`() {
        val handle = buildTable(Source(size = 12))
        rowCheckbox(0)._setValue(true)
        pagerButton(testTexts.pagination.next)._click()

        pagerButton(testTexts.pagination.previous)._click()

        assertTrue(rowCheckbox(0).value)
        assertTrue("ts-row-selected" in rows()[0].classes())
        assertEquals(setOf<Any>(1), handle.selected)
    }

    @Test
    fun `should keep the selection after refresh`() {
        val handle = buildTable(Source(size = 12))
        rowCheckbox(0)._setValue(true)

        handle.refresh(toFirstPage = true)

        assertTrue(rowCheckbox(0).value)
        assertEquals(setOf<Any>(1), handle.selected)
    }

    @Test
    fun `should select all rows of the page with the header checkbox`() {
        val handle = buildTable(Source(size = 12))

        headerCheckbox()._setValue(true)

        assertEquals(setOf<Any>(1, 2, 3, 4, 5), handle.selected)
        assertTrue(headerCheckbox().value)
        assertFalse(headerCheckbox().isIndeterminate)
        assertTrue(rowCheckbox(4).value)
        assertTrue("ts-row-selected" in rows()[4].classes())
    }

    @Test
    fun `should unselect only the rows of the page with the header checkbox`() {
        val handle = buildTable(Source(size = 12))
        pagerButton(testTexts.pagination.next)._click()
        rowCheckbox(0)._setValue(true)
        pagerButton(testTexts.pagination.previous)._click()
        headerCheckbox()._setValue(true)

        headerCheckbox()._setValue(false)

        assertEquals(setOf<Any>(6), handle.selected)
        assertFalse(rowCheckbox(0).value)
    }

    @Test
    fun `should show a partly selected page as indeterminate`() {
        buildTable(Source(size = 12))

        rowCheckbox(0)._setValue(true)

        assertTrue(headerCheckbox().isIndeterminate)
        assertFalse(headerCheckbox().value)
    }

    @Test
    fun `should check the header when every row of the page is selected one by one`() {
        buildTable(Source(size = 3))
        rowCheckbox(0)._setValue(true)
        rowCheckbox(1)._setValue(true)

        rowCheckbox(2)._setValue(true)

        assertTrue(headerCheckbox().value)
        assertFalse(headerCheckbox().isIndeterminate)
    }

    @Test
    fun `should show the header state of the current page after a page change`() {
        buildTable(Source(size = 12))
        headerCheckbox()._setValue(true)

        pagerButton(testTexts.pagination.next)._click()

        assertFalse(headerCheckbox().value)
        assertFalse(headerCheckbox().isIndeterminate)
    }

    @Test
    fun `should notify selection changes`() {
        var seen: Set<Any>? = null
        val handle = buildTable(Source(size = 12))
        handle.onSelectionChange { keys -> seen = keys }

        rowCheckbox(2)._setValue(true)

        assertEquals(setOf<Any>(3), seen)
    }

    @Test
    fun `should notify selection changes by the header checkbox`() {
        var seen: Set<Any>? = null
        val handle = buildTable(Source(size = 12))
        handle.onSelectionChange { keys -> seen = keys }

        headerCheckbox()._setValue(true)

        assertEquals(setOf<Any>(1, 2, 3, 4, 5), seen)
    }

    @Test
    fun `should clear the selection without fetching`() {
        val source = Source(size = 12)
        val handle = buildTable(source)
        headerCheckbox()._setValue(true)

        handle.clearSelection()

        assertEquals(emptySet<Any>(), handle.selected)
        assertFalse(rowCheckbox(0).value)
        assertFalse("ts-row-selected" in rows()[0].classes())
        assertFalse(headerCheckbox().value)
        assertEquals(1, source.requests.size)
    }

    @Test
    fun `should notify when the selection is cleared`() {
        var seen: Set<Any>? = null
        val handle = buildTable(Source(size = 12))
        rowCheckbox(0)._setValue(true)
        handle.onSelectionChange { keys -> seen = keys }

        handle.clearSelection()

        assertEquals(emptySet<Any>(), seen)
    }

    @Test
    fun `should start the selection signal with an empty set`() {
        val handle = buildTable(Source(size = 12))

        assertEquals(emptySet<Any>(), handle.selection.peek())
    }

    @Test
    fun `should put the key of a row selected by its checkbox into the selection signal`() {
        val handle = buildTable(Source(size = 12))

        rowCheckbox(1)._setValue(true)

        assertEquals(setOf<Any>(2), handle.selection.peek())
    }

    @Test
    fun `should put the keys of the page selected by the header checkbox into the selection signal`() {
        val handle = buildTable(Source(size = 12))

        headerCheckbox()._setValue(true)

        assertEquals(setOf<Any>(1, 2, 3, 4, 5), handle.selection.peek())
    }

    @Test
    fun `should empty the selection signal when the selection is cleared`() {
        val handle = buildTable(Source(size = 12))
        headerCheckbox()._setValue(true)

        handle.clearSelection()

        assertEquals(emptySet<Any>(), handle.selection.peek())
    }

    @Test
    fun `should update a text in the block actions bound to the selection signal when rows are selected`() {
        val source = Source(size = 12)
        buildTestPage {
            block(title = "Посылки") {
                val handle = table(key = { row -> row.id }, pageSize = 5, selectable = true, fetch = source::fetch) {
                    textColumn("Участник") { row -> row.name }
                }
                actions { text(handle.selection.map { keys -> "Выбрано: ${keys.size}" }) }
            }
        }

        rowCheckbox(0)._setValue(true)
        rowCheckbox(2)._setValue(true)

        assertEquals("Выбрано: 2", ui().find("ts-block__actions").child(0).element.text)
    }

    @Test
    fun `should keep the selection signal empty if the table is not selectable`() {
        lateinit var handle: TableHandle<Row>
        val source = Source(size = 12)
        buildTestPage {
            block(title = "Посылки") {
                handle = table(key = { row -> row.id }, pageSize = 5, fetch = source::fetch) {
                    textColumn("Участник") { row -> row.name }
                }
            }
        }

        handle.refresh()

        assertEquals(emptySet<Any>(), handle.selection.peek())
    }

    @Test
    fun `should not click the row when its checkbox is clicked`() {
        var clicked: Row? = null
        val handle = buildTable(Source(size = 12)) { onRowClick { row -> clicked = row } }

        rowCheckbox(1)._setValue(true)

        assertNull(clicked)
        assertEquals(setOf<Any>(2), handle.selected)
        assertTrue(ROW_CLICK_FILTER.contains("vaadin-checkbox"))
    }

    @Test
    fun `should span the empty text over the checkbox column`() {
        val source = Source(size = 12).apply { isEmpty = true }

        buildTable(source)

        assertEquals("3", rows()[0].child(0).element.getAttribute("colspan"))
    }

    @Test
    fun `should keep a highlighted row marked when its checkbox is unchecked`() {
        val source = Source(size = 12)
        val spec = TableScope<Row>(testTexts).apply { textColumn("Участник") { row -> row.name } }.spec()
        val highlighted = { row: Row -> row.id == 3 }
        val table = DataTable(testTexts, { row -> row.id }, 5, isSelectable = true, source::fetch, spec, highlighted)
        UI.getCurrent().add(table.table)
        rowCheckbox(2)._setValue(true)

        rowCheckbox(2)._setValue(false)

        assertTrue("ts-row-selected" in rows()[2].classes())
        assertEquals(emptySet<Any>(), table.selected)
    }

    private fun buildTable(source: Source, extra: TableScope<Row>.() -> Unit = {}): TableHandle<Row> {
        lateinit var handle: TableHandle<Row>
        buildTestPage {
            block(title = "Посылки") {
                handle = table(key = { row -> row.id }, pageSize = 5, selectable = true, fetch = source::fetch) {
                    codeColumn("ID", sortKey = "id") { row -> row.id.toString() }
                    textColumn("Участник") { row -> row.name }
                    extra()
                }
            }
        }
        return handle
    }

    private fun ui(): Component = UI.getCurrent()

    private fun headers(): List<Component> = ui().find("ts-table").child(0).child(0).children.toList()

    private fun rows(): List<Component> = ui().find("ts-table").child(1).children.toList()

    private fun headerCheckbox(): Checkbox = headers()[0].child(0) as Checkbox

    private fun rowCheckbox(index: Int): Checkbox = rows()[index].child(0).child(0) as Checkbox

    private fun pagerButton(label: String): NativeButton =
        ui().findAll("ts-pager__btn").single { button -> button.element.getAttribute("aria-label") == label } as NativeButton

    private class Row(val id: Int, val name: String)

    /** In-memory rows of the tests; records every request. */
    private class Source(size: Int) {
        val rows = (1..size).map { id -> Row(id, "Участник $id") }
        val requests = mutableListOf<PageRequest>()
        var isEmpty = false

        fun fetch(request: PageRequest): Page<Row> {
            requests += request
            val all = if (isEmpty) emptyList() else rows
            return Page(all.drop(request.offset).take(request.limit), all.size)
        }
    }
}
