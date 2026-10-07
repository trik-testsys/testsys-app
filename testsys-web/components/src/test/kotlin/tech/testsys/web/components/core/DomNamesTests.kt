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
    fun `should create a native tag`() {
        val element = htmlElement(HtmlTag.Span)

        assertEquals("span", element.tag)
    }

    @Test
    fun `should serialize attributes and string and boolean properties`() {
        val element = htmlElement(HtmlTag.Span)

        element.setAttribute(HtmlAttribute.AriaLabel, "Sample")
        element.setProperty(DomProperty.Value, "content")
        element.setProperty(DomProperty.Disabled, false)

        assertEquals("Sample", element.getAttribute("aria-label"))
        assertEquals("content", element.getProperty("value"))
        assertFalse(element.getProperty("disabled", true))
    }

    @Test
    fun `should remove an attribute and return the element`() {
        val element = htmlElement(HtmlTag.Span).apply { setAttribute(HtmlAttribute.AriaLabel, "Sample") }

        val result = element.removeAttribute(HtmlAttribute.AriaLabel)

        assertSame(element, result)
        assertFalse(element.hasAttribute("aria-label"))
    }

    @Test
    fun `should return the same registration from event data settings`() {
        val listener = Div().also { component -> UI.getCurrent().add(component) }.element.addEventListener(DomEvent.HeaderInput) {}

        val result = listener.addEventData(DomEventData.DetailQuery)

        assertSame(listener, result)
    }

    @Test
    fun `should return the same registration from filter settings`() {
        val listener = Div().also { component -> UI.getCurrent().add(component) }.element.addEventListener(DomEvent.HeaderInput) {}

        val result = listener.setFilter(DomEventFilter.TableSortKey)

        assertSame(listener, result)
    }

    @Test
    fun `should dispatch an event that passes the filter with its data`() {
        val component = Div().also { div -> UI.getCurrent().add(div) }
        var query = ""
        component.element.addEventListener(DomEvent.HeaderInput) { event ->
            query = event.eventData.get(DomEventData.DetailQuery).asString()
        }.addEventData(DomEventData.DetailQuery).setFilter(DomEventFilter.TableSortKey)
        val data = ObjectMapper().createObjectNode()
            .put("event.detail.query", "chosen")
            .put(DomEventFilter.TableSortKey.value, true)

        component._fireDomEvent("header-input", data)

        assertEquals("chosen", query)
    }
}
