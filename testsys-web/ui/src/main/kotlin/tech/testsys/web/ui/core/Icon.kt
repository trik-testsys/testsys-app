package tech.testsys.web.ui.core

import com.vaadin.flow.component.Svg
import tech.testsys.web.ui.ElementHandle
import tech.testsys.web.ui.layout.BlockRowScope
import tech.testsys.web.ui.layout.ContentScope

/** Icon size inside small controls and chips. */
internal const val ICON_SIZE_SMALL: Int = 14

/** Default icon size. */
internal const val ICON_SIZE: Int = 16

/** Icon size of alerts and the header. */
internal const val ICON_SIZE_LARGE: Int = 18

/** Builds the inline SVG of [name]; the icon inherits `currentColor`. */
internal fun svgIcon(name: IconName, size: Int = ICON_SIZE): Svg = Svg(
    """<svg xmlns="http://www.w3.org/2000/svg" width="$size" height="$size" viewBox="0 0 24 24" fill="none" """ +
        """stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">""" +
        name.paths + "</svg>",
).apply { element.classList.add("ts-icon") }

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
