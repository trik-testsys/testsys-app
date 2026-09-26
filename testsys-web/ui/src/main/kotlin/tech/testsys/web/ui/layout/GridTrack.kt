package tech.testsys.web.ui.layout

/** Number of columns of the page grid. */
internal const val GRID_COLUMNS: Int = 24

/**
 * Columns taken by the elements of one row; rejects a size out of `1..capacity`, a row wider than [capacity]
 * and any element after the one that took the rest of the row.
 */
internal class GridTrack(private val capacity: Int, private val owner: String) {
    private val sizes = mutableListOf<Int>()
    private var isClosed = false

    fun take(size: Int) {
        checkOpen()
        require(size in 1..capacity) { "$owner accepts sizes in 1..$capacity columns, got $size" }
        val taken = sizes + size
        val total = taken.sum()
        check(total <= capacity) {
            "$owner overflow: sizes ${taken.joinToString("+")} = $total exceed $capacity columns"
        }
        sizes += size
    }

    /** Takes all columns left in the row, closes the row and returns how many columns it took. */
    fun takeRest(): Int {
        checkOpen()
        val rest = capacity - sizes.sum()
        check(rest > 0) { "$owner is full: sizes ${sizes.joinToString("+")} leave no columns for an element without a size" }
        sizes += rest
        isClosed = true
        return rest
    }

    private fun checkOpen() {
        check(!isClosed) { "$owner is full: an element without a size already takes the rest of the row" }
    }
}
