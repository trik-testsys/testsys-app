package tech.testsys.web.ui.actions

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.ui.MockVaadinTests
import tech.testsys.web.ui.buildTestContent
import tech.testsys.web.ui.buildTestPage
import tech.testsys.web.ui.core.IconName
import tech.testsys.web.ui.data.Page
import tech.testsys.web.ui.data.table

class ActionTests : MockVaadinTests() {
    @Nested
    inner class RoleTests {
        @Test
        fun `should mark each action with its role`() {
            buildTestContent {
                mainAction("Главное")
                action("Обычное")
                destructiveAction("Удалить")
                linkAction("Подробнее")
            }

            assertEquals("main", role("Главное"))
            assertEquals("neutral", role("Обычное"))
            assertEquals("destructive", role("Удалить"))
            assertEquals("link", role("Подробнее"))
        }

        @Test
        fun `should label icon-only action for screen readers`() {
            buildTestContent { iconAction(IconName.Trash, "Удалить задачу") }

            val button = _find<Button>().single { button -> button.element.hasAttribute("data-ts-icon-only") }
            assertEquals("Удалить задачу", button.ariaLabel.orElseThrow())
        }

        private fun role(label: String): String = _get<Button> { text = label }.element.getAttribute("data-ts-role")
    }

    @Nested
    inner class SizeTests {
        @Test
        fun `should render actions in the block head small`() {
            buildTestPage { block(title = "Задачи") { actions { action("Фильтр") } } }

            assertEquals("sm", _get<Button> { text = "Фильтр" }.element.getAttribute("data-ts-size"))
        }

        @Test
        fun `should render actions in the body and footer medium`() {
            buildTestPage {
                block {
                    row { horizontal { action("В теле") } }
                    footer { mainAction("В подвале") }
                }
            }

            assertEquals("md", _get<Button> { text = "В теле" }.element.getAttribute("data-ts-size"))
            assertEquals("md", _get<Button> { text = "В подвале" }.element.getAttribute("data-ts-size"))
        }

        @Test
        fun `should render actions in table cells small`() {
            buildTestPage {
                block {
                    table(key = { id: Int -> id }, fetch = { Page(listOf(1), total = 1) }) { column("Посылка") { action("Открыть") } }
                }
            }

            assertEquals("sm", _get<Button> { text = "Открыть" }.element.getAttribute("data-ts-size"))
        }
    }

    @Nested
    inner class StateTests {
        @Test
        fun `should disable action through its handle`() {
            buildTestContent { mainAction("Отправить") { isEnabled = false } }

            assertFalse(_get<Button> { text = "Отправить" }.isEnabled)
        }

        @Test
        fun `should hide action through its handle`() {
            lateinit var handle: ActionHandle
            buildTestContent { handle = mainAction("Отправить") }
            val button = _get<Button> { text = "Отправить" }

            handle.isVisible = false

            assertFalse(button.isVisible)
        }

        @Test
        fun `should run click listener`() {
            var clicks = 0
            buildTestContent { mainAction("Отправить") { onClick { clicks++ } } }

            _get<Button> { text = "Отправить" }._click()

            assertEquals(1, clicks)
        }

        @Test
        fun `should ignore clicks and show spinner while loading`() {
            var clicks = 0
            buildTestContent {
                mainAction("Отправить", icon = IconName.Upload) {
                    isLoading = true
                    onClick { clicks++ }
                }
            }
            val button = _get<Button> { text = "Отправить" }

            button._click()

            assertEquals(0, clicks)
            assertEquals("true", button.element.getAttribute("aria-busy"))
            assertTrue("ts-spinner" in button.icon.element.classList)
        }

        @Test
        fun `should restore icon when loading ends`() {
            lateinit var handle: ActionHandle
            buildTestContent { handle = mainAction("Отправить", icon = IconName.Upload) { isLoading = true } }

            handle.isLoading = false

            val button = _get<Button> { text = "Отправить" }
            assertTrue("ts-icon" in button.icon.element.classList)
            assertNull(button.element.getAttribute("aria-busy"))
        }
    }

    @Nested
    inner class BindingTests {
        @Test
        fun `should apply the enabled signal value at once`() {
            buildTestContent { mainAction("Отправить") { bindEnabled(ValueSignal(false)) } }

            assertFalse(_get<Button> { text = "Отправить" }.isEnabled)
        }

        @Test
        fun `should follow the enabled signal while attached`() {
            lateinit var handle: ActionHandle
            buildTestContent { handle = mainAction("Отправить") }
            val signal = ValueSignal(true)
            handle.bindEnabled(signal)

            signal.set(false)

            assertFalse(_get<Button> { text = "Отправить" }.isEnabled)
        }

        @Test
        fun `should reject a manual enabled value while bound`() {
            lateinit var handle: ActionHandle
            buildTestContent { handle = mainAction("Отправить") }
            handle.bindEnabled(ValueSignal(true))

            assertThrows<BindingActiveException> { handle.isEnabled = false }
        }

        @Test
        fun `should reject a second enabled binding`() {
            lateinit var handle: ActionHandle
            buildTestContent { handle = mainAction("Отправить") }
            handle.bindEnabled(ValueSignal(true))

            assertThrows<BindingActiveException> { handle.bindEnabled(ValueSignal(false)) }
        }

        @Test
        fun `should show spinner and mark busy from the loading signal at once`() {
            buildTestContent { mainAction("Отправить", icon = IconName.Upload) { bindLoading(ValueSignal(true)) } }

            val button = _get<Button> { text = "Отправить" }
            assertTrue("ts-spinner" in button.icon.element.classList)
            assertEquals("true", button.element.getAttribute("aria-busy"))
        }

        @Test
        fun `should restore icon and clear busy when the loading signal turns false`() {
            lateinit var handle: ActionHandle
            buildTestContent { handle = mainAction("Отправить", icon = IconName.Upload) }
            val signal = ValueSignal(true)
            handle.bindLoading(signal)

            signal.set(false)

            val button = _get<Button> { text = "Отправить" }
            assertTrue("ts-icon" in button.icon.element.classList)
            assertNull(button.element.getAttribute("aria-busy"))
        }

        @Test
        fun `should report loading values of the bound signal to change callbacks`() {
            lateinit var handle: ActionHandle
            buildTestContent { handle = mainAction("Отправить") }
            val signal = ValueSignal(true)
            val reported = mutableListOf<Pair<Boolean, Boolean>>()
            handle.bindLoading(signal).onChange { context -> reported += context.oldValue to context.newValue }

            signal.set(false)

            assertEquals(listOf(true to true, true to false), reported)
        }

        @Test
        fun `should ignore clicks while loading from the signal`() {
            var clicks = 0
            lateinit var handle: ActionHandle
            buildTestContent { handle = mainAction("Отправить") { onClick { clicks++ } } }
            handle.bindLoading(ValueSignal(true))

            _get<Button> { text = "Отправить" }._click()

            assertEquals(0, clicks)
        }

        @Test
        fun `should reject a manual loading value while bound`() {
            lateinit var handle: ActionHandle
            buildTestContent { handle = mainAction("Отправить") }
            handle.bindLoading(ValueSignal(true))

            assertThrows<BindingActiveException> { handle.isLoading = false }
        }

        @Test
        fun `should reject a second loading binding`() {
            lateinit var handle: ActionHandle
            buildTestContent { handle = mainAction("Отправить") }
            handle.bindLoading(ValueSignal(true))

            assertThrows<BindingActiveException> { handle.bindLoading(ValueSignal(false)) }
        }
    }
}
