package tech.testsys.web.app.view

import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.PageRequest
import tech.testsys.web.components.data.TableHandle
import tech.testsys.web.components.data.TableScope
import tech.testsys.web.components.data.table
import tech.testsys.web.components.feedback.LoadHandle
import tech.testsys.web.components.feedback.load
import tech.testsys.web.components.layout.BlockScope

/** Rows of a table page that a page fetches. */
internal const val TABLE_PAGE_SIZE = 20

/**
 * Table that fills a block body after its first page is loaded in the background.
 *
 * @param T the type of the rows.
 */
internal class LoadedTable<T> {
    internal lateinit var load: LoadHandle

    // The table of the latest successful load, null from the start of a load until it shows a table; refresh may come
    // from any thread.
    @Volatile
    internal var table: TableHandle<T>? = null

    /** Refreshes the loaded table as `TableHandle.refresh` does, or loads the block again while no table is loaded. */
    fun refresh(toFirstPage: Boolean = false) {
        val loaded = table
        if (loaded == null) load.reload() else loaded.refresh(toFirstPage)
    }
}

/**
 * Fills the block body with a table of [pageSize] rows a page whose first page [fetch] returns in the background while
 * the body shows a skeleton; the table fetches the other pages, sorting and refreshes itself. [key] and [content] are
 * as in `table`. The refresh button in the block head loads the whole block again, from the first page and without sorting.
 *
 * @param T the type of the rows.
 */
internal fun <T> BlockScope.loadTable(
    key: (T) -> Any,
    pageSize: Int = TABLE_PAGE_SIZE,
    fetch: (PageRequest) -> Page<T>,
    content: TableScope<T>.() -> Unit,
): LoadedTable<T> {
    val first = PageRequest(offset = 0, limit = pageSize, sort = null)
    val loaded = LoadedTable<T>()
    loaded.load = load({
        loaded.table = null
        fetch(first)
    }) { page ->
        loaded.table = table(key = key, pageSize = pageSize, fetch = servingFirst(first, page, fetch), content = content)
    }
    return loaded
}

/** Returns a table fetch that answers the first request with [loaded] if it asks for [first]; other requests go to [fetch]. */
internal fun <T> servingFirst(first: PageRequest, loaded: Page<T>, fetch: (PageRequest) -> Page<T>): (PageRequest) -> Page<T> {
    var pending: Page<T>? = loaded
    return { request ->
        val ready = pending.takeIf { request == first }
        pending = null
        ready ?: fetch(request)
    }
}
