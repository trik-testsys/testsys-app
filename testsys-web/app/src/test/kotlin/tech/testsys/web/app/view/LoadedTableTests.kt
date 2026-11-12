package tech.testsys.web.app.view

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.PageRequest
import tech.testsys.web.components.data.Sort

class LoadedTableTests {
    private val first = PageRequest(offset = 0, limit = 20, sort = null)
    private val loaded = Page(listOf("loaded"), total = 40)
    private val requests = mutableListOf<PageRequest>()
    private val fetch: (PageRequest) -> Page<String> = { request ->
        requests += request
        Page(listOf("fetched"), total = 40)
    }

    @Test
    fun `should answer the first request of the first page with the loaded page without fetching`() {
        val tableFetch = servingFirst(first, loaded, fetch)

        val page = tableFetch(first)

        assertSame(loaded, page)
        assertEquals(emptyList<PageRequest>(), requests)
    }

    @Test
    fun `should fetch the first page again when the table asks for it again`() {
        val tableFetch = servingFirst(first, loaded, fetch)
        tableFetch(first)

        val page = tableFetch(first)

        assertEquals(listOf("fetched"), page.rows)
        assertEquals(listOf(first), requests)
    }

    @Test
    fun `should fetch the next page`() {
        val next = PageRequest(offset = 20, limit = 20, sort = null)
        val tableFetch = servingFirst(first, loaded, fetch)
        tableFetch(first)

        val page = tableFetch(next)

        assertEquals(listOf("fetched"), page.rows)
        assertEquals(listOf(next), requests)
    }

    @Test
    fun `should drop the loaded page if the first request asks for another one`() {
        val sorted = PageRequest(offset = 0, limit = 20, sort = Sort(key = "name", isDescending = true))
        val tableFetch = servingFirst(first, loaded, fetch)
        tableFetch(sorted)

        val page = tableFetch(first)

        assertEquals(listOf("fetched"), page.rows)
        assertEquals(listOf(sorted, first), requests)
    }
}
