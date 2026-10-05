@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.quiz

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.SelectionHandle
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.ElementType
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.setAriaDisabled
import tech.testsys.web.components.core.setAriaPressed
import tech.testsys.web.components.core.setClassName
import tech.testsys.web.components.core.setType
import tech.testsys.web.components.core.svgIcon
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope

/**
 * Application-provided checked result; the UI calculates no answer correctness.
 *
 * @since %CURRENT_VERSION%
 */
enum class QuizResult { Unchecked, Correct, Wrong }

/**
 * Quiz option state and application labels.
 *
 * @property label the answer caption.
 * @property letter the optional answer marker.
 * @property isSelected whether the answer is selected.
 * @property isMultiple whether the question permits several answers.
 * @property result the application-provided answer result, which locks selection after checking.
 * @since %CURRENT_VERSION%
 */
data class QuizOptionData(
    val label: String,
    val letter: String? = null,
    val isSelected: Boolean = false,
    val isMultiple: Boolean = false,
    val result: QuizResult = QuizResult.Unchecked,
)

/**
 * Adds an answer option of [data], locking user changes after a correct or wrong result.
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.quizOption(
    data: QuizOptionData,
    configure: SelectionHandle<QuizOptionData>.() -> Unit = {
    },
): SelectionHandle<QuizOptionData> {
    val root = NativeButton().apply { element.setType(ElementType.Button) }
    lateinit var handle: SelectionHandle<QuizOptionData>
    fun render(value: QuizOptionData) {
        root.removeAll()
        root.addClassName(CssClass.Qopt)
        root.setClassName(CssClass.QoptMulti, value.isMultiple)
        root.setClassName(CssClass.QoptSelected, value.isSelected && value.result == QuizResult.Unchecked)
        root.setClassName(CssClass.QoptCorrect, value.result == QuizResult.Correct)
        root.setClassName(CssClass.QoptWrong, value.result == QuizResult.Wrong)
        root.setClassName(CssClass.QoptLocked, value.result != QuizResult.Unchecked)
        root.element.setAriaPressed(value.isSelected)
        root.element.setAriaDisabled(value.result != QuizResult.Unchecked)
        root.add(
            Span().apply {
                addClassName(CssClass.QoptInd)
                if (value.isSelected || value.result == QuizResult.Correct) {
                    add(svgIcon(IconName.Check, tech.testsys.web.components.core.ICON_SIZE_TINY))
                }
            },
        )
        value.letter?.let { marker -> root.add(Span(marker).apply { addClassName(CssClass.QoptLetter) }) }
        root.add(Span(value.label).apply { addClassName(CssClass.QoptLabel) })
    }
    render(data)
    root.addClickListener {
        if (handle.data.result == QuizResult.Unchecked) {
            handle.choose(handle.data.copy(isSelected = !handle.data.isSelected))
        }
    }
    add(root)
    handle = SelectionHandle(root, data, ::render)
    return handle.apply(configure)
}

/**
 * Adds a quiz option on [size] columns, or the remaining columns.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.quizOption(
    data: QuizOptionData,
    size: Int? = null,
    configure: SelectionHandle<QuizOptionData>.() -> Unit = {
    },
): SelectionHandle<QuizOptionData> = placeContent(size, Div()).quizOption(data, configure)
