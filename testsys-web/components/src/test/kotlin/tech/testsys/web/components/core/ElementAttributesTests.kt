@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.core

import com.vaadin.flow.dom.Element
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

internal class ElementAttributesTests {
    @Test
    fun `should retain explicit false when setting an aria state`() {
        val element = Element("div")

        element.setAriaBusy(false)

        assertEquals("false", element.getAttribute("aria-busy"))
    }

    @Test
    fun `should remove an aria state when its value becomes absent`() {
        val element = Element("div").apply { setAttribute("aria-busy", "true") }

        element.setAriaBusy(null)

        assertFalse(element.hasAttribute("aria-busy"))
    }

    @Test
    fun `should serialize the hidden aria state as explicit true`() {
        val element = Element("div")

        element.setAriaHidden(true)

        assertEquals("true", element.getAttribute("aria-hidden"))
    }

    @Test
    fun `should remove spellcheck when disabling its legacy presence flag`() {
        val element = Element("textarea").apply { setAttribute("spellcheck", true) }

        element.setSpellcheckPresence(false)

        assertFalse(element.hasAttribute("spellcheck"))
    }

    @Test
    fun `should remove the current location when navigation stops being current`() {
        val element = Element("a").apply { setAttribute("aria-current", "page") }

        element.setAriaCurrent(null)

        assertFalse(element.hasAttribute("aria-current"))
    }
}
