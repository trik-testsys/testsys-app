@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.display

import com.vaadin.flow.component.Text
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.DataHandle
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/**
 * Adds a neutral numeric verdict with [score] and optional application-provided [label].
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.verdict(score: Double, label: String? = null, configure: DataHandle<Double>.() -> Unit = {}): DataHandle<Double> {
    val root = Span().apply { addClassNames(CssClass.Verdict, CssClass.VerdictScore) }

    fun render(value: Double) {
        require(value.isFinite()) { "Verdict score must be finite, got $value" }
        root.removeAll()
        root.add(Text(formatScore(value, texts.locale)))
        label?.let { caption -> root.add(Span(caption).apply { addClassName(CssClass.VerdictLabel) }) }
    }

    render(score)
    add(root)
    return DataHandle(root, score, ::render).apply(configure)
}

/**
 * Adds a numeric verdict on [size] columns, or the remaining columns of the row.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.verdict(
    score: Double,
    label: String? = null,
    size: Int? = null,
    configure: DataHandle<Double>.() -> Unit = {},
): DataHandle<Double> = placeContent(size, Span()).verdict(score, label, configure)

/** Formats [score] with the digit grouping of [locale], keeping every significant fractional digit. */
private fun formatScore(score: Double, locale: Locale): String {
    val decimal = BigDecimal.valueOf(score).stripTrailingZeros()
    return NumberFormat.getInstance(locale).apply { maximumFractionDigits = decimal.scale().coerceAtLeast(0) }.format(decimal)
}
