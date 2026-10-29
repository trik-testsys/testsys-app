package tech.testsys.web.components.overlay

import com.vaadin.flow.component.UI
import com.vaadin.flow.dom.Element
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.display.text

internal class TooltipTests : MockVaadinTests() {
    @ParameterizedTest
    @CsvSource("Top,top", "Bottom,bottom", "Left,start", "Right,end")
    fun `should place the tooltip on the requested side of its target`(placement: TooltipPlacement, position: String) {
        buildTestContent { text("Цель").tooltip("Подсказка", placement) }

        assertEquals(position, tooltipElement().getProperty("position"))
    }

    @Test
    fun `should show the initial text`() {
        lateinit var handle: TooltipHandle

        buildTestContent { handle = text("Цель").tooltip("Подсказка") }

        assertEquals("Подсказка", handle.text)
        assertEquals("Подсказка", tooltipElement().getProperty("text"))
    }

    @Test
    fun `should replace the text through the handle`() {
        lateinit var handle: TooltipHandle
        buildTestContent { handle = text("Цель").tooltip("Подсказка") }

        handle.text = "Новая подсказка"

        assertEquals("Новая подсказка", tooltipElement().getProperty("text"))
    }

    @Test
    fun `should follow a bound signal`() {
        lateinit var handle: TooltipHandle
        buildTestContent { handle = text("Цель").tooltip("Подсказка") }
        val signal = ValueSignal("Из сигнала")
        handle.bindText(signal)

        signal.set("Обновлено")

        assertEquals("Обновлено", tooltipElement().getProperty("text"))
        assertEquals("Обновлено", handle.text)
    }

    @Test
    fun `should reject a manual text while bound`() {
        lateinit var handle: TooltipHandle
        buildTestContent { handle = text("Цель").tooltip("Подсказка") }
        handle.bindText(ValueSignal("Из сигнала"))

        assertThrows<BindingActiveException> { handle.text = "Вручную" }
    }

    @Test
    fun `should reject a second binding`() {
        lateinit var handle: TooltipHandle
        buildTestContent { handle = text("Цель").tooltip("Подсказка") }
        handle.bindText(ValueSignal("Из сигнала"))

        assertThrows<BindingActiveException> { handle.bindText(ValueSignal("Ещё один")) }
    }

    private fun tooltipElement(): Element = UI.getCurrent().element.children.toList().single { child -> child.tag == "vaadin-tooltip" }
}
