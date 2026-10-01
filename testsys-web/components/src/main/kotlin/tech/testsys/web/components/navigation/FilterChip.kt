package tech.testsys.web.components.navigation

import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.Signal
import tech.testsys.web.components.Bindable
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.core.ICON_SIZE_TINY
import tech.testsys.web.components.core.ICON_STROKE_BOLD
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.svgIcon
import tech.testsys.web.components.layout.ContentScope

/**
 * Handle of a filter chip: whether the filter is on and the listener of user toggles.
 *
 * @property isSelected whether the filter is on; setting it does not run the listener. A manual change while
 * [bindSelected] is bound, and a second binding, throw [BindingActiveException].
 * @since %CURRENT_VERSION%
 */
class FilterChipHandle internal constructor(private val button: NativeButton, isSelected: Boolean) : ElementHandle(button) {
    private val check = svgIcon(IconName.Check, size = ICON_SIZE_TINY, strokeWidth = ICON_STROKE_BOLD)
    private val selected = Bindable(button.element, initial = isSelected) { value -> render(value) }
    private var listener: (Boolean) -> Unit = {}

    var isSelected: Boolean
        get() = selected.value
        set(value) {
            selected.value = value
        }

    init {
        button.addClassName("ts-filter")
        button.element.setAttribute("type", "button")
        button.addClickListener { toggle() }
        render(isSelected)
    }

    /**
     * Runs [listener] with the new state when a user toggles the chip. Replaces a listener set by an earlier call.
     *
     * @since %CURRENT_VERSION%
     */
    fun onChange(listener: (Boolean) -> Unit) {
        this.listener = listener
    }

    /**
     * Binds [isSelected] to [signal]: every state it produces is shown at once. A user toggle then only runs the
     * listener of [onChange], and the chip shows the new state once [signal] takes it. A manual [isSelected] while
     * bound, and a second binding, throw [BindingActiveException].
     *
     * @since %CURRENT_VERSION%
     */
    fun bindSelected(signal: Signal<Boolean>): SignalBinding<Boolean> = selected.bind(signal)

    private fun toggle() {
        val isTurnedOn = !isSelected
        if (!selected.isBound) selected.value = isTurnedOn
        listener(isTurnedOn)
    }

    private fun render(isOn: Boolean) {
        button.setClassName("ts-filter--on", isOn)
        button.element.setAttribute("aria-pressed", isOn.toString())
        if (isOn) button.addComponentAsFirst(check) else button.remove(check)
    }
}

/**
 * Adds a chip that switches a filter of what the block shows on and off, e.g. in the `actions { }` of a block;
 * [isSelected] is its initial state. The page reacts to a toggle through [FilterChipHandle.onChange].
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.filterChip(label: String, isSelected: Boolean = false, configure: FilterChipHandle.() -> Unit = {}): FilterChipHandle {
    val handle = FilterChipHandle(NativeButton(label), isSelected)
    add(handle.component)
    return handle.apply(configure)
}
