package tech.testsys.web.components.navigation

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
 * One application-controlled step of a stepper.
 *
 * @property label the step heading.
 * @property description the optional supporting caption.
 * @property isSelectable whether users may choose this step.
 * @since %CURRENT_VERSION%
 */
data class StepData(val label: String, val description: String? = null, val isSelectable: Boolean = true)

/**
 * Stepper state without workflow calculations.
 *
 * @property steps the ordered steps.
 * @property current the current zero-based step index.
 * @since %CURRENT_VERSION%
 */
data class StepperData(val steps: List<StepData>, val current: Int = 0) {
    init {
        require(current in steps.indices) {
            "Stepper current index $current must belong to ${steps.indices}"
        }
    }
}

/**
 * Adds a stepper of [data]; callbacks receive changed user selections only.
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.stepper(data: StepperData, configure: SelectionHandle<StepperData>.() -> Unit = {}): SelectionHandle<StepperData> {
    val root = Div().apply { addClassName("ts-stepper") }
    lateinit var handle: SelectionHandle<StepperData>
    fun render(value: StepperData) {
        root.removeAll()
        value.steps.forEachIndexed { index, step ->
            root.add(
                NativeButton().apply {
                    element.setAttribute("type", "button")
                    element.setAttribute("aria-label", step.label)
                    if (index == value.current) element.setAttribute("aria-current", "step")
                    addClassName("ts-step")
                    setClassName("ts-step--grow", index != value.steps.lastIndex)
                    setClassName("ts-step--done", index < value.current)
                    setClassName("ts-step--current", index == value.current)
                    isEnabled = step.isSelectable
                    add(
                        Span().apply {
                            addClassName("ts-step__dot")
                            if (index < value.current) {
                                add(svgIcon(IconName.Check, tech.testsys.web.components.core.ICON_SIZE_SMALL))
                            } else {
                                text = (index + 1).toString()
                            }
                        },
                    )
                    val titles = Span(Span(step.label).apply { addClassName("ts-step__label") }).apply {
                        addClassName(
                            "ts-step__text",
                        )
                    }
                    step.description?.let { caption -> titles.add(Span(caption).apply { addClassName("ts-step__sub") }) }
                    add(titles)
                    if (index != value.steps.lastIndex) add(Span().apply { addClassName("ts-step__line") })
                    addClickListener {
                        if (step.isSelectable) {
                            handle.choose(handle.data.copy(current = index))
                            root.children.toList().getOrNull(index)?.element?.executeJs("this.focus()")
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
 * Adds a stepper on [size] columns, or the remaining columns.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.stepper(
    data: StepperData,
    size: Int? = null,
    configure: SelectionHandle<StepperData>.() -> Unit = {},
): SelectionHandle<StepperData> = ContentScope(place(size, Div()), texts, Placement.Body).stepper(data, configure)
