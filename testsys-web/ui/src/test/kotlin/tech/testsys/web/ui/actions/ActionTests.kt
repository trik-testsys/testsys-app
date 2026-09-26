package tech.testsys.web.ui.actions

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.button.Button
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.web.ui.MockVaadinTests
import tech.testsys.web.ui.buildTestContent
import tech.testsys.web.ui.buildTestPage
import tech.testsys.web.ui.core.IconName

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
}
