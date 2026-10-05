@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.display

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.DataHandle
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.CssProperty
import tech.testsys.web.components.core.CssUnit
import tech.testsys.web.components.core.ElementRole
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.HtmlTag
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.htmlElement
import tech.testsys.web.components.core.removeAttribute
import tech.testsys.web.components.core.removeWidth
import tech.testsys.web.components.core.set
import tech.testsys.web.components.core.setAriaHidden
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setClassName
import tech.testsys.web.components.core.setRole
import tech.testsys.web.components.core.setWidth
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope

private const val PERCENT_MAX = 100.0
private const val DIFFICULTY_INDICATORS = 3

/**
 * Progress with either a known percentage or an unknown remaining duration.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ProgressValue {
    /**
     * Known progress; [percent] must be finite and between zero and one hundred.
     *
     * @property percent the completed percentage.
     * @since %CURRENT_VERSION%
     */
    data class Determinate(val percent: Double) : ProgressValue {
        init {
            require(percent.isFinite() && percent in 0.0..PERCENT_MAX) {
                "Progress percent must be 0..100, got $percent"
            }
        }
    }

    /**
     * Active progress without a known total.
     *
     * @since %CURRENT_VERSION%
     */
    data object Indeterminate : ProgressValue
}

/**
 * Adds a progress indicator of [value] with accessible [label] and semantic [tone].
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.progressBar(
    label: String,
    value: ProgressValue,
    tone: Tone = Tone.Info,
    configure: DataHandle<ProgressValue>.() -> Unit = {},
): DataHandle<ProgressValue> {
    val bar = Div().apply { addClassName(CssClass.ProgressBar) }
    val root = Div(bar).apply {
        addClassNames(CssClass.Progress, tone.progressClass)
        setClassName(CssClass.ProgressThin, placement.isCompact)
        element.setRole(ElementRole.ProgressBar)
        element.setAttribute(HtmlAttribute.AriaLabel, label)
        element.setAttribute(HtmlAttribute.AriaValueMin, "0")
        element.setAttribute(HtmlAttribute.AriaValueMax, "100")
    }

    fun render(progress: ProgressValue) {
        root.setClassName(CssClass.ProgressIndeterminate, progress is ProgressValue.Indeterminate)
        when (progress) {
            is ProgressValue.Determinate -> {
                root.element.setAttribute(HtmlAttribute.AriaValueNow, progress.percent.toString())
                bar.element.style.setWidth(progress.percent, CssUnit.Percent)
            }
            ProgressValue.Indeterminate -> {
                root.element.removeAttribute(HtmlAttribute.AriaValueNow)
                bar.element.style.removeWidth()
            }
        }
    }
    render(value)
    add(root)
    return DataHandle(root, value, ::render).apply(configure)
}

/**
 * Adds a progress indicator on [size] columns, or the remaining columns.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.progressBar(
    label: String,
    value: ProgressValue,
    size: Int? = null,
    tone: Tone = Tone.Info,
    configure: DataHandle<ProgressValue>.() -> Unit = {},
): DataHandle<ProgressValue> = placeContent(size, Div()).progressBar(label, value, tone, configure)

/**
 * Difficulty with one, two or three filled indicators.
 *
 * @since %CURRENT_VERSION%
 */
enum class DifficultyLevel {
    /**
     * One filled indicator.
     *
     * @since %CURRENT_VERSION%
     */
    Easy,

    /**
     * Two filled indicators.
     *
     * @since %CURRENT_VERSION%
     */
    Medium,

    /**
     * Three filled indicators.
     *
     * @since %CURRENT_VERSION%
     */
    Hard,
}

/**
 * Adds a difficulty indicator for [level], with application-provided [label].
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.difficulty(
    level: DifficultyLevel,
    label: String? = null,
    showLabel: Boolean = true,
    configure: DataHandle<DifficultyLevel>.() -> Unit = {},
): DataHandle<DifficultyLevel> {
    val root = Span().apply { addClassName(CssClass.Difficulty) }
    fun render(current: DifficultyLevel) {
        root.removeAll()
        val caption = label ?: texts.components.difficultyLabels[current.ordinal]
        root.element.setAttribute(HtmlAttribute.AriaLabel, caption)
        val marks = Span()
        repeat(DIFFICULTY_INDICATORS) { index ->
            marks.element.appendChild(
                htmlElement(HtmlTag.I).apply {
                    if (index <= current.ordinal) {
                        style.setDifficultyBackground(current)
                    }
                },
            )
        }
        marks.element.setAriaHidden(true)
        root.add(marks)
        if (showLabel) root.add(Span(caption))
    }
    render(level)
    add(root)
    return DataHandle(root, level, ::render).apply(configure)
}

/**
 * Adds a difficulty indicator on [size] columns, or the remaining columns.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.difficulty(
    level: DifficultyLevel,
    label: String? = null,
    size: Int? = null,
    showLabel: Boolean = true,
    configure: DataHandle<DifficultyLevel>.() -> Unit = {},
): DataHandle<DifficultyLevel> = placeContent(size, Div()).difficulty(level, label, showLabel, configure)

private val Tone.progressClass: CssClass
    get() = when (this) {
        Tone.Neutral -> CssClass.ProgressNeutral
        Tone.Info -> CssClass.ProgressAccent
        Tone.Success -> CssClass.ProgressSuccess
        Tone.Warning -> CssClass.ProgressWarning
        Tone.Danger -> CssClass.ProgressDanger
    }

private fun com.vaadin.flow.dom.Style.setDifficultyBackground(level: DifficultyLevel) = set(
    CssProperty.Background,
    when (level) {
        DifficultyLevel.Easy -> "var(--success)"
        DifficultyLevel.Medium -> "var(--warning)"
        DifficultyLevel.Hard -> "var(--danger)"
    },
)
