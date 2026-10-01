package tech.testsys.web.components.overlay

import com.vaadin.flow.component.shared.Tooltip
import com.vaadin.flow.dom.Element
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.signals.Signal
import tech.testsys.web.components.Bindable
import tech.testsys.web.components.ElementHandle

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
    Left,
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
 * Attaches plain [text] above or below this element, according to [placement].
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
            TooltipPlacement.Top ->
                Tooltip.TooltipPosition.TOP
            TooltipPlacement.Bottom -> Tooltip.TooltipPosition.BOTTOM
            TooltipPlacement.Left -> Tooltip.TooltipPosition.START
            TooltipPlacement.Right -> Tooltip.TooltipPosition.END
        },
    ),
    component.element,
).apply(configure)
