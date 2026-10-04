package tech.testsys.web.components.data

/**
 * A named application column of a leaderboard.
 *
 * @property key the stable unique column key.
 * @property label the accessible heading.
 * @property isMetric whether this is an aggregate metric column.
 * @property size the grid fractions, or `null` to take the remainder as the last column.
 * @since %CURRENT_VERSION%
 */
data class LeaderboardColumn(val key: String, val label: String, val isMetric: Boolean = false, val size: Int? = 1)

/**
 * Semantic appearance of an application-provided leaderboard cell.
 *
 * @since %CURRENT_VERSION%
 */
enum class LeaderboardCellState { None, Success, Error, Pending, HighlightedSuccess }

/**
 * An application-provided cell; the UI derives no grading results.
 *
 * @property value the primary caption.
 * @property detail the optional secondary caption.
 * @property state the semantic appearance.
 * @since %CURRENT_VERSION%
 */
data class LeaderboardCell(
    val value: String,
    val detail: String? = null,
    val state: LeaderboardCellState = LeaderboardCellState.None,
)

/**
 * An application-provided row with stable identity and explicit highlighting.
 *
 * @property key the stable unique row key.
 * @property place the rank caption provided by the application.
 * @property name the participant or team caption.
 * @property description the optional affiliation caption.
 * @property cells the cells keyed by column key.
 * @property isHighlighted whether the row is highlighted.
 * @since %CURRENT_VERSION%
 */
data class LeaderboardRow(
    val key: String,
    val place: String,
    val name: String,
    val description: String? = null,
    val cells: Map<String, LeaderboardCell>,
    val isHighlighted: Boolean = false,
)

/**
 * A neutral leaderboard without scoring or contest-format calculations.
 *
 * @property label the accessible table name.
 * @property placeLabel the place column heading.
 * @property identityLabel the identity column heading.
 * @property columns named cell and metric columns.
 * @property rows the ordered application rows.
 * @property placeSize the fractions of the rank column.
 * @property identitySize the fractions of the participant column.
 * @property gridColumns the logical capacity, or `null` to inherit the containing grid.
 * @since %CURRENT_VERSION%
 */
data class LeaderboardData(
    val label: String,
    val placeLabel: String,
    val identityLabel: String,
    val columns: List<LeaderboardColumn>,
    val rows: List<LeaderboardRow>,
    val placeSize: Int = 2,
    val identitySize: Int = 6,
    val gridColumns: Int? = null,
) {
    init {
        require(placeSize > 0 && identitySize > 0) { "Leaderboard identity sizes must be positive" }
        require(gridColumns == null || gridColumns > 0) { "Leaderboard grid capacity must be positive, got $gridColumns" }
        require(columns.map { column -> column.key }.distinct().size == columns.size) { "Leaderboard column keys must be unique" }
        require(rows.map { row -> row.key }.distinct().size == rows.size) { "Leaderboard row keys must be unique" }
        val keys = columns.map { column -> column.key }.toSet()
        require(rows.all { row -> row.cells.keys == keys }) { "Every leaderboard row must provide exactly its named columns" }
    }
}
