package tech.testsys.web.ui.layout

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.ui.MockVaadinTests
import tech.testsys.web.ui.actions.action
import tech.testsys.web.ui.button
import tech.testsys.web.ui.buildTestPage
import tech.testsys.web.ui.control
import tech.testsys.web.ui.find
import tech.testsys.web.ui.forms.textInput

class EditingTests : MockVaadinTests() {
    private var saves = 0
    private var cancels = 0

    private fun buildEditingBlock(isSaved: Boolean = true): BlockHandle {
        lateinit var handle: BlockHandle
        buildTestPage {
            handle = block(title = "Профиль") {
                editing(
                    onSave = {
                        saves++
                        isSaved
                    },
                    onCancel = { cancels++ },
                )
                row { textInput("Логин", labelSize = 4, size = 20) }
                row { textInput("Создан", labelSize = 4, size = 20) { isEditable = false } }
            }
        }
        return handle
    }

    @Test
    fun `should open the block in view mode`() {
        buildEditingBlock()

        assertTrue(control<TextField>("Логин").isReadOnly)
        assertTrue(button("Изменить").isVisible)
        assertFalse(button("Сохранить").isVisible)
        assertFalse(button("Отменить").isVisible)
    }

    @Test
    fun `should start editing on the edit action`() {
        buildEditingBlock()

        button("Изменить")._click()

        assertFalse(control<TextField>("Логин").isReadOnly)
        assertFalse(button("Изменить").isVisible)
        assertTrue(button("Сохранить").isVisible)
        assertTrue(button("Отменить").isVisible)
    }

    @Test
    fun `should keep a field that is never editable read-only while editing`() {
        buildEditingBlock()

        button("Изменить")._click()

        assertTrue(control<TextField>("Создан").isReadOnly)
    }

    @Test
    fun `should return to view mode after saving`() {
        buildEditingBlock(isSaved = true)
        button("Изменить")._click()

        button("Сохранить")._click()

        assertEquals(1, saves)
        assertTrue(control<TextField>("Логин").isReadOnly)
        assertTrue(button("Изменить").isVisible)
    }

    @Test
    fun `should stay in edit mode when saving fails`() {
        buildEditingBlock(isSaved = false)
        button("Изменить")._click()

        button("Сохранить")._click()

        assertEquals(1, saves)
        assertFalse(control<TextField>("Логин").isReadOnly)
        assertTrue(button("Сохранить").isVisible)
    }

    @Test
    fun `should cancel and return to view mode`() {
        buildEditingBlock()
        button("Изменить")._click()

        button("Отменить")._click()

        assertEquals(1, cancels)
        assertEquals(0, saves)
        assertTrue(control<TextField>("Логин").isReadOnly)
    }

    @Test
    fun `should switch the mode and the actions from code`() {
        val handle = buildEditingBlock()

        handle.isEditable = true

        assertFalse(control<TextField>("Логин").isReadOnly)
        assertTrue(button("Сохранить").isVisible)
    }

    @Test
    fun `should put the switch after the head actions`() {
        val bar = buildTestPage {
            block(title = "Профиль") {
                editing(onSave = { true }, onCancel = {})
                actions { action("Фильтр") }
            }
        }.find("ts-block__actions")

        val labels = bar.children.toList().map { child -> child.element.textRecursively }
        assertEquals(listOf("Фильтр", "Изменить", "Отменить", "Сохранить"), labels)
    }

    @Test
    fun `should add a head to a block without a title`() {
        val page = buildTestPage { block { editing(onSave = { true }, onCancel = {}) } }

        page.find("ts-block__head")
    }

    @Test
    fun `should reject a second editing switch`() {
        assertThrows<IllegalStateException> {
            buildTestPage {
                block {
                    editing(onSave = { true }, onCancel = {})
                    editing(onSave = { true }, onCancel = {})
                }
            }
        }
    }
}
