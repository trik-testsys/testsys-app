package tech.testsys.web.components.data

/**
 * Request of one page of table rows.
 *
 * @property offset the index of the first row.
 * @property limit the most rows to return.
 * @property sort the order, or `null` for the natural order of the source.
 * @since %CURRENT_VERSION%
 */
data class PageRequest(val offset: Int, val limit: Int, val sort: Sort?)

/**
 * Order of table rows by the sort key of a column.
 *
 * @property key the sort key given to the column.
 * @property isDescending whether larger values come first.
 * @since %CURRENT_VERSION%
 */
data class Sort(val key: String, val isDescending: Boolean)

/**
 * One page of table rows and the number of rows in all pages.
 *
 * @param T the type of the rows.
 * @property rows the rows of the page.
 * @property total the number of rows in all pages.
 * @since %CURRENT_VERSION%
 */
data class Page<T>(val rows: List<T>, val total: Int)
