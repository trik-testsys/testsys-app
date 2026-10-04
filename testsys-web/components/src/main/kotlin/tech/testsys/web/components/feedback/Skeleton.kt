package tech.testsys.web.components.feedback

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.Placement

private const val CIRCLE_SIZE = 28
private const val TITLE_HEIGHT = 10
private const val SUBTITLE_HEIGHT = 8
private const val VALUE_HEIGHT = 10
private const val BADGE_HEIGHT = 22
private const val PUBLIC_TEXT_HEIGHT = 12
private const val TEXT_MIN_WIDTH = 120
private const val RECTANGLE_MIN_WIDTH = 160
private const val RECTANGLE_HEIGHT = 80
private const val BADGE_MIN_WIDTH = 64

/** Widths in percent of the two lines of a skeleton row, repeated by row; the skeleton row pattern. */
private val LINE_WIDTHS = listOf(62 to 40, 80 to 30, 55 to 45, 72 to 25, 66 to 38)

/**
 * Builds the skeleton row markup: [rows] list rows of pulsing shapes, hidden from assistive
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
    width: Int? = null,
    height: Int? = null,
    configure: ElementHandle.() -> Unit = {
    },
): ElementHandle {
    require((width == null || width > 0) && (height == null || height > 0)) { "Skeleton dimensions must be positive" }
    val minimumWidth = width ?: when (shape) {
        SkeletonShape.Text -> TEXT_MIN_WIDTH
        SkeletonShape.Rectangle -> RECTANGLE_MIN_WIDTH
        SkeletonShape.Circle -> CIRCLE_SIZE
        SkeletonShape.Badge -> BADGE_MIN_WIDTH
    }
    val defaultHeight = when (shape) {
        SkeletonShape.Text -> PUBLIC_TEXT_HEIGHT
        SkeletonShape.Rectangle -> RECTANGLE_HEIGHT
        SkeletonShape.Circle -> CIRCLE_SIZE
        SkeletonShape.Badge -> BADGE_HEIGHT
    }
    val modifier = when (shape) {
        SkeletonShape.Circle -> "ts-skel--circle"
        SkeletonShape.Badge -> "ts-skel--badge"
        SkeletonShape.Text, SkeletonShape.Rectangle -> null
    }
    val part = skeleton(height = height ?: defaultHeight, width = "100%", modifier = modifier)
    val root = Div(part).apply {
        element.setAttribute("role", "status")
        element.setAttribute("aria-label", texts.components.loading)
        element.style.set("min-width", "${minimumWidth}px")
        if (width != null || shape == SkeletonShape.Circle || shape == SkeletonShape.Badge) {
            element.style.set("width", "${minimumWidth}px")
        }
    }
    part.element.setAttribute("aria-hidden", true)
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
    width: Int? = null,
    height: Int? = null,
    configure: ElementHandle.() -> Unit = {
    },
): ElementHandle = ContentScope(place(size, Div()), texts, Placement.Body).skeleton(shape, width, height, configure)

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
        element.setAttribute(
            "role",
            "status",
        )
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
    ContentScope(place(size, Div()), texts, Placement.Body).skeletonRows(rows, configure)
