@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.core

import com.github.mvysny.kaributesting.v10._fireDomEvent
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Div
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tools.jackson.databind.ObjectMapper

internal class DomNamesTests : MockVaadinTests() {
    @Test
    fun `should serialize native tags attributes and string and boolean properties`() {
        val element = htmlElement(HtmlTag.Span)

        element.setAttribute(HtmlAttribute.AriaLabel, "Sample")
        element.setProperty(DomProperty.Value, "content")
        element.setProperty(DomProperty.Disabled, false)

        assertEquals("span", element.tag)
        assertEquals("Sample", element.getAttribute("aria-label"))
        assertEquals("content", element.getProperty("value"))
        assertFalse(element.getProperty("disabled", true))
        assertSame(element, element.removeAttribute(HtmlAttribute.AriaLabel))
        assertFalse(element.hasAttribute("aria-label"))
    }

    @Test
    fun `should retain listener data and filter registration and dispatch`() {
        val component = Div()
        UI.getCurrent().add(component)
        var query = ""
        val listener = component.element.addEventListener(DomEvent.HeaderInput) { event ->
            query = event.eventData.get(DomEventData.DetailQuery).asString()
        }
        assertSame(listener, listener.addEventData(DomEventData.DetailQuery))
        assertSame(listener, listener.setFilter(DomEventFilter.TableSortKey))
        val data = ObjectMapper().createObjectNode()
            .put("event.detail.query", "chosen")
            .put("event.key === 'Enter' || event.key === ' '", true)

        component._fireDomEvent("header-input", data)

        assertEquals("chosen", query)
    }
}
