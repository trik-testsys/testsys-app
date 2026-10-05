@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.navigation

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.SelectionHandle
import tech.testsys.web.components.core.AriaCurrent
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.ElementType
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.focusClient
import tech.testsys.web.components.core.setAriaCurrent
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setClassName
import tech.testsys.web.components.core.setType
import tech.testsys.web.components.core.svgIcon
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope

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
    val root = Div().apply { addClassName(CssClass.Stepper) }
    lateinit var handle: SelectionHandle<StepperData>
    fun render(value: StepperData) {
        root.removeAll()
        value.steps.forEachIndexed { index, step ->
            root.add(
                NativeButton().apply {
                    element.setType(ElementType.Button)
                    element.setAttribute(HtmlAttribute.AriaLabel, step.label)
                    if (index == value.current) element.setAriaCurrent(AriaCurrent.Step)
                    addClassName(CssClass.Step)
                    setClassName(CssClass.StepGrow, index != value.steps.lastIndex)
                    setClassName(CssClass.StepDone, index < value.current)
                    setClassName(CssClass.StepCurrent, index == value.current)
                    isEnabled = step.isSelectable
                    add(
                        Span().apply {
                            addClassName(CssClass.StepDot)
                            if (index < value.current) {
                                add(svgIcon(IconName.Check, tech.testsys.web.components.core.ICON_SIZE_SMALL))
                            } else {
                                text = (index + 1).toString()
                            }
                        },
                    )
                    val titles = Span(Span(step.label).apply { addClassName(CssClass.StepLabel) }).apply {
                        addClassName(CssClass.StepText)
                    }
                    step.description?.let { caption -> titles.add(Span(caption).apply { addClassName(CssClass.StepSub) }) }
                    add(titles)
                    if (index != value.steps.lastIndex) add(Span().apply { addClassName(CssClass.StepLine) })
                    addClickListener {
                        if (step.isSelectable) {
                            handle.choose(handle.data.copy(current = index))
                            root.children.toList().getOrNull(index)?.element?.focusClient()
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
): SelectionHandle<StepperData> = placeContent(size, Div()).stepper(data, configure)
