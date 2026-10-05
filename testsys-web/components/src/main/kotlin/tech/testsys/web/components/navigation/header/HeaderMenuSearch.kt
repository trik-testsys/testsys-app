@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.navigation.header

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Input
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.dom.Element
import tech.testsys.web.components.core.AriaPopup
import tech.testsys.web.components.core.Autocomplete
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.DomEvent
import tech.testsys.web.components.core.DomEventData
import tech.testsys.web.components.core.ElementRole
import tech.testsys.web.components.core.ElementType
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addEventData
import tech.testsys.web.components.core.addEventListener
import tech.testsys.web.components.core.get
import tech.testsys.web.components.core.setAriaExpanded
import tech.testsys.web.components.core.setAriaHasPopup
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setAutocomplete
import tech.testsys.web.components.core.setRole
import tech.testsys.web.components.core.setType
import tech.testsys.web.components.core.svgIcon
import tech.testsys.web.components.texts.HeaderTexts
import java.util.UUID

/** Common native input and visual shell for both supported search modes. */
internal class HeaderSearchField(val field: Input, val component: Div)

internal fun headerSearchField(label: String): HeaderSearchField {
    val field = Input(null).apply {
        element.setType(ElementType.Search)
        element.setAttribute(HtmlAttribute.Placeholder, label)
        element.setAttribute(HtmlAttribute.AriaLabel, label)
        element.setAutocomplete(Autocomplete.Off)
    }
    val shell = Div(svgIcon(IconName.Search), field, Span("⌘K").apply { addClassName(CssClass.Kbd) }).apply {
        addClassName(CssClass.HeaderSearch)
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
        field.element.setAttribute(HtmlAttribute.AriaControls, id)
        field.element.setAriaHasPopup(AriaPopup.Dialog)
        field.element.setAriaExpanded(false)
        menu.popup.addOpenedChangeListener { event ->
            field.element.setAriaExpanded(event.isOpened)
        }
        field.element.addEventListener(DomEvent.HeaderMenuInput) { event ->
            inputChanged(event.eventData.get(DomEventData.DetailQuery).asString())
        }
            .addEventData(DomEventData.DetailQuery)
        field.element.addEventListener(DomEvent.HeaderMenuClose) { close() }
        component.addAttachListener {
            field.element.attachMenuSearch(popup = menu.popup.element, trigger = menu.trigger.element)
        }
        component.addDetachListener {
            close()
            field.element.detachMenuSearch()
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

private fun Element.attachMenuSearch(popup: Element, trigger: Element) = executeJs(
    "window.testsysHeader.menuSearchAttach(this, $0, $1)",
    popup,
    trigger,
)

private fun Element.detachMenuSearch() = executeJs("window.testsysHeader.menuSearchDetach(this)")
