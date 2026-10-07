package tech.testsys.web.components

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HtmlContainer
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.Signal

/**
 * Handle of a design system element that a page can show or hide after building it.
 *
 * @property isVisible whether the element is shown.
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
open class ElementHandle internal constructor(internal val component: Component) {
    open var isVisible: Boolean
        get() = component.isVisible
        set(value) {
            component.isVisible = value
        }

    /**
     * Binds [isVisible] to [signal]: every value it produces is shown at once. A manual [isVisible] while bound, and
     * a second binding, throw [BindingActiveException].
     *
     * @since %CURRENT_VERSION%
     */
    open fun bindVisible(signal: Signal<Boolean>): SignalBinding<Boolean> = component.bindVisible(signal)
}

/**
 * Handle of an element whose text a page replaces, e.g. a counter or a paragraph of text.
 *
 * @property text the shown text.
 * @since %CURRENT_VERSION%
 */
class TextHandle internal constructor(private val holder: HtmlContainer) : ElementHandle(holder) {
    var text: String
        get() = holder.text
        set(value) {
            holder.text = value
        }

    /**
     * Binds [text] to [signal]: every value it produces is shown at once. A manual [text] while bound, and a second
     * binding, throw [BindingActiveException].
     *
     * @since %CURRENT_VERSION%
     */
    fun bindText(signal: Signal<String>): SignalBinding<String> = holder.bindText(signal)
}
