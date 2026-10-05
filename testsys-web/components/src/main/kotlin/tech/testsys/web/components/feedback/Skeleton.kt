@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.feedback

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.core.ElementRole
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.setAriaHidden
import tech.testsys.web.components.core.setRole
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope

private const val CIRCLE_SIZE = 28
private const val TITLE_HEIGHT = 10
private const val SUBTITLE_HEIGHT = 8
private const val VALUE_HEIGHT = 10
private const val BADGE_HEIGHT = 22

/** Widths in percent of the two lines of a skeleton row, repeated by row; the skeleton row pattern. */
private val LINE_WIDTHS = listOf(62 to 40, 80 to 30, 55 to 45, 72 to 25, 66 to 38)

/**
 * Builds the skeleton row markup: [rows] list rows of pulsing shapes, hidden from assistive
 * technologies.
 */
internal fun buildSkeletonRows(rows: Int): Div = Div().apply {
    element.setAriaHidden(true)
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

/**
 * Semantic shapes available to a public loading placeholder.
 *
 * @since %CURRENT_VERSION%
 */
enum class SkeletonShape { Text, Circle, Badge, Rectangle }

/**
 * Adds an accessible loading placeholder with [shape].
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.skeleton(
    shape: SkeletonShape = SkeletonShape.Text,
    configure: ElementHandle.() -> Unit = {
    },
): ElementHandle {
    val part = Span().apply {
        addClassNames("ts-skel", "ts-skel--${shape.name.lowercase()}")
        element.setAriaHidden(true)
    }
    val root = Div(part).apply {
        addClassNames("ts-skeleton", "ts-skeleton--${shape.name.lowercase()}")
        element.setRole(ElementRole.Status)
        element.setAttribute("aria-label", texts.components.loading)
    }
    add(root)
    return ElementHandle(root).apply(configure)
}

/**
 * Adds a loading placeholder on [size] columns, or the remaining columns.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.skeleton(
    shape: SkeletonShape = SkeletonShape.Text,
    size: Int? = null,
    configure: ElementHandle.() -> Unit = {
    },
): ElementHandle = placeContent(size, Div()).skeleton(shape, configure)

/**
 * Adds [rows] loading rows, using one accessible status for the decorative shapes.
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.skeletonRows(rows: Int = 5, configure: ElementHandle.() -> Unit = {}): ElementHandle {
    require(rows > 0) { "Skeleton row count must be positive, got $rows" }
    val root = Div(
        buildSkeletonRows(
            rows,
        ),
    ).apply {
        element.setRole(ElementRole.Status)
        element.setAttribute(
            "aria-label",
            texts.components.loading,
        )
    }
    add(root)
    return ElementHandle(root).apply(configure)
}

/**
 * Adds loading rows on [size] columns, or the remaining columns.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.skeletonRows(rows: Int = 5, size: Int? = null, configure: ElementHandle.() -> Unit = {}): ElementHandle =
    placeContent(size, Div()).skeletonRows(rows, configure)
