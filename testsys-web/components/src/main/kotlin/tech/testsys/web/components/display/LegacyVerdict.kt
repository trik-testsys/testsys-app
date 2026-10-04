package tech.testsys.web.components.display

import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.DataHandle
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope

/**
 * Compatibility captions for external verdict formats; they define no system grading rules.
 *
 * @since %CURRENT_VERSION%
 */
enum class LegacyVerdict(internal val caption: String, internal val modifier: String) {
    Accepted("OK", "ok"),
    WrongAnswer("WA", "wa"),
    TimeLimitExceeded("TLE", "tle"),
    MemoryLimitExceeded("MLE", "mle"),
    RuntimeError("RE", "re"),
    CompilationError("CE", "ce"),
    Queued("…", "queue"),
}

/**
 * Adds the compatibility verdict [value] using core markup.
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.legacyVerdict(value: LegacyVerdict, configure: DataHandle<LegacyVerdict>.() -> Unit = {}): DataHandle<LegacyVerdict> {
    val root = Span()
    fun render(verdict: LegacyVerdict) {
        root.classNames.clear()
        root.addClassNames("ts-verdict", "ts-verdict--${verdict.modifier}")
        root.text = verdict.caption
    }
    render(value)
    add(root)
    return DataHandle(root, value, ::render).apply(configure)
}

/**
 * Adds a compatibility verdict on [size] fractions, or the remaining fractions.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.legacyVerdict(
    value: LegacyVerdict,
    size: Int? = null,
    configure: DataHandle<LegacyVerdict>.() -> Unit = {},
): DataHandle<LegacyVerdict> = placeContent(size, Span()).legacyVerdict(value, configure)
