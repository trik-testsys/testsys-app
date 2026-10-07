package tech.testsys.web.components

import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.testsys.web.components.display.verdict

class DataHandleTests : MockVaadinTests() {
    @Test
    fun `should show the value of the bound signal`() {
        lateinit var handle: DataHandle<Double>
        val root = buildTestContent { handle = verdict(1.0) }

        handle.bindData(ValueSignal(7.0))

        assertEquals(7.0, handle.data)
        assertEquals("7", root.find("ts-verdict").element.textRecursively)
    }

    @Test
    fun `should follow a new value of the bound signal`() {
        lateinit var handle: DataHandle<Double>
        val root = buildTestContent { handle = verdict(1.0) }
        val signal = ValueSignal(7.0)
        handle.bindData(signal)

        signal.set(9.0)

        assertEquals("9", root.find("ts-verdict").element.textRecursively)
    }

    @Test
    fun `should reject manual data while bound`() {
        lateinit var handle: DataHandle<Double>
        buildTestContent { handle = verdict(1.0) }
        handle.bindData(ValueSignal(7.0))

        assertThrows(BindingActiveException::class.java) { handle.data = 2.0 }
    }
}
