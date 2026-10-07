package tech.testsys.web.components.overlay

import com.vaadin.flow.component.shared.Tooltip
import com.vaadin.flow.dom.Element
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.signals.Signal
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.core.Bindable

/**
 * Position of a plain text tooltip.
 *
 * @since %CURRENT_VERSION%
 */
enum class TooltipPlacement {
    /**
     * Above the target.
     *
     * @since %CURRENT_VERSION%
     */
    Top,

    /**
     * Below the target.
     *
     * @since %CURRENT_VERSION%
     */
    Bottom,

    /**
     * Before the target in the reading direction: on the left in left-to-right text.
     *
     * @since %CURRENT_VERSION%
     */
    Left,

    /**
     * After the target in the reading direction: on the right in left-to-right text.
     *
     * @since %CURRENT_VERSION%
     */
    Right,
}

/**
 * Handle of a noninteractive tooltip attached to an existing element.
 *
 * @property text the plain tooltip content.
 * @since %CURRENT_VERSION%
 */
class TooltipHandle internal constructor(private val tooltip: Tooltip, element: Element) {
    private val state = Bindable(element, tooltip.text) { value -> tooltip.text = value }
    var text: String
        get() = state.value
        set(value) {
            state.value = value
        }

    /**
     * Binds tooltip content to [signal].
     *
     * @since %CURRENT_VERSION%
     */
    fun bindText(signal: Signal<String>): SignalBinding<String> = state.bind(signal)
}

/**
 * Attaches plain [text] beside this element, on the side given by [placement].
 *
 * @since %CURRENT_VERSION%
 */
fun ElementHandle.tooltip(
    text: String,
    placement: TooltipPlacement = TooltipPlacement.Top,
    configure: TooltipHandle.() -> Unit = {},
): TooltipHandle = TooltipHandle(
    Tooltip.forComponent(component).withText(text).withPosition(
        when (placement) {
            TooltipPlacement.Top -> Tooltip.TooltipPosition.TOP
            TooltipPlacement.Bottom -> Tooltip.TooltipPosition.BOTTOM
            TooltipPlacement.Left -> Tooltip.TooltipPosition.START
            TooltipPlacement.Right -> Tooltip.TooltipPosition.END
        },
    ),
    component.element,
).apply(configure)
