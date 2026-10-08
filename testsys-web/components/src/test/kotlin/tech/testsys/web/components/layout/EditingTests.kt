package tech.testsys.web.components.layout

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.button
import tech.testsys.web.components.control
import tech.testsys.web.components.find
import tech.testsys.web.components.findAllButtons
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.pendingJavaScript

class EditingTests : MockVaadinTests() {
    private var saves = 0
    private var cancels = 0

    @Test
    fun `should request the client focus search from the cancel action after starting editing`() {
        buildEditingBlock()
        pendingJavaScript()

        button("Изменить")._click()

        val cancel = button("Отменить")
        assertTrue(pendingJavaScript().any { call -> call.owner == cancel.element.node && FOCUS_SEARCH in call.invocation.expression })
    }

    @Test
    fun `should not request focus when mode changes programmatically`() {
        val handle = buildEditingBlock()
        pendingJavaScript()

        handle.isEditable = true

        assertTrue(pendingJavaScript().none { call -> FOCUS_SEARCH in call.invocation.expression })
    }

    @Test
    fun `should request fallback focus even when block body is detached`() {
        buildTestPage { row { block { editing(onSave = { true }, onCancel = {}) } } }
        pendingJavaScript()

        button("Изменить")._click()

        val cancel = button("Отменить")
        assertTrue(pendingJavaScript().any { call -> call.owner == cancel.element.node && FOCUS_SEARCH in call.invocation.expression })
    }

    @Test
    fun `should not request focus when mode follows a signal`() {
        val editable = ValueSignal(false)
        buildTestPage {
            row { block { row { textInput("Логин", labelSize = 4, size = 20) } }.bindEditable(editable) }
        }
        pendingJavaScript()

        editable.set(true)

        assertTrue(pendingJavaScript().none { call -> FOCUS_SEARCH in call.invocation.expression })
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
            row {
                block(title = "Профиль") {
                    editing(onSave = { true }, onCancel = {})
                    actions { action("Отфильтровать") }
                }
            }
        }.find("ts-block__actions")

        val labels = findAllButtons(bar).map { button -> button.text }
        assertEquals(listOf("Отфильтровать", "Изменить", "Отменить", "Сохранить"), labels)
    }

    @Test
    fun `should add a head with the switch to a block without a title`() {
        val page = buildTestPage { row { block { editing(onSave = { true }, onCancel = {}) } } }

        val labels = findAllButtons(page.find("ts-block__head")).map { button -> button.text }
        assertEquals(listOf("Изменить", "Отменить", "Сохранить"), labels)
    }

    @Test
    fun `should reject a second editing switch`() {
        assertThrows<IllegalStateException> {
            buildTestPage {
                row {
                    block {
                        editing(onSave = { true }, onCancel = {})
                        editing(onSave = { true }, onCancel = {})
                    }
                }
            }
        }
    }

    @Test
    fun `should reject a signal binding of a block with an editing switch`() {
        val handle = buildEditingBlock()

        val error = assertThrows<IllegalStateException> { handle.bindEditable(ValueSignal(true)) }

        assertTrue("editing switch" in error.message.orEmpty())
    }

    private fun buildEditingBlock(isSaved: Boolean = true): BlockHandle {
        lateinit var handle: BlockHandle
        buildTestPage {
            row {
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
        }
        return handle
    }

    private companion object {
        const val FOCUS_SEARCH = "window.testsysEditingFocus.focusFirst(this)"
    }
}
