@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.navigation

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Input
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.HeaderTexts
import tech.testsys.web.components.core.AriaPopup
import tech.testsys.web.components.core.Autocomplete
import tech.testsys.web.components.core.ElementRole
import tech.testsys.web.components.core.ElementType
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.setAriaExpanded
import tech.testsys.web.components.core.setAriaHasPopup
import tech.testsys.web.components.core.setAutocomplete
import tech.testsys.web.components.core.setRole
import tech.testsys.web.components.core.setType
import tech.testsys.web.components.core.svgIcon
import java.util.UUID

/** Common native input and visual shell for both supported search modes. */
internal class HeaderSearchField(val field: Input, val component: Div)

internal fun headerSearchField(label: String): HeaderSearchField {
    val field = Input(null).apply {
        element.setType(ElementType.Search)
        element.setAttribute("placeholder", label)
        element.setAttribute("aria-label", label)
        element.setAutocomplete(Autocomplete.Off)
    }
    val shell = Div(svgIcon(IconName.Search), field, Span("⌘K").apply { addClassName("ts-kbd") }).apply {
        addClassName("ts-header__search")
    }
    return HeaderSearchField(field, shell)
}

/** Filters the existing menu surface synchronously without creating a provider popup. */
internal class HeaderMenuSearchController(private val menu: MegaMenuHandle, private val texts: HeaderTexts) {
    private val input = headerSearchField(texts.search)
    val field = input.field
    val component = input.component

    init {
        menu.searchMode(component)
        val id = "ts-header-menu-${UUID.randomUUID()}"
        menu.popup.setId(id)
        menu.popup.isAutofocus = false
        menu.popup.isCloseOnEsc = false
        menu.popup.isCloseOnOutsideClick = false
        field.element.setRole(ElementRole.SearchBox)
        field.element.setAttribute("aria-controls", id)
        field.element.setAriaHasPopup(AriaPopup.Dialog)
        field.element.setAriaExpanded(false)
        menu.popup.addOpenedChangeListener { event ->
            field.element.setAriaExpanded(event.isOpened)
        }
        field.element.addEventListener("header-menu-input") { event -> inputChanged(event.eventData.get("event.detail.query").asString()) }
            .addEventData("event.detail.query")
        field.element.addEventListener("header-menu-close") { close() }
        component.addAttachListener {
            field.element.executeJs(
                "window.testsysHeader.menuSearchAttach(this, $0, $1)",
                menu.popup.element,
                menu.trigger.element,
            )
        }
        component.addDetachListener {
            close()
            field.element.executeJs("window.testsysHeader.menuSearchDetach(this)")
        }
    }

    fun inputChanged(query: String) {
        menu.filter(query = query, emptyText = texts.searchEmpty)
        menu.popup.open()
    }

    fun close() {
        menu.popup.close()
    }
}
