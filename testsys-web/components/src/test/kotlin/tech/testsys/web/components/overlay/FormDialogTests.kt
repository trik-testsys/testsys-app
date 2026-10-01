package tech.testsys.web.components.overlay

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._setValue
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.button
import tech.testsys.web.components.child
import tech.testsys.web.components.classes
import tech.testsys.web.components.control
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.openDialogs
import tech.testsys.web.components.testTexts

class FormDialogTests : MockVaadinTests() {
    @Test
    fun `should bind edit mode before opening and preserve own read-only fields`() {
        val editable = ValueSignal(false)
        val handle = dialog(title = "Тур") {
            row { textInput("Название", labelSize = 4, size = 8) }
            row { textInput("Код", labelSize = 4, size = 8) { isEditable = false } }
        }
        handle.bindEditable(editable)
        handle.open()
        openDialogs()
        assertTrue(control<TextField>("Название").isReadOnly)

        editable.set(true)

        assertFalse(control<TextField>("Название").isReadOnly)
        assertTrue(control<TextField>("Код").isReadOnly)
    }

    @Test
    fun `should reject manual mode and second binding while dialog mode is bound`() {
        val handle = dialog(title = "Тур") {}
        handle.bindEditable(ValueSignal(false))

        assertThrows(BindingActiveException::class.java) { handle.isEditable = true }
        assertThrows(BindingActiveException::class.java) { handle.bindEditable(ValueSignal(true)) }
    }

    @Test
    fun `should apply current signal on reopening without resetting values`() {
        val editable = ValueSignal(true)
        val handle = dialog(title = "Тур") { row { textInput("Название", labelSize = 4, size = 8) } }
        handle.bindEditable(editable)
        handle.open()
        openDialogs()
        control<TextField>("Название")._setValue("Весенний кубок")
        handle.close()
        openDialogs()
        editable.set(false)

        handle.open()

        openDialogs()
        assertTrue(control<TextField>("Название").isReadOnly)
        assertEquals("Весенний кубок", control<TextField>("Название").value)
    }

    @BeforeEach
    fun setUpPage() {
        buildTestPage {}
    }

    @Test
    fun `should build a wide dialog without opening it`() {
        val handle = dialog(title = "Новый тур") { row { textInput("Название", labelSize = 4, size = 8) } }

        assertTrue(openDialogs().isEmpty())
        assertFalse(handle.isOpen)
    }

    @Test
    fun `should open and close through the handle`() {
        val handle = dialog(title = "Новый тур") { row { textInput("Название", labelSize = 4, size = 8) } }

        handle.open()

        val card = openDialogs().single().find("ts-dialog")
        assertTrue("ts-dialog--md" in card.classes())
        assertEquals("Новый тур", card.find("ts-dialog__title").element.text)
        assertTrue(handle.isOpen)
    }

    @Test
    fun `should hide the dialog when closed through the handle`() {
        val handle = dialog(title = "Новый тур") { row { textInput("Название", labelSize = 4, size = 8) } }
        handle.open()
        openDialogs()

        handle.close()

        assertTrue(openDialogs().isEmpty())
        assertFalse(handle.isOpen)
    }

    @Test
    fun `should lay rows on the twelve columns grid`() {
        val handle = dialog(title = "Новый тур") { row { vertical {} } }
        handle.open()

        val grid = openDialogs().single().find("ts-dialog__grid")
        assertEquals("span 12", grid.find("ts-block__row").child(0).element.style.get("grid-column"))
    }

    @Test
    fun `should reject a row wider than twelve columns`() {
        assertThrows(IllegalStateException::class.java) {
            dialog(title = "Новый тур") {
                row {
                    textInput("Название", labelSize = 4, size = 4)
                    textInput("Код", labelSize = 3, size = 2)
                }
            }
        }
    }

    @Test
    fun `should skip empty rows`() {
        val handle = dialog(title = "Новый тур") {
            row {}
            row { textInput("Название", labelSize = 4, size = 8) }
        }
        handle.open()

        assertEquals(1, openDialogs().single().findAll("ts-block__row").size)
    }

    @Test
    fun `should close from a footer action`() {
        val handle = dialog(title = "Новый тур") {
            row { textInput("Название", labelSize = 4, size = 8) }
            footer { dialog -> action("Отмена") { onClick { dialog.close() } } }
        }
        handle.open()
        openDialogs()

        button("Отмена")._click()

        assertTrue(openDialogs().isEmpty())
        assertFalse(handle.isOpen)
    }

    @Test
    fun `should notify every close`() {
        var closes = 0
        val handle = dialog(title = "Новый тур") { row { textInput("Название", labelSize = 4, size = 8) } }
        handle.onClose { closes++ }
        handle.open()
        openDialogs()
        handle.close()
        handle.open()
        openDialogs()

        _find<Button>().single { button -> button.ariaLabel.orElse(null) == testTexts.dialog.close }._click()

        assertEquals(2, closes)
    }

    @Test
    fun `should make its fields read-only when not editable`() {
        val handle = dialog(title = "Новый тур") { row { textInput("Название", labelSize = 4, size = 8) } }
        handle.open()
        openDialogs()

        handle.isEditable = false

        assertTrue(control<TextField>("Название").isReadOnly)
        assertFalse(handle.isEditable)
    }

    @Test
    fun `should keep field values between openings`() {
        val handle = dialog(title = "Новый тур") { row { textInput("Название", labelSize = 4, size = 8) } }
        handle.open()
        openDialogs()
        control<TextField>("Название")._setValue("Весенний кубок")
        handle.close()
        openDialogs()

        handle.open()

        openDialogs()
        assertEquals("Весенний кубок", control<TextField>("Название").value)
    }

    @Test
    fun `should show the subtitle under the title`() {
        val handle = dialog(title = "Новый тур", subtitle = "Черновик") { row { textInput("Название", labelSize = 4, size = 8) } }

        handle.open()

        assertEquals("Черновик", openDialogs().single().find("ts-block__sub").element.text)
    }
}
