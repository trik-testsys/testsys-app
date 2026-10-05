@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.feedback

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.CssUnit
import tech.testsys.web.components.core.ElementRole
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.setAriaHidden
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setHeightPixels
import tech.testsys.web.components.core.setRole
import tech.testsys.web.components.core.setWidth
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
        skeleton(height = TITLE_HEIGHT, width = lineWidths.first),
        skeleton(height = SUBTITLE_HEIGHT, width = lineWidths.second),
    ).apply { addClassName(CssClass.SkelRowLines) }
    return Div(
        skeleton(height = CIRCLE_SIZE, width = CIRCLE_SIZE, unit = CssUnit.Pixels, modifier = CssClass.SkelCircle),
        lines,
        skeleton(height = VALUE_HEIGHT),
        skeleton(height = BADGE_HEIGHT, modifier = CssClass.SkelBadge),
    ).apply { addClassNames(CssClass.ListRow, CssClass.SkelRow) }
}

/** A pulsing shape [height] pixels high, [width] units wide or as wide as its place, with an optional shape [modifier]. */
private fun skeleton(height: Int, width: Int? = null, unit: CssUnit = CssUnit.Percent, modifier: CssClass? = null): Span = Span().apply {
    addClassName(CssClass.Skel)
    modifier?.let { name -> addClassName(name) }
    width?.let { value -> style.setWidth(value, unit) }
    style.setHeightPixels(height)
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
        addClassNames(CssClass.Skel, shape.partClass)
        element.setAriaHidden(true)
    }
    val root = Div(part).apply {
        addClassNames(CssClass.Skeleton, shape.rootClass)
        element.setRole(ElementRole.Status)
        element.setAttribute(HtmlAttribute.AriaLabel, texts.components.loading)
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
            HtmlAttribute.AriaLabel,
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

private val SkeletonShape.partClass: CssClass
    get() = when (this) {
        SkeletonShape.Text -> CssClass.SkelText
        SkeletonShape.Circle -> CssClass.SkelCircle
        SkeletonShape.Badge -> CssClass.SkelBadge
        SkeletonShape.Rectangle -> CssClass.SkelRectangle
    }

private val SkeletonShape.rootClass: CssClass
    get() = when (this) {
        SkeletonShape.Text -> CssClass.SkeletonText
        SkeletonShape.Circle -> CssClass.SkeletonCircle
        SkeletonShape.Badge -> CssClass.SkeletonBadge
        SkeletonShape.Rectangle -> CssClass.SkeletonRectangle
    }
