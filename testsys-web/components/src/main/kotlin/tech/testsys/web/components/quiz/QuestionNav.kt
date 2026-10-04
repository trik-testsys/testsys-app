package tech.testsys.web.components.quiz

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.SelectionHandle
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
    val root = Div().apply { addClassName("ts-qnav") }
    lateinit var handle: SelectionHandle<QuestionNavData>
    fun render(value: QuestionNavData) {
        root.removeAll()
        for (number in 1..value.total) {
            root.add(
                NativeButton(number.toString()).apply {
                    element.setAttribute("type", "button")
                    element.setAttribute(
                        "aria-label",
                        texts.components.questionStatus(
                            number,
                            number in value.answered,
                            number in value.flagged,
                        ),
                    )
                    if (number == value.current) element.setAttribute("aria-current", "step")
                    addClassName("ts-qnav__cell")
                    setClassName("ts-qnav__cell--current", number == value.current)
                    setClassName("ts-qnav__cell--answered", number in value.answered && number != value.current)
                    isEnabled = number !in value.unavailable
                    if (number in value.flagged) {
                        add(
                            Span().apply {
                                addClassName("ts-qnav__flag")
                                element.setAttribute(
                                    "aria-hidden",
                                    "true",
                                )
                            },
                        )
                    }
                    element.setAttribute("data-answered", (number in value.answered).toString())
                    element.setAttribute("data-flagged", (number in value.flagged).toString())
                    addClickListener {
                        if (number !in handle.data.unavailable) {
                            handle.choose(handle.data.copy(current = number))
                            root.children.toList().getOrNull(number - 1)?.element?.executeJs("this.focus()")
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
