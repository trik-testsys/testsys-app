package tech.testsys.web.ui.feedback

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span

private const val CIRCLE_SIZE = 28
private const val TITLE_HEIGHT = 10
private const val SUBTITLE_HEIGHT = 8
private const val VALUE_HEIGHT = 10
private const val BADGE_HEIGHT = 22

/** Widths in percent of the two lines of a skeleton row, repeated by row; the table of the reference `SkeletonRows`. */
private val LINE_WIDTHS = listOf(62 to 40, 80 to 30, 55 to 45, 72 to 25, 66 to 38)

/**
 * Builds the markup of the reference `SkeletonRows`: [rows] list rows of pulsing shapes, hidden from assistive
 * technologies.
 */
internal fun buildSkeletonRows(rows: Int): Div = Div().apply {
    element.setAttribute("aria-hidden", "true")
    repeat(rows) { index -> add(skeletonRow(LINE_WIDTHS[index % LINE_WIDTHS.size])) }
}

private fun skeletonRow(lineWidths: Pair<Int, Int>): Div {
    val lines = Div(
        skeleton(height = TITLE_HEIGHT, width = "${lineWidths.first}%"),
        skeleton(height = SUBTITLE_HEIGHT, width = "${lineWidths.second}%"),
    ).apply { addClassName("ts-skel-row__lines") }
    return Div(
        skeleton(height = CIRCLE_SIZE, width = "${CIRCLE_SIZE}px", modifier = "ts-skel--circle"),
        lines,
        skeleton(height = VALUE_HEIGHT),
        skeleton(height = BADGE_HEIGHT, modifier = "ts-skel--badge"),
    ).apply { addClassNames("ts-list-row", "ts-skel-row") }
}

/** A pulsing shape [height] pixels high, [width] wide or as wide as its place, with an optional shape [modifier]. */
private fun skeleton(height: Int, width: String? = null, modifier: String? = null): Span = Span().apply {
    addClassName("ts-skel")
    modifier?.let { name -> addClassName(name) }
    width?.let { value -> style.set("width", value) }
    style.set("height", "${height}px")
}
