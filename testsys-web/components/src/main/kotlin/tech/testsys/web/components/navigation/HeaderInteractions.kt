@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.navigation

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.dependency.JsModule
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.popover.Popover
import com.vaadin.flow.component.popover.PopoverPosition
import com.vaadin.flow.dom.Element
import tech.testsys.web.components.core.AriaPopup
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.CssTheme
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.addThemeName
import tech.testsys.web.components.core.setAriaExpanded
import tech.testsys.web.components.core.setAriaHasPopup

private const val HEADER_INTERACTIONS_MODULE = "./testsys-ui/header-interactions.ts"

/** Coordinates the header layers; opening one closes the preceding layer. */
internal class HeaderInteractions {
    private var active: Popover? = null

    fun popup(trigger: Component, label: String, theme: CssTheme, autofocus: Boolean = false): Popover {
        trigger.element.setAriaHasPopup(AriaPopup.Dialog)
        trigger.element.setAriaExpanded(false)
        val popup = Popover().apply {
            target = trigger
            isModal = false
            isAutofocus = autofocus
            isCloseOnEsc = true
            isCloseOnOutsideClick = true
            isOpenOnClick = true
            position = PopoverPosition.BOTTOM_END
            setAriaLabel(label)
            addThemeName(CssTheme.HeaderPopup)
            addThemeName(theme)
        }
        popup.addOpenedChangeListener { event ->
            trigger.element.setAriaExpanded(event.isOpened)
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

@JsModule(HEADER_INTERACTIONS_MODULE)
internal class HeaderRoot : Div() {
    init {
        addClassNames(CssClass.Header, CssClass.HeaderSticky)
        addAttachListener { element.attachHeaderInteractions() }
        addDetachListener { element.detachHeaderInteractions() }
    }
}

private fun Element.attachHeaderInteractions() = executeJs("window.testsysHeader.attach(this)")

private fun Element.detachHeaderInteractions() = executeJs("window.testsysHeader.detach(this)")
