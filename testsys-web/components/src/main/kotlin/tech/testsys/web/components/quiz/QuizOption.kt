package tech.testsys.web.components.quiz

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.SelectionHandle
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.svgIcon
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.Placement

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
    val root = NativeButton().apply { element.setAttribute("type", "button") }
    lateinit var handle: SelectionHandle<QuizOptionData>
    fun render(value: QuizOptionData) {
        root.removeAll()
        root.addClassName("ts-qopt")
        root.setClassName("ts-qopt--multi", value.isMultiple)
        root.setClassName("ts-qopt--selected", value.isSelected && value.result == QuizResult.Unchecked)
        root.setClassName("ts-qopt--correct", value.result == QuizResult.Correct)
        root.setClassName("ts-qopt--wrong", value.result == QuizResult.Wrong)
        root.setClassName("ts-qopt--locked", value.result != QuizResult.Unchecked)
        root.element.setAttribute("aria-pressed", value.isSelected.toString())
        root.element.setAttribute("aria-disabled", (value.result != QuizResult.Unchecked).toString())
        root.add(
            Span().apply {
                addClassName("ts-qopt__ind")
                if (value.isSelected || value.result == QuizResult.Correct) {
                    add(svgIcon(IconName.Check, tech.testsys.web.components.core.ICON_SIZE_TINY))
                }
            },
        )
        value.letter?.let { marker -> root.add(Span(marker).apply { addClassName("ts-qopt__letter") }) }
        root.add(Span(value.label).apply { addClassName("ts-qopt__label") })
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
): SelectionHandle<QuizOptionData> = ContentScope(place(size, Div()), texts, Placement.Body).quizOption(data, configure)
