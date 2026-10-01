package tech.testsys.web.components.display

import com.vaadin.flow.component.Text
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.DataHandle
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import java.math.BigDecimal

/**
 * Adds a neutral numeric verdict with [score] and optional application-provided [label].
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.verdict(score: Double, label: String? = null, configure: DataHandle<Double>.() -> Unit = {}): DataHandle<Double> {
    val root = Span().apply { addClassNames("ts-verdict", "ts-verdict--score") }
    fun render(value: Double) {
        require(value.isFinite()) { "Verdict score must be finite, got $value" }
        root.removeAll()
        root.add(Text(BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()))
        label?.let { caption -> root.add(Span(caption).apply { addClassName("ts-verdict__label") }) }
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
): DataHandle<Double> {
    val holder = Span()
    val handle = ContentScope(holder, texts, tech.testsys.web.components.layout.Placement.Body).verdict(score, label, configure)
    place(size, holder)
    return handle
}
