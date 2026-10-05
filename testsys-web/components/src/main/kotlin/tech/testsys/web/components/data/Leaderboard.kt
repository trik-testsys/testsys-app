@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.data

import com.vaadin.flow.component.html.Div
import tech.testsys.web.components.DataHandle
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.ElementScope
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.HtmlTag
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.add
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.htmlElement
import tech.testsys.web.components.core.setAttribute
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
        addClassNames(CssClass.TableScroll, CssClass.Leaderboard)
        element.setAttribute(HtmlAttribute.TabIndex, "0")
    }

    fun render(value: LeaderboardData) {
        root.element.removeAllChildren()
        val table = htmlElement(HtmlTag.Table).apply { setAttribute(HtmlAttribute.AriaLabel, value.label) }
        resolveTableLayout(
            value.gridColumns ?: gridColumns,
            listOf(value.placeSize, value.identitySize) + value.columns.map { column -> column.size },
        ).applyTo(table)
        val headings = htmlElement(HtmlTag.Tr)
        (listOf(value.placeLabel, value.identityLabel) + value.columns.map { column -> column.label }).forEach { caption ->
            headings.appendChild(
                htmlElement(HtmlTag.Th).apply {
                    text = caption
                    setScope(ElementScope.Col)
                },
            )
        }
        table.appendChild(htmlElement(HtmlTag.Thead).apply { appendChild(headings) })
        val body = htmlElement(HtmlTag.Tbody)
        value.rows.forEach { row ->
            val line = htmlElement(HtmlTag.Tr).apply {
                setAttribute(HtmlAttribute.DataKey, row.key)
                if (row.isHighlighted) classList.add(CssClass.LeaderboardHighlight)
            }
            line.appendChild(
                htmlElement(HtmlTag.Td).apply {
                    text = row.place
                    classList.add(CssClass.Mono)
                },
            )
            line.appendChild(
                htmlElement(HtmlTag.Th).apply {
                    setScope(ElementScope.Row)
                    appendChild(htmlElement(HtmlTag.Span).apply { text = row.name })
                    row.description?.let { detail -> appendChild(htmlElement(HtmlTag.Small).apply { text = detail }) }
                },
            )
            value.columns.forEach { column ->
                val cell = row.cells.getValue(column.key)
                line.appendChild(
                    htmlElement(HtmlTag.Td).apply {
                        classList.add(CssClass.LbCell)
                        classList.add(
                            when (cell.state) {
                                LeaderboardCellState.None -> CssClass.LbCellNone
                                LeaderboardCellState.Success -> CssClass.LbCellOk
                                LeaderboardCellState.Error -> CssClass.LbCellFail
                                LeaderboardCellState.Pending -> CssClass.LbCellPending
                                LeaderboardCellState.HighlightedSuccess -> CssClass.LbCellFirst
                            },
                        )
                        if (column.isMetric) classList.add(CssClass.LeaderboardMetric)
                        appendChild(htmlElement(HtmlTag.B).apply { text = cell.value })
                        cell.detail?.let { detail -> appendChild(htmlElement(HtmlTag.Small).apply { text = detail }) }
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
