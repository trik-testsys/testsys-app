package tech.testsys.web.components.overlay

import com.vaadin.flow.component.UI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.display.text
import tech.testsys.web.components.error.NotFoundPage
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll
import tech.testsys.web.components.navigation.header.CabinetHeader
import tech.testsys.web.components.testTexts

class GuideTests : MockVaadinTests() {
    @Test
    fun `should place one accessible page button and one block button`() {
        buildTestPage {
            head("Страница")
            guide("Описание страницы")
            row { block(title = "Блок") { guide("Описание блока") } }
        }

        val buttons = UI.getCurrent().findAll("ts-guide-button")
        assertEquals(listOf("page", "block"), buttons.map { button -> button.element.getAttribute("data-guide-start") })
        assertEquals(
            listOf(testTexts.guide.page, testTexts.guide.block),
            buttons.map { button -> button.element.getAttribute("aria-label") },
        )
        assertEquals("Описание страницы", UI.getCurrent().find("ts-page-head").element.getAttribute("data-guide-description"))
    }

    @Test
    fun `should leave unconfigured blocks without guide controls`() {
        val page = buildTestPage { row { block(title = "Блок") {} } }

        assertTrue(page.findAll("ts-guide-button").isEmpty())
    }

    @Test
    fun `should create heading for an untitled dark block guide`() {
        val page = buildTestPage { row { highlightBlock { guide("Описание", title = "Таймер") } } }

        val block = page.find("ts-block--dark")
        assertEquals("Таймер", block.element.getAttribute("data-guide-title"))
        assertEquals("block", block.find("ts-guide-button").element.getAttribute("data-guide-start"))
    }

    @Test
    fun `should keep detailed steps in their declared order`() {
        lateinit var first: ElementHandle
        lateinit var second: ElementHandle

        buildTestPage {
            row {
                block(title = "Блок") {
                    row {
                        first = text("Первое", size = 12)
                        second = text("Второе")
                    }
                    guide("Описание") {
                        step(second, title = "Второе", description = "Сначала второе")
                        step(first, title = "Первое", description = "Потом первое")
                    }
                }
            }
        }

        assertEquals("0", second.component.element.getAttribute("data-guide-order"))
        assertEquals("1", first.component.element.getAttribute("data-guide-order"))
    }

    @Test
    fun `should reject a target from another block`() {
        lateinit var target: ElementHandle

        assertThrows<IllegalArgumentException> {
            buildTestPage {
                row { block { row { target = text("Чужое поле") } } }
                row { block(title = "Блок") { guide("Описание") { step(target, title = "Поле", description = "Описание") } } }
            }
        }
    }

    @Test
    fun `should reject a repeated page guide`() {
        assertThrows<IllegalStateException> {
            buildTestPage {
                guide("Первый")
                guide("Второй")
            }
        }
    }

    @Test
    fun `should reject a repeated block guide`() {
        assertThrows<IllegalStateException> {
            buildTestPage {
                row {
                    block(title = "Блок") {
                        guide("Первый")
                        guide("Второй")
                    }
                }
            }
        }
    }

    @Test
    fun `should reject a blank page description`() {
        assertThrows<IllegalArgumentException> { buildTestPage { guide(" ") } }
    }

    @Test
    fun `should reject an untitled block without guide title`() {
        assertThrows<IllegalArgumentException> { buildTestPage { row { block { guide("Описание") } } } }
    }

    @Test
    fun `should replace guide controls when the page is rebuilt`() {
        val view = RebuiltGuideView()
        UI.getCurrent().add(view)
        view.show("Первый")

        view.show("Второй")

        assertEquals(2, UI.getCurrent().findAll("ts-guide-button").size)
        assertEquals("Второй", UI.getCurrent().find("ts-page-head").element.getAttribute("data-guide-description"))
        assertEquals(1, UI.getCurrent().findAll("ts-guide-controller").size)
    }

    @Test
    fun `should reject repeated target in detailed steps`() {
        assertThrows<IllegalArgumentException> {
            buildTestPage {
                row {
                    block(title = "Блок") {
                        lateinit var target: ElementHandle
                        row { target = text("Значение") }
                        guide("Описание") {
                            step(target, title = "Первый", description = "Описание")
                            step(target, title = "Второй", description = "Описание")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should add guides to error pages`() {
        val page = NotFoundPage(testTexts)

        assertEquals(2, page.findAll("ts-guide-button").size)
        assertFalse(page.find("ts-block").element.getAttribute("data-guide-description").isNullOrBlank())
    }
}

private class RebuiltGuideView : TestSysView(testTexts) {
    fun show(description: String) {
        page(CabinetHeader()) {
            head("Страница")
            guide(description)
            row { block(title = "Блок") { guide("Описание блока") } }
        }
    }
}
