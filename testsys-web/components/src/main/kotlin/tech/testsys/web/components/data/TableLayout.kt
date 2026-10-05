@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.data

import com.vaadin.flow.dom.Element
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.CssUnit
import tech.testsys.web.components.core.HtmlTag
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.add
import tech.testsys.web.components.core.htmlElement
import tech.testsys.web.components.core.setTableUsed
import tech.testsys.web.components.core.setTableWidth
import tech.testsys.web.components.core.setWidth
import tech.testsys.web.components.layout.GridTrack

/** Resolved column fractions of one logical grid, including utility columns. */
internal class TableLayout(val gridColumns: Int, val sizes: List<Int>) {
    fun applyTo(table: Element) {
        val used = sizes.sum()
        table.classList.add(CssClass.TableGrid)
        table.style.setTableUsed(used)
        table.style.setTableWidth(used.toDouble() / gridColumns * FULL_PERCENT)
        val group = htmlElement(HtmlTag.ColGroup)
        sizes.forEach { size ->
            group.appendChild(htmlElement(HtmlTag.Col).apply { style.setWidth(size.toDouble() / used * FULL_PERCENT, CssUnit.Percent) })
        }
        table.insertChild(0, group)
    }
    private companion object {
        const val FULL_PERCENT = 100
    }
}

/** Reserves utility fractions before resolving an ordinary column that takes the remainder. */
internal fun resolveTableLayout(gridColumns: Int, sizes: List<Int?>, selectionSize: Int = 0, menuSize: Int = 0): TableLayout {
    require(gridColumns > 0) { "Table grid capacity must be positive, got $gridColumns" }
    require(selectionSize >= 0 && menuSize >= 0) { "Table utility sizes must be nonnegative, got $selectionSize and $menuSize" }
    val track = GridTrack(gridColumns, owner = "Table grid")
    if (selectionSize > 0) track.take(selectionSize)
    if (menuSize > 0) track.take(menuSize)
    val resolved = sizes.map { size -> if (size == null) track.takeRest() else size.also { track.take(size) } }
    return TableLayout(gridColumns, listOfNotNull(selectionSize.takeIf { it > 0 }) + resolved + listOfNotNull(menuSize.takeIf { it > 0 }))
}
