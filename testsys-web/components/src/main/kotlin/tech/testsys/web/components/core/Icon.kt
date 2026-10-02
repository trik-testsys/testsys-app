package tech.testsys.web.components.core

import com.vaadin.flow.component.Svg
import com.vaadin.flow.component.html.NativeButton
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope

/** Icon size inside small controls and chips. */
internal const val ICON_SIZE_SMALL: Int = 14

/** Default icon size. */
internal const val ICON_SIZE: Int = 16

/** Icon size of alerts and the header. */
internal const val ICON_SIZE_LARGE: Int = 18

/** Icon size of the check mark of a filter chip. */
internal const val ICON_SIZE_TINY: Int = 12

/** Default stroke width of an icon on its 24×24 grid. */
internal const val ICON_STROKE: Int = 2

/** Stroke width of the bold check mark of a filter chip. */
internal const val ICON_STROKE_BOLD: Int = 3

/** Builds the inline SVG of [name] drawn with [strokeWidth]; the icon inherits `currentColor`. */
internal fun svgIcon(name: IconName, size: Int = ICON_SIZE, strokeWidth: Int = ICON_STROKE): Svg = Svg(
    """<svg xmlns="http://www.w3.org/2000/svg" width="$size" height="$size" viewBox="0 0 24 24" fill="none" """ +
        """stroke="currentColor" stroke-width="$strokeWidth" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">""" +
        name.paths + "</svg>",
).apply { element.classList.add("ts-icon") }

/** Builds an arrow button of a pager: a `.ts-pager__btn` with the small icon [name] and the accessible name [label]. */
internal fun pagerArrow(name: IconName, label: String): NativeButton = NativeButton().apply {
    addClassName("ts-pager__btn")
    element.setAttribute("aria-label", label)
    element.setAttribute("type", "button")
    add(svgIcon(name, ICON_SIZE_SMALL))
}

/**
 * Adds an icon of the standard size next to a text; an icon never replaces a label.
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.icon(name: IconName): ElementHandle = ElementHandle(svgIcon(name).also { icon -> add(icon) })

/**
 * Adds an icon of the standard size on [size] columns of the row, or on the rest of it.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.icon(name: IconName, size: Int? = null): ElementHandle = ElementHandle(place(size, svgIcon(name)))
