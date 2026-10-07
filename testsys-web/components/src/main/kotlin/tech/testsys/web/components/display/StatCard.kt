@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.display

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.TextHandle
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.layout.BlockHeading
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.SlotRowScope

/**
 * Direction of a stat card change; it colours the delta.
 *
 * @since %CURRENT_VERSION%
 */
enum class Trend(internal val cssClass: CssClass) {
    Up(CssClass.StatDeltaUp),
    Down(CssClass.StatDeltaDown),
}

/**
 * Adds a block with one metric: its [label], [value] and an optional [delta]; the handle replaces the value.
 * The block takes [size] columns, or the whole slot if [size] is `null`.
 *
 * @since %CURRENT_VERSION%
 */
fun SlotRowScope.statCard(label: String, value: String, size: Int? = null, delta: String? = null, trend: Trend? = null): TextHandle {
    val stat = buildStat(label = label, value = value, delta = delta, trend = trend)
    val block = place(size, BlockHeading(title = null, subtitle = null), highlight = false) {
        row { place(size = null, stat.card) }
    }
    return TextHandle(holder = stat.value, component = block.component)
}

/**
 * Adds one metric on [size] columns of the row, or on the rest of it; the handle replaces the value.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.statCard(label: String, value: String, size: Int? = null, delta: String? = null, trend: Trend? = null): TextHandle {
    val stat = buildStat(label = label, value = value, delta = delta, trend = trend)
    place(size, stat.card)
    return TextHandle(stat.value, stat.card)
}

/** Markup of a metric and its value text. */
private class Stat(val card: Div, val value: Span)

private fun buildStat(label: String, value: String, delta: String?, trend: Trend?): Stat {
    val valueText = Span(value).apply { addClassName(CssClass.StatValue) }
    val card = Div(Span(label).apply { addClassName(CssClass.StatLabel) }, valueText).apply { addClassName(CssClass.Stat) }
    if (delta != null) {
        card.add(
            Span(delta).apply {
                addClassName(CssClass.StatDelta)
                trend?.let { direction -> addClassName(direction.cssClass) }
            },
        )
    }
    return Stat(card, valueText)
}
