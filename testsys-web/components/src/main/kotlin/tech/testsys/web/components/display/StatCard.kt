package tech.testsys.web.components.display

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.TextHandle
import tech.testsys.web.components.layout.BlockHeading
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.SlotRowScope

/**
 * Direction of a stat card change; it colours the delta.
 *
 * @since %CURRENT_VERSION%
 */
enum class Trend(internal val modifier: String) {
    Up("up"),
    Down("down"),
}

/**
 * Adds a block with one metric: its [label], [value] and an optional [delta]; the handle replaces the value.
 * The block takes [size] columns, or the whole slot if [size] is `null`.
 *
 * @since %CURRENT_VERSION%
 */
fun SlotRowScope.statCard(label: String, value: String, size: Int? = null, delta: String? = null, trend: Trend? = null): TextHandle {
    val stat = buildStat(label, value, delta, trend)
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
    val stat = buildStat(label, value, delta, trend)
    place(size, stat.card)
    return TextHandle(stat.value, stat.card)
}

/** Markup of a metric and its value text. */
private class Stat(val card: Div, val value: Span)

private fun buildStat(label: String, value: String, delta: String?, trend: Trend?): Stat {
    val valueText = Span(value).apply { addClassName("ts-stat__value") }
    val card = Div(Span(label).apply { addClassName("ts-stat__label") }, valueText).apply { addClassName("ts-stat") }
    if (delta != null) {
        card.add(
            Span(delta).apply {
                addClassName("ts-stat__delta")
                trend?.let { direction -> addClassName("ts-stat__delta--${direction.modifier}") }
            },
        )
    }
    return Stat(card, valueText)
}
