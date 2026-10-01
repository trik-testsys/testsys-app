package tech.testsys.web.components.navigation

import com.vaadin.flow.component.UI
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.testsys.web.components.Background
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll
import tech.testsys.web.components.testTexts
import java.util.concurrent.Executor
import kotlin.concurrent.thread

class HeaderSearchTests : MockVaadinTests() {
    private val pending = mutableListOf<Runnable>()
    private val destination = HeaderDestination.Action {}

    @BeforeEach
    fun useQueue() {
        Background.executorOverride = Executor { work -> pending.add(work) }
    }

    @AfterEach
    fun restoreExecutor() {
        Background.executorOverride = null
    }

    @Test
    fun `should keep native popup target separate from the combobox accessible controls`() {
        val search = search { emptyList() }

        search.inputChanged("query")

        assertSame(search.component, search.popup.target)
        assertEquals("listbox", search.field.element.getAttribute("aria-haspopup"))
        assertEquals(search.component.find("ts-header-results").id.orElseThrow(), search.field.element.getAttribute("aria-controls"))
    }

    @Test
    fun `should discard an older answer arriving after the latest query`() {
        val search = search { query -> listOf(result(query)) }
        search.inputChanged(" old ")
        search.fetchPending("old")
        search.inputChanged("new")
        search.fetchPending("new")
        val old = pending.removeAt(0)
        pending.removeAt(0).run()

        old.run()

        assertEquals("new", search.component.findAll("ts-header-result").single().element.textRecursively)
        assertTrue(search.popup.isOpened)
    }

    @Test
    fun `should invalidate the previous answer during the next query debounce`() {
        val search = search { query -> listOf(result(query)) }
        search.inputChanged("old")
        search.fetchPending("old")
        search.inputChanged("new")

        pending.single().run()

        assertEquals(testTexts.header.searchLoading, search.status.text)
        assertTrue(search.component.findAll("ts-header-result").isEmpty())
        assertEquals("true", search.field.element.getAttribute("aria-busy"))
    }

    @Test
    fun `should restore provider interruption on the worker instead of the UI thread`() {
        val search = search { throw InterruptedException("Provider interrupted") }
        search.inputChanged("query")
        search.fetchPending("query")

        val worker = thread { pending.single().run() }
        worker.join()

        assertTrue(worker.isInterrupted)
        assertFalse(Thread.currentThread().isInterrupted)
    }

    @Test
    fun `should ignore pending answers after clearing the query`() {
        val search = search { query -> listOf(result(query)) }
        search.inputChanged("query")
        search.fetchPending("query")
        search.inputChanged("   ")

        pending.single().run()

        assertFalse(search.popup.isOpened)
        assertEquals("false", search.field.element.getAttribute("aria-expanded"))
        assertEquals("false", search.field.element.getAttribute("aria-busy"))
        assertTrue(search.component.findAll("ts-header-result").isEmpty())
    }

    @Test
    fun `should ignore pending answers after closing the popup`() {
        val search = search { query -> listOf(result(query)) }
        search.inputChanged("query")
        search.fetchPending("query")
        search.close()

        pending.single().run()

        assertFalse(search.popup.isOpened)
    }

    @Test
    fun `should ignore pending answers after detach`() {
        val search = search { query -> listOf(result(query)) }
        search.inputChanged("query")
        search.fetchPending("query")
        UI.getCurrent().remove(search.component)

        pending.single().run()

        assertFalse(search.popup.isOpened)
    }

    @Test
    fun `should show an error and retry the current query`() {
        var attempts = 0
        val search = search { query ->
            if (++attempts == 1) error("Provider rejected request")
            listOf(result(query))
        }
        search.inputChanged("query")
        search.fetchPending("query")
        pending.removeAt(0).run()
        assertEquals(testTexts.header.searchFailed, search.status.text)

        search.retry()
        pending.removeAt(0).run()

        assertEquals(2, attempts)
        assertEquals("query", search.component.findAll("ts-header-result").single().element.textRecursively)
    }

    @Test
    fun `should select with arrows and open only the chosen result`() {
        val opened = mutableListOf<String>()
        val search = search { listOf(result("first", opened), result("second", opened)) }
        search.inputChanged("query")
        search.fetchPending("query")
        pending.single().run()
        search.key("Enter")
        assertTrue(opened.isEmpty())
        search.key("ArrowDown")
        assertEquals("true", search.component.findAll("ts-header-result").first().element.getAttribute("aria-selected"))
        assertEquals("listbox", search.field.element.getAttribute("aria-haspopup"))
        search.key("ArrowDown")

        search.key("Enter")

        assertEquals(listOf("second"), opened)
        assertFalse(search.popup.isOpened)
    }

    @Test
    fun `should show the empty state without selecting an absent result`() {
        val search = search { emptyList() }
        search.inputChanged("query")
        search.fetchPending("query")

        pending.single().run()
        search.key("ArrowUp")
        search.key("Enter")

        assertEquals(testTexts.header.searchEmpty, search.status.text)
        assertTrue(search.popup.isOpened)
    }

    private fun search(fetch: (String) -> List<HeaderSearchResult>): HeaderSearchController =
        HeaderSearchController(HeaderSearch(fetch), testTexts.header, HeaderInteractions()).also { controller ->
            UI.getCurrent().add(controller.component)
        }

    private fun result(label: String, opened: MutableList<String>? = null): HeaderSearchResult = HeaderSearchResult(
        key = label,
        label = label,
        destination = if (opened == null) destination else HeaderDestination.Action { opened.add(label) },
    )
}
