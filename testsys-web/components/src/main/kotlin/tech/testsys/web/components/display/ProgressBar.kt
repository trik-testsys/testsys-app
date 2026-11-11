@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.display

import com.vaadin.flow.component.html.Div
import tech.testsys.web.components.DataHandle
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.CssUnit
import tech.testsys.web.components.core.ElementRole
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.removeAttribute
import tech.testsys.web.components.core.removeWidth
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setClassName
import tech.testsys.web.components.core.setRole
import tech.testsys.web.components.core.setWidth
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope

private const val PERCENT_MAX = 100.0

/**
 * Progress with either a known percentage or an unknown remaining duration.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ProgressValue {
    /**
     * Known progress; [percent] must be finite and between zero and one hundred.
     *
     * @property percent the completed percentage.
     * @since 1.0.0-beta.1
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
     * @since 1.0.0-beta.1
     */
    data object Indeterminate : ProgressValue
}

/**
 * Adds a progress indicator of [value] with accessible [label] and semantic [tone].
 *
 * @since 1.0.0-beta.1
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
 * @since 1.0.0-beta.1
 */
fun BlockRowScope.progressBar(
    label: String,
    value: ProgressValue,
    size: Int? = null,
    tone: Tone = Tone.Info,
    configure: DataHandle<ProgressValue>.() -> Unit = {},
): DataHandle<ProgressValue> = placeContent(size, Div()).progressBar(label, value, tone, configure)

private val Tone.progressClass: CssClass
    get() = when (this) {
        Tone.Neutral -> CssClass.ProgressNeutral
        Tone.Info -> CssClass.ProgressAccent
        Tone.Success -> CssClass.ProgressSuccess
        Tone.Warning -> CssClass.ProgressWarning
        Tone.Danger -> CssClass.ProgressDanger
    }
