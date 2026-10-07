@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.display

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._fireDomEvent
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.internal.nodefeature.ElementListenerMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.child
import tech.testsys.web.components.core.DomEventFilter
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.find
import tech.testsys.web.components.pendingJavaScript
import tools.jackson.databind.ObjectMapper

internal class ContestCardTests : MockVaadinTests() {
    @Test
    fun `should separate card and CTA callbacks`() {
        var cardClicks = 0
        var actionClicks = 0
        val root = buildTestContent {
            contestCard(card(actionLabel = "Join")) {
                onClick { cardClicks++ }
                onAction { actionClicks++ }
            }
        }
        val button = requireNotNull(root.find("ts-btn") as? NativeButton)

        button._click()

        assertEquals(1, actionClicks)
        assertEquals(0, cardClicks)
    }

    @Test
    fun `should call onClick when the card is clicked`() {
        var cardClicks = 0
        val card = buildTestContent { contestCard(card()) { onClick { cardClicks++ } } }.find("ts-ccard")

        card._fireDomEvent("click", filterPassed(DomEventFilter.CardClick))

        assertEquals(1, cardClicks)
    }

    @Test
    fun `should call onClick when Enter or Space is pressed on the card`() {
        var cardClicks = 0
        val card = buildTestContent { contestCard(card()) { onClick { cardClicks++ } } }.find("ts-ccard")

        card._fireDomEvent("keydown", filterPassed(DomEventFilter.CardKey))

        assertEquals(1, cardClicks)
    }

    @Test
    fun `should prevent the default key action through the filtered keydown listener`() {
        val card = buildTestContent { contestCard(card()) { onClick {} } }.find("ts-ccard")

        val expressions = card.element.node.getFeature(ElementListenerMap::class.java).getExpressions("keydown")

        assertTrue("(${DomEventFilter.CardKey.value}) && event.preventDefault()" in expressions)
        assertTrue(pendingJavaScript().none { call -> "preventDefault" in call.invocation.expression })
    }

    @Test
    fun `should make the card a focusable link when onClick is set`() {
        val card = buildTestContent { contestCard(card()) { onClick {} } }.find("ts-ccard")

        assertEquals("link", card.element.getAttribute("role"))
        assertEquals("0", card.element.getAttribute("tabindex"))
    }

    @Test
    fun `should keep the card out of the tab order without onClick`() {
        val card = buildTestContent { contestCard(card()) }.find("ts-ccard")

        assertNull(card.element.getAttribute("role"))
        assertNull(card.element.getAttribute("tabindex"))
    }

    @Test
    fun `should hide the call to action without an action label`() {
        val root = buildTestContent { contestCard(card(actionLabel = null)) }

        assertFalse(root.find("ts-btn").isVisible)
    }

    @Test
    fun `should place the card on the requested columns of a slot row`() {
        val main = buildTestPage { row { slot(size = 16) { row { contestCard(card(), size = 8) } } } }

        val card = main.child(0).child(0).child(0).child(0)
        assertTrue("ts-ccard" in card.element.classList)
        assertEquals("span 8", card.element.style.get("grid-column"))
    }

    private fun card(actionLabel: String? = "Join") = ContestCardData(
        title = "Title",
        format = "Format",
        status = "Status",
        tone = Tone.Info,
        whenText = "Today",
        actionLabel = actionLabel,
    )

    private fun filterPassed(filter: DomEventFilter) = ObjectMapper().createObjectNode().put(filter.value, true)
}
