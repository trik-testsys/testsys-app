package tech.testsys.web.components.data

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.NativeButton
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.button
import tech.testsys.web.components.classes
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll
import tech.testsys.web.components.testTexts

class TableStatesTests : MockVaadinTests() {
    @Test
    fun `should show the default empty text as an empty state title`() {
        buildTable(Source(size = 0)::fetch)

        assertEquals(testTexts.table.empty, ui().find("ts-empty__title").element.text)
        assertTrue(ui().findAll("ts-empty__desc").isEmpty())
    }

    @Test
    fun `should show the own empty state with a description and an action`() {
        buildTable(Source(size = 0)::fetch) { empty("Посылок нет", description = "Измените фильтр") { action("Сбросить") } }

        assertEquals("Посылок нет", ui().find("ts-empty__title").element.text)
        assertEquals("Измените фильтр", ui().find("ts-empty__desc").element.text)
        assertEquals("sm", button("Сбросить").element.getAttribute("data-ts-size"))
    }

    @Test
    fun `should keep the empty call with a title only`() {
        buildTable(Source(size = 0)::fetch) { empty("Пока пусто") }

        assertEquals("Пока пусто", ui().find("ts-empty__title").element.text)
    }

    @Test
    fun `should show the load failure instead of rows`() {
        buildTable(Source(size = 12, failures = 1)::fetch)

        val state = ui().find("ts-empty")
        assertTrue("ts-empty--error" in state.classes())
        assertEquals(testTexts.load.failed, state.find("ts-empty__title").element.text)
        assertEquals(testTexts.load.failedHint, state.find("ts-empty__desc").element.text)
        assertTrue("ts-row-empty" in state.parent.orElseThrow().parent.orElseThrow().classes())
    }

    @Test
    fun `should show the load failure if cell content throws`() {
        buildTable(Source(size = 12)::fetch) { column("Вердикт", size = 1) { error("Test cell failure") } }

        assertTrue("ts-empty--error" in ui().find("ts-empty").classes())
    }

    @Test
    fun `should not catch an Error from fetch`() {
        assertThrows<NotImplementedError> { buildTable(fetch = { TODO("Test fetch error") }) }
    }

    @Test
    fun `should hide the pagination while the load fails`() {
        val source = Source(size = 12)
        val handle = buildTable(source::fetch)
        source.failures = 1

        handle.refresh()

        assertFalse(ui().find("ts-block__foot").isVisible)
    }

    @Test
    fun `should fetch the rows again on retry`() {
        val source = Source(size = 12, failures = 1)
        buildTable(source::fetch)

        button(testTexts.load.retry)._click()

        assertEquals(5, rows().size)
    }

    @Test
    fun `should show the pagination on retry after the first load failed`() {
        val source = Source(size = 12, failures = 1)
        buildTable(source::fetch)

        button(testTexts.load.retry)._click()

        assertTrue(ui().find("ts-block__foot").isVisible)
        assertTrue(ui().find("ts-table-pager").isVisible)
    }

    @Test
    fun `should retry the page that failed`() {
        val source = Source(size = 12)
        val handle = buildTable(source::fetch)
        (ui().findAll("ts-pager__btn").last() as NativeButton)._click()
        source.failures = 1
        handle.refresh()

        button(testTexts.load.retry)._click()

        assertEquals(5, source.requests.last().offset)
    }

    @Test
    fun `should keep the selection across a failure`() {
        val source = Source(size = 12)
        val handle = buildTable(source::fetch, selectable = true)
        handle.table.selected += 1
        source.failures = 1
        handle.refresh()

        button(testTexts.load.retry)._click()

        assertEquals(setOf<Any>(1), handle.selected)
    }

    @Test
    fun `should keep the load failure when the selection is cleared`() {
        val source = Source(size = 12)
        val handle = buildTable(source::fetch, selectable = true)
        handle.table.selected += 1
        source.failures = 1
        handle.refresh()

        handle.clearSelection()

        val state = ui().find("ts-empty")
        assertTrue("ts-empty--error" in state.classes())
        assertEquals(emptySet<Any>(), handle.selected)
    }

    @Test
    fun `should not report a selection change when the load fails`() {
        val source = Source(size = 12)
        val handle = buildTable(source::fetch, selectable = true)
        val changes = mutableListOf<Set<Any>>()
        handle.onSelectionChange { keys -> changes += keys }
        source.failures = 1

        handle.refresh()

        assertTrue(changes.isEmpty())
    }

    @Test
    fun `should retry the clamped page when it also fails to load`() {
        val source = ShrinkingSource(size = 12)
        val handle = buildTable(source::fetch)
        (ui().findAll("ts-pager__btn").last() as NativeButton)._click()
        (ui().findAll("ts-pager__btn").last() as NativeButton)._click()
        source.rows = source.rows.take(6)
        source.failOffset = 5
        handle.refresh()
        source.requests.clear()

        button(testTexts.load.retry)._click()

        assertEquals(listOf(5), source.requests.map { request -> request.offset })
    }

    private fun buildTable(
        fetch: (PageRequest) -> Page<Row>,
        selectable: Boolean = false,
        extra: TableScope<Row>.() -> Unit = {},
    ): TableHandle<Row> {
        lateinit var handle: TableHandle<Row>
        buildTestPage {
            block(title = "Посылки") {
                handle = table(key = { row -> row.id }, pageSize = 5, selectable = selectable, fetch = fetch) {
                    textColumn("Участник", size = 1) { row -> row.name }
                    extra()
                }
            }
        }
        return handle
    }

    private fun ui(): Component = UI.getCurrent()

    private fun rows(): List<Component> = ui().find("ts-table").child(1).children.toList()

    private fun Component.child(index: Int): Component = children.toList()[index]

    private class Row(val id: Int, val name: String)

    /** In-memory rows; the next [failures] fetches throw. */
    private class Source(size: Int, var failures: Int = 0) {
        private val rows = (1..size).map { id -> Row(id, "Участник $id") }
        val requests = mutableListOf<PageRequest>()

        fun fetch(request: PageRequest): Page<Row> {
            requests += request
            if (failures > 0) {
                failures--
                error("Test fetch failure")
            }
            return Page(rows.drop(request.offset).take(request.limit), rows.size)
        }
    }

    /** In-memory rows that can shrink after being fetched once; the fetch at [failOffset] throws once. */
    private class ShrinkingSource(size: Int) {
        var rows = (1..size).map { id -> Row(id, "Участник $id") }
        var failOffset: Int? = null
        val requests = mutableListOf<PageRequest>()

        fun fetch(request: PageRequest): Page<Row> {
            requests += request
            if (request.offset == failOffset) {
                failOffset = null
                error("Test fetch failure")
            }
            return Page(rows.drop(request.offset).take(request.limit), rows.size)
        }
    }
}
