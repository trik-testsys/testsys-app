package tech.testsys.web.components.forms

import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._setValue
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.dom.Element
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.control
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.table
import tech.testsys.web.components.findAll
import tech.testsys.web.components.layout.ContentScope

class ChoiceTests : MockVaadinTests() {
    private enum class Language { Kotlin, Python }

    @Test
    fun `should offer all select items`() {
        buildTestRow { select("Язык", labelSize = 4, size = 20, Language.entries, itemLabel = { language -> language.name }) }

        assertEquals(2, control<Select<*>>("Язык").listDataView.itemCount)
    }

    @Test
    fun `should report selected item through the handle`() {
        lateinit var input: ValueInput<Language?>
        buildTestRow { input = select("Язык", labelSize = 4, size = 20, Language.entries, itemLabel = { language -> language.name }) }

        input.value = Language.Kotlin

        assertEquals(Language.Kotlin, input.value)
    }

    @Test
    fun `should report checkbox state through the handle`() {
        lateinit var input: ValueInput<Boolean>
        buildTestRow { input = checkbox("Открытый тур", labelSize = 4, size = 20) }

        control<Checkbox>("Открытый тур")._setValue(true)

        assertTrue(input.value)
    }

    @Nested
    inner class LabelLessSelectTests {
        @Test
        fun `should render the select in the block head small`() {
            buildTestPage { row { block(title = "Посылки") { actions { languageSelect() } } } }

            assertEquals("sm", selectControl().element.getAttribute("data-ts-size"))
        }

        @Test
        fun `should render the select in a table cell small`() {
            buildTestPage {
                row {
                    block {
                        table(key = { id: Int -> id }, fetch = { Page(listOf(1), total = 1) }) { column("Язык") { languageSelect() } }
                    }
                }
            }

            assertEquals("sm", selectControl().element.getAttribute("data-ts-size"))
        }

        @Test
        fun `should render the select in the block body medium`() {
            buildTestContent { languageSelect() }

            assertEquals("md", selectControl().element.getAttribute("data-ts-size"))
        }

        @Test
        fun `should name the select by its label and show the label as the placeholder`() {
            buildTestContent { languageSelect() }

            assertEquals("Язык", selectControl().ariaLabel.orElseThrow())
            assertEquals("Язык", selectControl().placeholder)
        }

        @Test
        fun `should render no visible label`() {
            buildTestContent { languageSelect() }

            assertNull(selectControl().label)
            assertTrue(UI.getCurrent().findAll("ts-field").isEmpty())
        }

        @Test
        fun `should build a labelled field if select is called directly in a block row`() {
            buildTestRow { select("Язык", labelSize = 4, size = 20, Language.entries, itemLabel = { language -> language.name }) }

            assertEquals(1, UI.getCurrent().findAll("ts-field").size)
            assertNull(control<Select<*>>("Язык").element.getAttribute("data-ts-size"))
        }

        @Test
        fun `should offer the items by their labels`() {
            buildTestContent { languageSelect() }

            assertEquals(2, selectControl().listDataView.itemCount)
            assertEquals("kotlin", selectControl().itemLabelGenerator.apply(Language.Kotlin))
        }

        @Test
        fun `should offer an empty item named by emptyLabel`() {
            buildTestContent { languageSelect(emptyLabel = "Все языки") }

            assertEquals(listOf("Все языки", "kotlin", "python"), renderedItems().map { item -> item.text })
        }

        @Test
        fun `should offer no empty item without emptyLabel`() {
            buildTestContent { languageSelect() }

            assertEquals(listOf("kotlin", "python"), renderedItems().map { item -> item.text })
        }

        @Test
        fun `should keep the label as the placeholder of the closed select with an empty item`() {
            buildTestContent { languageSelect(emptyLabel = "Все языки") }

            assertEquals("Язык", selectControl().placeholder)
            assertEquals("", renderedItems().first().getAttribute("label"))
        }

        @Test
        fun `should report a null value when the user chooses the empty item`() {
            lateinit var input: ValueInput<Language?>
            buildTestContent { input = languageSelect(emptyLabel = "Все языки") }
            input.value = Language.Kotlin
            val changes = mutableListOf<Pair<Language?, Boolean>>()
            input.addValueChangeListener { event -> changes += event.value to event.isFromClient }

            selectControl()._setValue(null)

            assertEquals(listOf(null to true), changes)
            assertNull(input.value)
        }

        @Test
        fun `should report a user choice as a change from the client`() {
            lateinit var input: ValueInput<Language?>
            buildTestContent { input = languageSelect() }
            val changes = mutableListOf<Pair<Language?, Boolean>>()
            input.addValueChangeListener { event -> changes += event.value to event.isFromClient }

            selectControl()._setValue(Language.Python)

            assertEquals(listOf(Language.Python to true), changes)
            assertEquals(Language.Python, input.value)
        }

        @Test
        fun `should show the value of the bound signal`() {
            lateinit var input: ValueInput<Language?>
            buildTestContent { input = languageSelect() }
            val signal = ValueSignal<Language?>(Language.Kotlin)
            input.bindValue(signal) { value -> signal.set(value) }

            signal.set(Language.Python)

            assertEquals(Language.Python, selectControl().value)
        }

        @Test
        fun `should write a user choice to the bound signal through the callback`() {
            lateinit var input: ValueInput<Language?>
            buildTestContent { input = languageSelect() }
            val signal = ValueSignal<Language?>(null)
            input.bindValue(signal) { value -> signal.set(value) }

            selectControl()._setValue(Language.Kotlin)

            assertEquals(Language.Kotlin, signal.peek())
        }

        @Test
        fun `should show the required indicator of the control`() {
            buildTestContent { languageSelect { isRequiredIndicatorVisible = true } }

            assertTrue(selectControl().isRequiredIndicatorVisible)
        }

        @Test
        fun `should mark the control invalid with the error message`() {
            buildTestContent {
                languageSelect {
                    errorMessage = "Выберите язык"
                    isInvalid = true
                }
            }

            assertTrue(selectControl().isInvalid)
            assertEquals("Выберите язык", selectControl().errorMessage)
        }

        @Test
        fun `should make the control read-only if the select is not editable`() {
            buildTestContent { languageSelect { isEditable = false } }

            assertTrue(selectControl().isReadOnly)
        }

        @Test
        fun `should disable the control through the handle`() {
            buildTestContent { languageSelect { isEnabled = false } }

            assertFalse(selectControl().isEnabled)
        }

        @Test
        fun `should hide the select through the handle`() {
            lateinit var input: ValueInput<Language?>
            buildTestContent { input = languageSelect() }
            val control = selectControl()

            input.isVisible = false

            assertFalse(input.isVisible)
            assertFalse(control.parent.orElseThrow().isVisible)
        }

        private fun ContentScope.languageSelect(
            emptyLabel: String? = null,
            configure: ValueInput<Language?>.() -> Unit = {},
        ): ValueInput<Language?> =
            select("Язык", Language.entries, itemLabel = { language -> language.name.lowercase() }, emptyLabel, configure)

        private fun selectControl(): Select<Language?> = _get<Select<Language?>>()

        /** The items of the drop-down of the select as the browser gets them. */
        private fun renderedItems(): List<Element> = selectControl().element.children.toList()
            .single { child -> child.tag == "vaadin-select-list-box" }
            .children.toList()
    }
}
