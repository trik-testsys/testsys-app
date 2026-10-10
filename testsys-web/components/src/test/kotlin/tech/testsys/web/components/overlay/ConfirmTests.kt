package tech.testsys.web.components.overlay

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._setValue
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.button
import tech.testsys.web.components.child
import tech.testsys.web.components.classes
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll
import tech.testsys.web.components.openDialogs
import tech.testsys.web.components.testTexts

class ConfirmTests : MockVaadinTests() {
    @Nested
    inner class OnPageTests {
        @BeforeEach
        fun setUpPage() {
            buildTestPage {}
        }

        @Test
        fun `should accept a name stored with trailing whitespace`() {
            var confirmations = 0
            confirm(title = "Удалить тур?", action = "Удалить", typeToConfirm = "Весенний кубок ") { confirmations++ }
            val dialog = openDialogs().single()
            assertFalse(button("Удалить").isEnabled)

            dialog._get<TextField>()._setValue("Весенний кубок")
            button("Удалить")._click()

            assertEquals(1, confirmations)
        }

        @Test
        fun `should open a narrow dialog with the title, text and actions`() {
            confirm(title = "Закрыть тур?", text = "Посылки больше не принимаются", action = "Закрыть тур") {}

            val dialog = openDialogs().single()
            val card = dialog.find("ts-dialog")
            assertFalse("ts-dialog--md" in card.classes())
            assertEquals("Закрыть тур?", card.find("ts-dialog__title").element.text)
            assertEquals("Посылки больше не принимаются", card.find("ts-dialog__body").child(0).child(0).element.text)
            assertTrue(button(testTexts.dialog.cancel).isVisible)
            assertTrue(button("Закрыть тур").isEnabled)
        }

        @Test
        fun `should run the action and close`() {
            var confirmations = 0
            confirm(title = "Закрыть тур?", action = "Закрыть тур") { confirmations++ }
            openDialogs()

            button("Закрыть тур")._click()

            assertEquals(1, confirmations)
            assertTrue(openDialogs().isEmpty())
        }

        @Test
        fun `should close on cancel without acting`() {
            var confirmations = 0
            confirm(title = "Закрыть тур?", action = "Закрыть тур") { confirmations++ }
            openDialogs()

            button(testTexts.dialog.cancel)._click()

            assertEquals(0, confirmations)
            assertTrue(openDialogs().isEmpty())
        }

        @Test
        fun `should stay open when the action fails`() {
            confirm(title = "Закрыть тур?", action = "Закрыть тур") { error("Тур уже закрыт") }
            openDialogs()

            assertThrows(IllegalStateException::class.java) { button("Закрыть тур")._click() }

            assertEquals(1, openDialogs().size)
        }

        @Test
        fun `should close from the head close button without acting`() {
            var confirmations = 0
            confirm(title = "Закрыть тур?", action = "Закрыть тур") { confirmations++ }
            openDialogs()

            _find<Button>().single { button -> button.ariaLabel.orElse(null) == testTexts.dialog.close }._click()

            assertEquals(0, confirmations)
            assertTrue(openDialogs().isEmpty())
        }

        @Test
        fun `should announce the confirmation as one dialog named by its title`() {
            confirm(title = "Закрыть тур?", action = "Закрыть тур") {}

            val dialog = openDialogs().single()
            assertEquals("dialog", dialog.ariaRole.orElse(null))
            assertEquals("Закрыть тур?", dialog.element.getProperty("ariaLabel"))
            assertFalse(dialog.find("ts-dialog").element.hasAttribute("role"))
        }

        @Test
        fun `should announce a dangerous confirmation as an alert dialog named by its title`() {
            confirm(title = "Удалить тур?", action = "Удалить", isDanger = true) {}

            val dialog = openDialogs().single()
            assertEquals("alertdialog", dialog.ariaRole.orElse(null))
            assertEquals("Удалить тур?", dialog.element.getProperty("ariaLabel"))
            assertFalse(dialog.find("ts-dialog").element.hasAttribute("role"))
        }

        @Test
        fun `should make a dangerous confirmation an alert with a danger action`() {
            confirm(title = "Удалить тур?", action = "Удалить", isDanger = true) {}

            val card = openDialogs().single().find("ts-dialog")
            assertTrue("ts-dialog--alert" in card.classes())
            assertEquals(1, card.findAll("ts-dialog__glyph").size)
            assertTrue(card.findAll("ts-dialog__head").isEmpty())
            assertEquals("danger", button("Удалить").element.getAttribute("data-ts-role"))
        }

        @Test
        fun `should make the main action the default role`() {
            confirm(title = "Закрыть тур?", action = "Закрыть тур") {}
            openDialogs()

            assertEquals("main", button("Закрыть тур").element.getAttribute("data-ts-role"))
        }

        @ParameterizedTest
        @CsvSource("'Весенний кубок', true", "'  Весенний кубок  ', true", "'весенний кубок', false", "'Весенний', false", "'', false")
        fun `should enable the action only when the typed name matches`(typed: String, isEnabled: Boolean) {
            confirm(title = "Удалить тур?", action = "Удалить", isDanger = true, typeToConfirm = "Весенний кубок") {}
            val dialog = openDialogs().single()

            dialog._get<TextField>()._setValue(typed)

            assertEquals(isEnabled, button("Удалить").isEnabled)
        }

        @Test
        fun `should focus the name field when the dialog opens`() {
            confirm(title = "Удалить тур?", action = "Удалить", isDanger = true, typeToConfirm = "Весенний кубок") {}

            assertTrue(openDialogs().single()._get<TextField>().isAutofocus)
        }

        @Test
        fun `should prompt with the name to type`() {
            confirm(title = "Удалить тур?", action = "Удалить", isDanger = true, typeToConfirm = "Весенний кубок") {}

            val field = openDialogs().single()._get<TextField>()
            assertEquals(testTexts.dialog.typeToConfirm("Весенний кубок"), field.label)
        }
    }

    @Test
    fun `should reject opening without a page`() {
        assertThrows(IllegalStateException::class.java) {
            confirm(title = "Закрыть тур?", action = "Закрыть тур") {}
        }
    }
}
