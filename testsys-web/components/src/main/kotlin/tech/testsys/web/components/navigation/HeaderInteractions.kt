package tech.testsys.web.components.navigation

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.dependency.JsModule
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.popover.Popover
import com.vaadin.flow.component.popover.PopoverPosition

/** Coordinates the header layers; opening one closes the preceding layer. */
internal class HeaderInteractions {
    private var active: Popover? = null

    fun popup(trigger: Component, label: String, theme: String, autofocus: Boolean = false): Popover {
        trigger.element.setAttribute("aria-haspopup", "dialog")
        trigger.element.setAttribute("aria-expanded", "false")
        val popup = Popover().apply {
            target = trigger
            isModal = false
            isAutofocus = autofocus
            isCloseOnEsc = true
            isCloseOnOutsideClick = true
            isOpenOnClick = true
            position = PopoverPosition.BOTTOM_END
            setAriaLabel(label)
            addThemeName("ts-header-popup")
            addThemeName(theme)
        }
        popup.addOpenedChangeListener { event ->
            trigger.element.setAttribute("aria-expanded", event.isOpened.toString())
            if (event.isOpened) {
                val previous = active
                active = popup
                previous?.takeIf { layer -> layer !== popup }?.close()
            } else if (active === popup) {
                active = null
            }
        }
        trigger.addDetachListener { popup.close() }
        return popup
    }
}

@JsModule("./testsys-ui/header-interactions.ts")
internal class HeaderRoot : Div() {
    init {
        addClassName("ts-header")
        addAttachListener { element.executeJs("window.testsysHeader.attach(this)") }
        addDetachListener { element.executeJs("window.testsysHeader.detach(this)") }
    }
}
