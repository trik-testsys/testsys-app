package tech.testsys.web.ui.feedback

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.ui.ElementHandle
import tech.testsys.web.ui.MockVaadinTests
import tech.testsys.web.ui.actions.action
import tech.testsys.web.ui.buildTestPage
import tech.testsys.web.ui.button
import tech.testsys.web.ui.child
import tech.testsys.web.ui.classes
import tech.testsys.web.ui.core.IconName
import tech.testsys.web.ui.data.Page
import tech.testsys.web.ui.data.table
import tech.testsys.web.ui.display.text
import tech.testsys.web.ui.find
import tech.testsys.web.ui.findAll
import tech.testsys.web.ui.testTexts

class EmptyStateTests : MockVaadinTests() {
    @Test
    fun `should show the title with the default icon and nothing else`() {
        buildTestPage { block(title = "Посылки") { emptyState("Посылок пока нет") } }

        val state = ui().find("ts-empty")
        assertEquals("Посылок пока нет", state.find("ts-empty__title").element.text)
        val svg = state.find("ts-empty__icon").child(0).element.getProperty("innerHTML")
        assertTrue(svg.contains(IconName.File.paths))
        assertTrue(state.findAll("ts-empty__desc").isEmpty())
        assertTrue(state.findAll("ts-empty__actions").isEmpty())
        assertFalse("ts-empty--error" in state.classes())
    }

    @Test
    fun `should show the description and small actions`() {
        buildTestPage {
            block {
                emptyState("Посылок пока нет", description = "Отправьте решение", icon = IconName.Upload) {
                    action("К задачам")
                }
            }
        }

        assertEquals("Отправьте решение", ui().find("ts-empty__desc").element.text)
        assertEquals("sm", button("К задачам").element.getAttribute("data-ts-size"))
    }

    @Test
    fun `should draw an error state red`() {
        val state = buildEmptyState(EmptyContent("Сбой"), testTexts, isError = true)

        assertTrue("ts-empty--error" in state.classes())
        val svg = state.find("ts-empty__icon").child(0).element.getProperty("innerHTML")
        assertTrue(svg.contains(IconName.TriangleAlert.paths))
    }

    @Test
    fun `should take the whole body that is no longer a grid`() {
        buildTestPage { block { emptyState("Пусто") } }

        val body = ui().find("ts-block__body")
        assertFalse("ts-block__body--grid" in body.classes())
        assertFalse("ts-block__body--flush" in body.classes())
    }

    @Test
    fun `should hide the empty state through its handle`() {
        lateinit var handle: ElementHandle
        buildTestPage { block { handle = emptyState("Пусто") } }

        handle.isVisible = false

        assertFalse(ui().find("ts-empty").isVisible)
    }

    @Test
    fun `should reject rows after an empty state`() {
        assertThrows<IllegalStateException> {
            buildTestPage {
                block {
                    emptyState("Пусто")
                    row { text("x") }
                }
            }
        }
    }

    @Test
    fun `should reject an empty state after rows`() {
        assertThrows<IllegalStateException> {
            buildTestPage {
                block {
                    row { text("x") }
                    emptyState("Пусто")
                }
            }
        }
    }

    @Test
    fun `should reject a second empty state`() {
        assertThrows<IllegalStateException> {
            buildTestPage {
                block {
                    emptyState("Пусто")
                    emptyState("Пусто")
                }
            }
        }
    }

    @Test
    fun `should reject a table after an empty state`() {
        assertThrows<IllegalStateException> {
            buildTestPage {
                block {
                    emptyState("Пусто")
                    table<Int>(key = { row -> row }, fetch = { Page(emptyList(), 0) }) { textColumn("Номер") { row -> "$row" } }
                }
            }
        }
    }

    @Test
    fun `should reject an empty state after a table`() {
        assertThrows<IllegalStateException> {
            buildTestPage {
                block {
                    table<Int>(key = { row -> row }, fetch = { Page(emptyList(), 0) }) { textColumn("Номер") { row -> "$row" } }
                    emptyState("Пусто")
                }
            }
        }
    }

    private fun ui(): Component = UI.getCurrent()
}
