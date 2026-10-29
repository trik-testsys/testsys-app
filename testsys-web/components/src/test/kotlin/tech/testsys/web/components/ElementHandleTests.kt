package tech.testsys.web.components

import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.display.text

class ElementHandleTests : MockVaadinTests() {
    @Nested
    inner class BindVisibleTests {
        @Test
        fun `should apply the signal value at once`() {
            lateinit var handle: TextHandle
            buildTestContent { handle = text("Текст") }

            handle.bindVisible(ValueSignal(false))

            assertFalse(handle.isVisible)
        }

        @Test
        fun `should follow the signal while attached`() {
            lateinit var handle: TextHandle
            buildTestContent { handle = text("Текст") }
            val signal = ValueSignal(true)
            handle.bindVisible(signal)

            signal.set(false)

            assertFalse(handle.isVisible)
        }

        @Test
        fun `should reject a manual value while bound`() {
            lateinit var handle: TextHandle
            buildTestContent { handle = text("Текст") }
            handle.bindVisible(ValueSignal(true))

            assertThrows<BindingActiveException> { handle.isVisible = false }
        }

        @Test
        fun `should reject a second binding`() {
            lateinit var handle: TextHandle
            buildTestContent { handle = text("Текст") }
            handle.bindVisible(ValueSignal(true))

            assertThrows<BindingActiveException> { handle.bindVisible(ValueSignal(false)) }
        }
    }

    @Nested
    inner class BindTextTests {
        @Test
        fun `should apply the signal value at once`() {
            lateinit var handle: TextHandle
            buildTestContent { handle = text("Изначально") }

            handle.bindText(ValueSignal("Новый текст"))

            assertEquals("Новый текст", handle.text)
        }

        @Test
        fun `should follow the signal while attached`() {
            lateinit var handle: TextHandle
            buildTestContent { handle = text("Изначально") }
            val signal = ValueSignal("Один")
            handle.bindText(signal)

            signal.set("Два")

            assertEquals("Два", handle.text)
        }

        @Test
        fun `should reject a manual value while bound`() {
            lateinit var handle: TextHandle
            buildTestContent { handle = text("Изначально") }
            handle.bindText(ValueSignal("Один"))

            assertThrows<BindingActiveException> { handle.text = "Другое" }
        }

        @Test
        fun `should reject a second binding`() {
            lateinit var handle: TextHandle
            buildTestContent { handle = text("Изначально") }
            handle.bindText(ValueSignal("Один"))

            assertThrows<BindingActiveException> { handle.bindText(ValueSignal("Два")) }
        }
    }
}
