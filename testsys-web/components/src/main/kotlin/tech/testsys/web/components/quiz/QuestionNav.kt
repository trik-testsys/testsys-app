@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.quiz

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.SelectionHandle
import tech.testsys.web.components.core.AriaCurrent
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.ElementType
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.focusClient
import tech.testsys.web.components.core.setAnswered
import tech.testsys.web.components.core.setAriaCurrent
import tech.testsys.web.components.core.setAriaHidden
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setClassName
import tech.testsys.web.components.core.setFlagged
import tech.testsys.web.components.core.setType
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope

/**
 * Navigation between one-based question numbers; progress is supplied by the application.
 *
 * @property total the positive number of questions.
 * @property current the selected one-based number.
 * @property answered the answered numbers.
 * @property flagged the marked numbers.
 * @property unavailable the numbers unavailable for user selection.
 * @since %CURRENT_VERSION%
 */
data class QuestionNavData(
    val total: Int,
    val current: Int = 1,
    val answered: Set<Int> = emptySet(),
    val flagged: Set<Int> = emptySet(),
    val unavailable: Set<Int> = emptySet(),
) {
    init {
        require(total > 0 && current in 1..total) { "Question current $current must belong to 1..$total" }
        require(
            (answered + flagged + unavailable).all { number -> number in 1..total },
        ) { "Question status numbers must belong to 1..$total" }
    }
}

/**
 * Adds a question navigator of [data] with application-controlled statuses.
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.questionNav(
    data: QuestionNavData,
    configure: SelectionHandle<QuestionNavData>.() -> Unit = {
    },
): SelectionHandle<QuestionNavData> {
    val root = Div().apply { addClassName(CssClass.Qnav) }
    lateinit var handle: SelectionHandle<QuestionNavData>
    fun render(value: QuestionNavData) {
        root.removeAll()
        for (number in 1..value.total) {
            root.add(
                NativeButton(number.toString()).apply {
                    element.setType(ElementType.Button)
                    element.setAttribute(
                        HtmlAttribute.AriaLabel,
                        texts.components.questionStatus(
                            number,
                            number in value.answered,
                            number in value.flagged,
                        ),
                    )
                    if (number == value.current) element.setAriaCurrent(AriaCurrent.Step)
                    addClassName(CssClass.QnavCell)
                    setClassName(CssClass.QnavCellCurrent, number == value.current)
                    setClassName(CssClass.QnavCellAnswered, number in value.answered && number != value.current)
                    isEnabled = number !in value.unavailable
                    if (number in value.flagged) {
                        add(
                            Span().apply {
                                addClassName(CssClass.QnavFlag)
                                element.setAriaHidden(true)
                            },
                        )
                    }
                    element.setAnswered(number in value.answered)
                    element.setFlagged(number in value.flagged)
                    addClickListener {
                        if (number !in handle.data.unavailable) {
                            handle.choose(handle.data.copy(current = number))
                            root.children.toList().getOrNull(number - 1)?.element?.focusClient()
                        }
                    }
                },
            )
        }
    }
    render(data)
    add(root)
    handle = SelectionHandle(root, data, ::render)
    return handle.apply(configure)
}

/**
 * Adds a question navigator on [size] columns, or the remaining columns.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.questionNav(
    data: QuestionNavData,
    size: Int? = null,
    configure: SelectionHandle<QuestionNavData>.() -> Unit = {},
): SelectionHandle<QuestionNavData> = placeContent(size, Div()).questionNav(data, configure)
