package tech.testsys.web.components.data

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._fireDomEvent
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.child
import tech.testsys.web.components.classes
import tech.testsys.web.components.display.text
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll
import tech.testsys.web.components.testTexts
import tools.jackson.databind.ObjectMapper

class TableTests : MockVaadinTests() {
    @Test
    fun `should style and invoke only rows allowed by the clickable predicate`() {
        val clicked = mutableListOf<Int>()
        buildTestPage {
            row {
                block {
                    table(key = { value: Int -> value }, fetch = { Page(listOf(1, 2), 2) }) {
                        textColumn("Name") { value -> "Row $value" }
                        onRowClick(isNavigation = true, isClickable = { value -> value == 1 }) { value -> clicked.add(value) }
                    }
                }
            }
        }

        rows()[1]._fireDomEvent("click", rowClick(isOnControl = false))
        rows()[0]._fireDomEvent("click", rowClick(isOnControl = false))

        assertEquals(listOf(1), clicked)
        assertTrue("ts-row-clickable" in rows()[0].classes())
        assertTrue("ts-navigation-cell" in rows()[0].child(0).classes())
        assertFalse("ts-row-clickable" in rows()[1].classes())
        assertFalse("ts-navigation-cell" in rows()[1].child(0).classes())
    }

    @Test
    fun `should request the first page without sort`() {
        val source = Source(size = 12)

        buildTable(source)

        assertEquals(PageRequest(offset = 0, limit = 5, sort = null), source.requests.single())
    }

    @Test
    fun `should render one row per fetched item`() {
        buildTable(Source(size = 12))

        assertEquals(5, rows().size)
        assertEquals(listOf("1", "Участник 1", "10"), cellTexts(rows()[0]))
    }

    @Test
    fun `should mark code and number cells`() {
        buildTable(Source(size = 12))

        assertTrue("ts-num" in rows()[0].child(0).classes())
        assertEquals(setOf("ts-num", "ts-right"), rows()[0].child(2).classes())
    }

    @Test
    fun `should blur the values of an obscured code column until focused`() {
        buildTestPage {
            row {
                block {
                    table(key = { row: Row -> row.id }, fetch = Source(size = 1)::fetch) {
                        codeColumn("Код", size = 1, isObscured = true) { row -> row.name }
                    }
                }
            }
        }

        val value = rows()[0].child(0).child(0)
        assertTrue(value.element.hasAttribute("data-ts-obscured"))
        assertTrue("ts-obscured-value" in value.classes())
        assertEquals("0", value.element.getAttribute("tabindex"))
    }

    @Test
    fun `should make the body flush and not a grid`() {
        buildTable(Source(size = 12))

        assertTrue("ts-block__body--flush" in ui().find("ts-block__body").classes())
        assertFalse("ts-block__body--grid" in ui().find("ts-block__body").classes())
    }

    @Test
    fun `should sort descending on the first header click`() {
        val source = Source(size = 12)
        buildTable(source)

        scoreHeader()._fireDomEvent("click")

        assertEquals(PageRequest(offset = 0, limit = 5, sort = Sort("score", isDescending = true)), source.requests.last())
        assertTrue("ts-sorted" in scoreHeader().classes())
        assertTrue(scoreHeader().element.textRecursively.endsWith("↓"))
    }

    @Test
    fun `should toggle sort direction on a second click`() {
        val source = Source(size = 12)
        buildTable(source)
        scoreHeader()._fireDomEvent("click")

        scoreHeader()._fireDomEvent("click")

        assertEquals(Sort("score", isDescending = false), source.requests.last().sort)
        assertTrue(scoreHeader().element.textRecursively.endsWith("↑"))
    }

    @Test
    fun `should sort descending by another column on its first click`() {
        val source = Source(size = 12)
        buildTable(source)
        scoreHeader()._fireDomEvent("click")

        idHeader()._fireDomEvent("click")

        assertEquals(Sort("id", isDescending = true), source.requests.last().sort)
        assertTrue("ts-sorted" in idHeader().classes())
        assertTrue(idHeader().element.textRecursively.endsWith("↓"))
        assertFalse("ts-sorted" in scoreHeader().classes())
        assertEquals("Баллы", scoreHeader().element.textRecursively)
    }

    @Test
    fun `should make sortable headers focusable with no sort order`() {
        buildTable(Source(size = 12))

        assertEquals("0", scoreHeader().element.getAttribute("tabindex"))
        assertEquals("none", scoreHeader().element.getAttribute("aria-sort"))
    }

    @Test
    fun `should announce the sort order of the sorted header only`() {
        buildTable(Source(size = 12))
        scoreHeader()._fireDomEvent("click")

        idHeader()._fireDomEvent("click")
        idHeader()._fireDomEvent("click")

        assertEquals("ascending", idHeader().element.getAttribute("aria-sort"))
        assertEquals("none", scoreHeader().element.getAttribute("aria-sort"))
    }

    @Test
    fun `should announce descending order after the first click`() {
        buildTable(Source(size = 12))

        scoreHeader()._fireDomEvent("click")

        assertEquals("descending", scoreHeader().element.getAttribute("aria-sort"))
    }

    @Test
    fun `should sort when Enter or Space is pressed on a header`() {
        val source = Source(size = 12)
        buildTable(source)

        scoreHeader()._fireDomEvent("keydown", sortKeyPress(isSortKey = true))

        assertEquals(Sort("score", isDescending = true), source.requests.last().sort)
    }

    @Test
    fun `should not sort when another key is pressed on a header`() {
        val source = Source(size = 12)
        buildTable(source)

        scoreHeader()._fireDomEvent("keydown", sortKeyPress(isSortKey = false))

        assertNull(source.requests.last().sort)
    }

    @Test
    fun `should leave headers without sort key plain`() {
        buildTable(Source(size = 12))

        assertFalse("ts-sortable" in nameHeader().classes())
        assertNull(nameHeader().element.getAttribute("tabindex"))
        assertNull(nameHeader().element.getAttribute("aria-sort"))
    }

    @Test
    fun `should align the number header right`() {
        buildTable(Source(size = 12))

        assertTrue("ts-right" in scoreHeader().classes())
        assertFalse("ts-right" in nameHeader().classes())
    }

    @Test
    fun `should resolve explicit fractions and the remaining fraction in declaration order`() {
        buildTable(Source(size = 3)) { textColumn("Комментарий") { row -> row.name } }

        val cols = _get<com.vaadin.flow.component.html.Table>().element.getChild(0).children.toList()
        assertEquals(
            listOf("4.166666666666666%", "4.166666666666666%", "4.166666666666666%", "87.5%"),
            cols.map { col -> col.style.get("width") },
        )
    }

    @Test
    fun `should go back to the first page when the sort changes`() {
        val source = Source(size = 12)
        buildTable(source)
        pagerButton(testTexts.pagination.next)._click()

        scoreHeader()._fireDomEvent("click")

        assertEquals(0, source.requests.last().offset)
    }

    @Test
    fun `should show the range and the page counter`() {
        buildTable(Source(size = 12))

        assertEquals(testTexts.table.range(1, 5, 12), ui().find("ts-table-pager").child(0).element.textRecursively)
        assertEquals("1 / 3", ui().find("ts-pager__label").element.textRecursively)
    }

    @Test
    fun `should group the digits of a large page count`() {
        buildTable(Source(size = 7_060))

        assertEquals("1 / 1 412", ui().find("ts-pager__label").element.textRecursively)
    }

    @Test
    fun `should go to the next page`() {
        val source = Source(size = 12)
        buildTable(source)

        pagerButton(testTexts.pagination.next)._click()

        assertEquals(5, source.requests.last().offset)
        assertEquals("2 / 3", ui().find("ts-pager__label").element.textRecursively)
    }

    @Test
    fun `should go to the previous page`() {
        val source = Source(size = 12)
        buildTable(source)
        pagerButton(testTexts.pagination.next)._click()

        pagerButton(testTexts.pagination.previous)._click()

        assertEquals(0, source.requests.last().offset)
    }

    @Test
    fun `should disable previous on the first page`() {
        buildTable(Source(size = 12))

        assertFalse(pagerButton(testTexts.pagination.previous).isEnabled)
        assertTrue(pagerButton(testTexts.pagination.next).isEnabled)
    }

    @Test
    fun `should disable next on the last page`() {
        buildTable(Source(size = 12))
        pagerButton(testTexts.pagination.next)._click()

        pagerButton(testTexts.pagination.next)._click()

        assertTrue(pagerButton(testTexts.pagination.previous).isEnabled)
        assertFalse(pagerButton(testTexts.pagination.next).isEnabled)
    }

    @Test
    fun `should hide pagination and the footer when rows fit one page`() {
        buildTable(Source(size = 3))

        assertFalse(ui().find("ts-block__foot").isVisible)
    }

    @Test
    fun `should show pagination when a refresh brings more rows than one page`() {
        val source = Source(size = 3)
        val handle = buildTable(source)
        source.rows = (1..12).map { id -> Row(id, "Участник $id", id * 10) }

        handle.refresh()

        assertTrue(ui().find("ts-block__foot").isVisible)
        assertTrue(ui().find("ts-table-pager").isVisible)
    }

    @Test
    fun `should keep the footer when it has own content`() {
        val source = Source(size = 3)

        buildTestPage {
            row {
                block(title = "Посылки") {
                    footer { text("Итого") }
                    table(key = { row: Row -> row.id }, pageSize = 5, fetch = source::fetch) {
                        textColumn("Участник", size = 1) { row -> row.name }
                    }
                }
            }
        }

        assertTrue(ui().find("ts-block__foot").isVisible)
        assertFalse(ui().find("ts-table-pager").isVisible)
    }

    @Test
    fun `should put the pagination after own footer content`() {
        val source = Source(size = 12)

        buildTestPage {
            row {
                block(title = "Посылки") {
                    footer { text("Итого") }
                    table(key = { row: Row -> row.id }, pageSize = 5, fetch = source::fetch) {
                        textColumn("Участник", size = 1) { row -> row.name }
                    }
                }
            }
        }

        assertTrue("ts-table-pager" in ui().find("ts-block__foot").child(1).classes())
        assertTrue(ui().find("ts-table-pager").isVisible)
    }

    @Test
    fun `should put the pagination after own footer content declared after the table`() {
        val source = Source(size = 12)

        buildTestPage {
            row {
                block(title = "Посылки") {
                    table(key = { row: Row -> row.id }, pageSize = 5, fetch = source::fetch) {
                        textColumn("Участник", size = 1) { row -> row.name }
                    }
                    footer { text("Итого") }
                }
            }
        }

        assertEquals("Итого", ui().find("ts-block__foot").child(0).element.textRecursively)
        assertTrue("ts-table-pager" in ui().find("ts-block__foot").child(1).classes())
    }

    @Test
    fun `should refresh the current page`() {
        val source = Source(size = 12)
        val handle = buildTable(source)
        pagerButton(testTexts.pagination.next)._click()

        handle.refresh()

        assertEquals(5, source.requests.last().offset)
    }

    @Test
    fun `should refresh to the first page`() {
        val source = Source(size = 12)
        val handle = buildTable(source)
        pagerButton(testTexts.pagination.next)._click()

        handle.refresh(toFirstPage = true)

        assertEquals(0, source.requests.last().offset)
    }

    @Test
    fun `should move to the last page with rows when the total shrinks`() {
        val source = Source(size = 12)
        val handle = buildTable(source)
        pagerButton(testTexts.pagination.next)._click()
        pagerButton(testTexts.pagination.next)._click()
        source.rows = source.rows.take(6)

        handle.refresh()

        assertEquals(5, source.requests.last().offset)
        assertEquals("2 / 2", ui().find("ts-pager__label").element.textRecursively)
    }

    @Test
    fun `should show the default empty text across all columns if there are no rows`() {
        val source = Source(size = 12).apply { isEmpty = true }

        buildTable(source)

        assertEquals(1, rows().size)
        assertEquals("3", rows()[0].child(0).element.getAttribute("colspan"))
        assertEquals(testTexts.table.empty, rows()[0].element.textRecursively)
    }

    @Test
    fun `should mark the empty row so that it has no hover`() {
        val source = Source(size = 12).apply { isEmpty = true }

        buildTable(source)

        assertTrue("ts-row-empty" in rows()[0].classes())
    }

    @Test
    fun `should show the given empty text if there are no rows`() {
        val source = Source(size = 12).apply { isEmpty = true }

        buildTable(source) { empty("Посылок нет") }

        assertEquals("Посылок нет", rows()[0].element.textRecursively)
    }

    @Test
    fun `should run row click with the clicked row`() {
        var clicked: Row? = null
        buildTable(Source(size = 12)) { onRowClick { row -> clicked = row } }

        rows()[1]._fireDomEvent("click", rowClick(isOnControl = false))

        assertEquals(2, clicked?.id)
        assertTrue("ts-row-clickable" in rows()[1].classes())
    }

    @Test
    fun `should not run row click when a control inside the row is clicked`() {
        var clicked: Row? = null
        buildTable(Source(size = 12)) { onRowClick { row -> clicked = row } }

        rows()[1]._fireDomEvent("click", rowClick(isOnControl = true))

        assertNull(clicked)
    }

    @Test
    fun `should mark the first text cell if the row click navigates`() {
        buildTable(Source(size = 12)) {
            menuColumn { item("Открыть") {} }
            onRowClick(isNavigation = true) {}
        }

        val cells = rows()[0].children.toList()

        assertEquals(listOf(false, true, false, false), cells.map { cell -> "ts-navigation-cell" in cell.classes() })
    }

    @Test
    fun `should not mark a cell if the row click does not navigate`() {
        buildTable(Source(size = 12)) { onRowClick {} }

        val cells = rows()[0].children.toList()

        assertTrue(cells.none { cell -> "ts-navigation-cell" in cell.classes() })
    }

    @Test
    fun `should reject rows and a table in one block`() {
        val source = Source(size = 3)

        assertThrows<IllegalStateException> {
            buildTestPage {
                row {
                    block {
                        row { text("…") }
                        table(key = { row: Row -> row.id }, fetch = source::fetch) { textColumn("Участник", size = 1) { row -> row.name } }
                    }
                }
            }
        }
    }

    @Test
    fun `should reject rows after a table in one block`() {
        val source = Source(size = 3)

        assertThrows<IllegalStateException> {
            buildTestPage {
                row {
                    block {
                        table(key = { row: Row -> row.id }, fetch = source::fetch) { textColumn("Участник", size = 1) { row -> row.name } }
                        row { text("…") }
                    }
                }
            }
        }
    }

    @Test
    fun `should reject a second table`() {
        val source = Source(size = 3)

        assertThrows<IllegalStateException> {
            buildTestPage {
                row {
                    block {
                        table(key = { row: Row -> row.id }, fetch = source::fetch) { textColumn("Участник", size = 1) { row -> row.name } }
                        table(key = { row: Row -> row.id }, fetch = source::fetch) { textColumn("Участник", size = 1) { row -> row.name } }
                    }
                }
            }
        }
    }

    @Test
    fun `should hide the table and its pagination through the handle`() {
        val handle = buildTable(Source(size = 12))

        handle.isVisible = false

        assertFalse(ui().find("ts-table-scroll").isVisible)
        assertFalse(ui().find("ts-block__foot").isVisible)
    }

    @Test
    fun `should hide the table and its pagination from the visible signal at once`() {
        val handle = buildTable(Source(size = 12))

        handle.bindVisible(ValueSignal(false))

        assertFalse(ui().find("ts-table-scroll").isVisible)
        assertFalse(ui().find("ts-block__foot").isVisible)
    }

    @Test
    fun `should follow the visible signal while attached`() {
        val handle = buildTable(Source(size = 12))
        val signal = ValueSignal(true)
        handle.bindVisible(signal)

        signal.set(false)

        assertFalse(ui().find("ts-table-scroll").isVisible)
    }

    @Test
    fun `should restore the table viewport and keep pagination outside it`() {
        val handle = buildTable(Source(size = 12))
        handle.isVisible = false

        handle.isVisible = true

        val viewport = ui().find("ts-table-scroll")
        assertTrue(viewport.isVisible)
        assertEquals(listOf(ui().find("ts-table")), viewport.children.toList())
        assertTrue(ui().find("ts-block__foot").isVisible)
        assertTrue(ui().find("ts-block__foot").find("ts-table-pager").isVisible)
    }

    @Test
    fun `should reject a manual visible value while bound`() {
        val handle = buildTable(Source(size = 12))
        handle.bindVisible(ValueSignal(true))

        assertThrows<BindingActiveException> { handle.isVisible = false }
    }

    @Test
    fun `should reject a second visible binding`() {
        val handle = buildTable(Source(size = 12))
        handle.bindVisible(ValueSignal(true))

        assertThrows<BindingActiveException> { handle.bindVisible(ValueSignal(false)) }
    }

    @Test
    fun `should reject a table without columns`() {
        val source = Source(size = 3)

        assertThrows<IllegalArgumentException> {
            buildTestPage { row { block { table(key = { row: Row -> row.id }, fetch = source::fetch) { empty("Посылок нет") } } } }
        }
    }

    @Test
    fun `should reject a page size below one`() {
        assertThrows<IllegalArgumentException> { buildTable(Source(size = 3), pageSize = 0) }
    }

    private fun buildTable(source: Source, pageSize: Int = 5, extra: TableScope<Row>.() -> Unit = {}): TableHandle<Row> {
        lateinit var handle: TableHandle<Row>
        buildTestPage {
            row {
                block(title = "Посылки") {
                    handle = table(key = { row -> row.id }, pageSize = pageSize, fetch = source::fetch) {
                        codeColumn("ID", sortKey = "id", size = 1) { row -> row.id.toString() }
                        textColumn("Участник", size = 1) { row -> row.name }
                        numberColumn("Баллы", sortKey = "score", size = 1) { row -> row.score }
                        extra()
                    }
                }
            }
        }
        return handle
    }

    private fun ui(): Component = UI.getCurrent()

    /** Rows of the table body. */
    private fun rows(): List<Component> = ui().find("ts-table").child(1).children.toList()

    private fun cellTexts(row: Component): List<String> = row.children.map { cell -> cell.element.textRecursively }.toList()

    private fun headers(): List<Component> = ui().find("ts-table").child(0).child(0).children.toList()

    private fun idHeader(): Component = headers()[0]

    private fun nameHeader(): Component = headers()[1]

    private fun scoreHeader(): Component = headers()[2]

    private fun pagerButton(label: String): NativeButton =
        ui().findAll("ts-pager__btn").single { button -> button.element.getAttribute("aria-label") == label } as NativeButton

    /** Data of a row click as the client sends it: whether the click passed the filter of clicks on controls. */
    private fun rowClick(isOnControl: Boolean) = ObjectMapper().createObjectNode().put(ROW_CLICK_FILTER, !isOnControl)

    /** Data of a key press on a header as the client sends it: whether the key passed the filter of sort keys. */
    private fun sortKeyPress(isSortKey: Boolean) = ObjectMapper().createObjectNode().put(SORT_KEY_FILTER, isSortKey)

    private class Row(val id: Int, val name: String, val score: Int)

    /** In-memory rows of the tests; records every request. Scores grow with ids, so both sort keys give one order. */
    private class Source(size: Int) {
        var rows = (1..size).map { id -> Row(id, "Участник $id", id * 10) }
        val requests = mutableListOf<PageRequest>()
        var isEmpty = false

        fun fetch(request: PageRequest): Page<Row> {
            requests += request
            val all = if (isEmpty) emptyList() else sorted(request.sort)
            return Page(all.drop(request.offset).take(request.limit), all.size)
        }

        private fun sorted(sort: Sort?): List<Row> = when {
            sort == null -> rows
            sort.isDescending -> rows.sortedByDescending { row -> row.score }
            else -> rows.sortedBy { row -> row.score }
        }
    }
}
