package tech.testsys.domain.contract.persistence

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

data class Sort(val orders: List<Order>) {

    data class Order(
        val field: String,
        val direction: Direction = Direction.ASC,
    )

    enum class Direction {
        ASC,
        DESC,
    }

    companion object {

        val UNSORTED = Sort(emptyList())
    }
}

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
