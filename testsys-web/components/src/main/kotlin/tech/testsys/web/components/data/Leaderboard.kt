@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.data

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.dom.Element
import tech.testsys.web.components.DataHandle
import tech.testsys.web.components.core.ElementScope
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.setScope
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.Placement

/**
 * Adds an accessible, horizontally scrollable neutral leaderboard of [data].
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.leaderboard(data: LeaderboardData, configure: DataHandle<LeaderboardData>.() -> Unit = {}): DataHandle<LeaderboardData> {
    val root = Div().apply {
        addClassNames("ts-table-scroll", "ts-leaderboard")
        element.setAttribute("tabindex", "0")
    }

    fun render(value: LeaderboardData) {
        root.element.removeAllChildren()
        val table = Element("table").apply { setAttribute("aria-label", value.label) }
        resolveTableLayout(
            value.gridColumns ?: gridColumns,
            listOf(value.placeSize, value.identitySize) + value.columns.map { column -> column.size },
        ).applyTo(table)
        val headings = Element("tr")
        (listOf(value.placeLabel, value.identityLabel) + value.columns.map { column -> column.label }).forEach { caption ->
            headings.appendChild(
                Element("th").apply {
                    text = caption
                    setScope(ElementScope.Col)
                },
            )
        }
        table.appendChild(Element("thead").apply { appendChild(headings) })
        val body = Element("tbody")
        value.rows.forEach { row ->
            val line = Element("tr").apply {
                setAttribute("data-key", row.key)
                if (row.isHighlighted) classList.add("ts-leaderboard__highlight")
            }
            line.appendChild(
                Element("td").apply {
                    text = row.place
                    classList.add("ts-mono")
                },
            )
            line.appendChild(
                Element("th").apply {
                    setScope(ElementScope.Row)
                    appendChild(Element("span").apply { text = row.name })
                    row.description?.let { detail -> appendChild(Element("small").apply { text = detail }) }
                },
            )
            value.columns.forEach { column ->
                val cell = row.cells.getValue(column.key)
                line.appendChild(
                    Element("td").apply {
                        classList.add("ts-lb-cell")
                        classList.add(
                            "ts-lb-cell--${when (cell.state) {
                                LeaderboardCellState.None -> "none"
                                LeaderboardCellState.Success -> "ok"
                                LeaderboardCellState.Error -> "fail"
                                LeaderboardCellState.Pending -> "pending"
                                LeaderboardCellState.HighlightedSuccess -> "first"
                            }}",
                        )
                        if (column.isMetric) classList.add("ts-leaderboard__metric")
                        appendChild(Element("b").apply { text = cell.value })
                        cell.detail?.let { detail -> appendChild(Element("small").apply { text = detail }) }
                    },
                )
            }
            body.appendChild(line)
        }
        table.appendChild(body)
        root.element.appendChild(table)
    }
    render(data)
    add(root)
    return DataHandle(root, data, ::render).apply(configure)
}

/**
 * Adds a leaderboard on [size] columns, or the remaining columns.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.leaderboard(
    data: LeaderboardData,
    size: Int? = null,
    configure: DataHandle<LeaderboardData>.() -> Unit = {},
): DataHandle<LeaderboardData> = placeContent(size, Div()).leaderboard(data, configure)

/**
 * Adds a leaderboard directly to the block body with no inner padding.
 *
 * @since %CURRENT_VERSION%
 */
fun tech.testsys.web.components.layout.BlockScope.leaderboard(
    data: LeaderboardData,
    configure: DataHandle<LeaderboardData>.() -> Unit = {},
): DataHandle<LeaderboardData> {
    val container = Div()
    val handle = ContentScope(container, texts, Placement.Body, columns).leaderboard(data, configure)
    placeWhole(container, "a leaderboard")
    requestFlushBody()
    return handle
}
