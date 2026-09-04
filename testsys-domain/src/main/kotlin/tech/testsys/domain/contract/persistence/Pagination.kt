package tech.testsys.domain.contract.persistence

/**
 * A request for one page of a larger result set. Throws [IllegalArgumentException] if [page] is negative
 * or [size] is not positive.
 *
 * @property page the zero-based index of the requested page.
 * @property size the maximum number of elements on the page.
 * @property sort the ordering to apply before slicing; [Sort.UNSORTED] by default.
 * @since %CURRENT_VERSION%
 */
data class Pagination(
    val page: Int,
    val size: Int,
    val sort: Sort = Sort.UNSORTED,
) {

    init {
        require(page >= 0) { "page must be >= 0" }
        require(size > 0) { "size must be > 0" }
    }
}

/**
 * An ordering of a result set. Orders are applied in list order: the first is primary, the following break ties.
 * [UNSORTED] is the empty ordering: results come in the storage's natural order.
 *
 * @property orders the ordering criteria, most significant first.
 * @since %CURRENT_VERSION%
 */
data class Sort(val orders: List<Order>) {

    /**
     * A single ordering criterion.
     *
     * @property field the name of the field to order by, as understood by the persistence adapter.
     * @property direction the sort direction; [Direction.ASC] by default.
     * @since %CURRENT_VERSION%
     */
    data class Order(
        val field: String,
        val direction: Direction = Direction.ASC,
    )

    /**
     * Sort direction.
     *
     * @since %CURRENT_VERSION%
     */
    enum class Direction {
        ASC,
        DESC,
    }

    companion object {
        val UNSORTED = Sort(emptyList())
    }
}

/**
 * One page of a result set.
 *
 * @param T the type of the elements on the page.
 * @property content the elements of the page; may be shorter than [Pagination.size] on the last page.
 * @property pagination the request this page answers.
 * @property totalElements the total number of elements across all pages.
 * @property totalPages the total number of pages; `0` when there are no elements.
 * @property hasNext whether there is at least one element after this page.
 * @since %CURRENT_VERSION%
 */
data class Page<T>(
    val content: List<T>,
    val pagination: Pagination,
    val totalElements: Long,
) {

    val totalPages: Int
        get() = if (pagination.size == 0) 0 else ((totalElements + pagination.size - 1) / pagination.size).toInt()

    val hasNext: Boolean
        get() = (pagination.page + 1) * pagination.size < totalElements
}
