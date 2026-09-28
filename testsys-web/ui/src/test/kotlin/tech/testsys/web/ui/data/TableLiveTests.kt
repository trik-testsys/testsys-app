package tech.testsys.web.ui.data

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._fireDomEvent
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.NativeButton
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import tech.testsys.web.ui.MockVaadinTests
import tech.testsys.web.ui.button
import tech.testsys.web.ui.buildTestPage
import tech.testsys.web.ui.child
import tech.testsys.web.ui.classes
import tech.testsys.web.ui.find
import tech.testsys.web.ui.findAll
import tech.testsys.web.ui.testTexts
import kotlin.concurrent.thread

class TableLiveTests : MockVaadinTests() {
    /** Root of the page built by [buildTable]. */
    private lateinit var page: Component
    @Nested
    inner class BackgroundRefreshTests {
        @Test
        fun `should show new rows after a refresh from another thread`() {
            val source = Source(size = 3)
            val handle = buildTable(source)
            source.add(Row(4, 40))

            thread { handle.refresh() }.join()
            MockVaadin.clientRoundtrip()

            assertEquals(listOf("1", "2", "3", "4"), shownIds())
        }

        @Test
        fun `should not reload rows until the UI runs the queued refresh`() {
            val source = Source(size = 3)
            val handle = buildTable(source)
            source.add(Row(4, 40))

            thread { handle.refresh() }.join()

            assertEquals(listOf("1", "2", "3"), shownIds())
        }

        @Test
        fun `should keep the selection sort and page after a refresh from another thread`() {
            val source = Source(size = 12)
            val handle = buildTable(source, selectable = true)
            scoreHeader()._fireDomEvent("click")
            nextPage()
            handle.table.selected += 7

            thread { handle.refresh() }.join()
            MockVaadin.clientRoundtrip()

            assertEquals(PageRequest(offset = 5, limit = 5, sort = Sort("score", isDescending = true)), source.requests.last())
            assertEquals(setOf<Any>(7), handle.selected)
        }

        @Test
        fun `should skip the refresh if the UI was detached`() {
            val source = Source(size = 3)
            val handle = buildTable(source)
            val ui = UI.getCurrent()
            ui.session.removeUI(ui)

            assertDoesNotThrow { handle.refresh() }
            assertEquals(1, source.requests.size)
        }

        @Test
        fun `should skip the queued refresh if the UI is detached before it runs`() {
            val source = Source(size = 3)
            val handle = buildTable(source)
            thread { handle.refresh() }.join()
            val ui = UI.getCurrent()
            val session = ui.session
            session.removeUI(ui)

            assertDoesNotThrow { session.service.runPendingAccessTasks(session) }
            assertEquals(1, source.requests.size)
        }
    }

    @Nested
    inner class LeftPageTests {
        @Test
        fun `should not fetch on a refresh from another thread after the page was left`() {
            val source = Source(size = 3)
            val handle = buildTable(source)
            leave()

            thread { handle.refresh() }.join()

            assertDoesNotThrow { MockVaadin.clientRoundtrip() }
            assertEquals(1, source.requests.size)
        }

        @Test
        fun `should not fetch on a refresh in the UI thread after the page was left`() {
            val source = Source(size = 3)
            val handle = buildTable(source)
            leave()

            handle.refresh()

            assertEquals(1, source.requests.size)
        }

        @Test
        fun `should not fetch on a queued refresh if the page was left before it runs`() {
            val source = Source(size = 3)
            val handle = buildTable(source)
            thread { handle.refresh() }.join()
            leave()

            MockVaadin.clientRoundtrip()

            assertEquals(1, source.requests.size)
        }

        @Test
        fun `should refresh when the page is shown again after a skipped refresh`() {
            val source = Source(size = 3)
            val handle = buildTable(source)
            leave()
            source.add(Row(4, 40))
            handle.refresh()

            comeBack()

            assertEquals(listOf("1", "2", "3", "4"), shownIds())
            assertEquals(listOf("4"), newIds())
        }

        @Test
        fun `should refresh to the first page when the page is shown again after a skipped refresh to it`() {
            val source = Source(size = 12)
            val handle = buildTable(source)
            nextPage()
            leave()
            handle.refresh(toFirstPage = true)
            handle.refresh()

            comeBack()

            assertEquals(listOf("1", "2", "3", "4", "5"), shownIds())
            assertTrue(newIds().isEmpty())
        }

        @Test
        fun `should not fetch when the page is shown again without skipped refreshes`() {
            val source = Source(size = 3)
            buildTable(source)
            leave()

            comeBack()

            assertEquals(1, source.requests.size)
        }

        @Test
        fun `should refresh after the page is shown again`() {
            val source = Source(size = 3)
            val handle = buildTable(source)
            leave()
            comeBack()
            source.add(Row(4, 40))

            thread { handle.refresh() }.join()
            MockVaadin.clientRoundtrip()

            assertEquals(listOf("1", "2", "3", "4"), shownIds())
        }
    }

    @Nested
    inner class HighlightTests {
        @Test
        fun `should highlight only rows with new keys after a refresh`() {
            val source = Source(size = 3)
            val handle = buildTable(source)
            source.add(Row(4, 40))

            handle.refresh()

            assertEquals(listOf("4"), newIds())
        }

        @Test
        fun `should highlight a new row that replaced another one on the page`() {
            val source = Source(size = 5)
            val handle = buildTable(source)
            source.addFirst(Row(6, 60))

            handle.refresh()

            assertEquals(listOf("6"), newIds())
        }

        @Test
        fun `should not highlight rows again on a second refresh without changes`() {
            val source = Source(size = 3)
            val handle = buildTable(source)
            source.add(Row(4, 40))
            handle.refresh()

            handle.refresh()

            assertTrue(newIds().isEmpty())
        }

        @Test
        fun `should highlight new rows after a refresh from another thread`() {
            val source = Source(size = 3)
            val handle = buildTable(source)
            source.add(Row(4, 40))

            thread { handle.refresh() }.join()
            MockVaadin.clientRoundtrip()

            assertEquals(listOf("4"), newIds())
        }

        @Test
        fun `should highlight rows of a page that was empty`() {
            val source = Source(size = 0)
            val handle = buildTable(source)
            source.add(Row(1, 10))

            handle.refresh()

            assertEquals(listOf("1"), newIds())
        }

        @Test
        fun `should not highlight rows on the first load`() {
            buildTable(Source(size = 3))

            assertTrue(newIds().isEmpty())
        }

        @Test
        fun `should not highlight rows on a refresh to the first page`() {
            val source = Source(size = 3)
            val handle = buildTable(source)
            source.add(Row(4, 40))

            handle.refresh(toFirstPage = true)

            assertTrue(newIds().isEmpty())
        }

        @Test
        fun `should not highlight rows on a page change`() {
            buildTable(Source(size = 12))

            nextPage()

            assertTrue(newIds().isEmpty())
        }

        @Test
        fun `should not highlight rows on a sort change`() {
            buildTable(Source(size = 12))

            scoreHeader()._fireDomEvent("click")

            assertTrue(newIds().isEmpty())
        }

        @Test
        fun `should not highlight rows on retry after a load failure`() {
            val source = Source(size = 3)
            val handle = buildTable(source)
            source.failures = 1
            handle.refresh()
            source.add(Row(4, 40))

            button(testTexts.load.retry)._click()

            assertTrue(newIds().isEmpty())
        }

        @Test
        fun `should compare with the last shown rows on a refresh after a load failure`() {
            val source = Source(size = 3)
            val handle = buildTable(source)
            source.failures = 1
            handle.refresh()
            source.add(Row(4, 40))

            handle.refresh()

            assertEquals(listOf("4"), newIds())
        }

        @Test
        fun `should not highlight rows if the refresh moved to the last page with rows`() {
            val source = Source(size = 12)
            val handle = buildTable(source)
            nextPage()
            nextPage()
            source.removeFrom(10)

            handle.refresh()

            assertEquals(listOf("6", "7", "8", "9", "10"), shownIds())
            assertTrue(newIds().isEmpty())
        }
    }

    private fun buildTable(source: Source, selectable: Boolean = false): TableHandle<Row> {
        lateinit var handle: TableHandle<Row>
        val main = buildTestPage {
            block(title = "Посылки") {
                handle = table(key = { row -> row.id }, pageSize = 5, selectable = selectable, fetch = source::fetch) {
                    codeColumn("ID") { row -> row.id.toString() }
                    numberColumn("Баллы", sortKey = "score") { row -> row.score }
                }
            }
        }
        page = main.parent.orElseThrow()
        return handle
    }

    /** Takes the page built by [buildTable] out of the UI, as navigation to another route does. */
    private fun leave() {
        UI.getCurrent().remove(page)
    }

    /** Puts the page taken out by [leave] back into the UI, as navigation back to a kept view does. */
    private fun comeBack() {
        UI.getCurrent().add(page)
    }

    private fun ui(): Component = UI.getCurrent()

    private fun rows(): List<Component> = ui().find("ts-table").child(1).children.toList()

    private fun idOf(row: Component): String = row.child(row.children.count().toInt() - 2).element.textRecursively

    private fun shownIds(): List<String> = rows().map(::idOf)

    private fun newIds(): List<String> = rows().filter { row -> "ts-row-new" in row.classes() }.map(::idOf)

    private fun scoreHeader(): Component = ui().findAll("ts-sortable").single()

    private fun nextPage() {
        (ui().findAll("ts-pager__btn").last() as NativeButton)._click()
    }

    private class Row(val id: Int, val score: Int)

    /** Rows kept in memory that tests change between fetches; the next [failures] fetches throw. */
    private class Source(size: Int, var failures: Int = 0) {
        private val rows = (1..size).map { id -> Row(id, id * 10) }.toMutableList()
        val requests = mutableListOf<PageRequest>()

        fun add(row: Row) {
            rows += row
        }

        fun addFirst(row: Row) {
            rows.add(0, row)
        }

        fun removeFrom(index: Int) {
            rows.subList(index, rows.size).clear()
        }

        fun fetch(request: PageRequest): Page<Row> {
            requests += request
            if (failures > 0) {
                failures--
                error("Test fetch failure")
            }
            return Page(rows.drop(request.offset).take(request.limit), rows.size)
        }
    }
}
