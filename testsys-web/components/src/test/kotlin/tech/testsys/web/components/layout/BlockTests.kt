package tech.testsys.web.components.layout

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.child
import tech.testsys.web.components.classes
import tech.testsys.web.components.display.text
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll

class BlockTests : MockVaadinTests() {
    @Test
    fun `should render head with title and subtitle`() {
        val block = buildTestPage { row { block(title = "Решения", subtitle = "За неделю") { row { text("…") } } } }.child(0).child(0)

        assertEquals("section", block.element.tag)
        assertEquals("Решения", block.find("ts-block__title").element.textRecursively)
        assertEquals("За неделю", block.find("ts-block__sub").element.textRecursively)
        assertTrue("ts-block__head" in block.child(0).classes())
        assertTrue("ts-block__body" in block.child(1).classes())
    }

    @Test
    fun `should lay the block, its body and its rows on the grid`() {
        val block = buildTestPage { row { block { row { text("…") } } } }.child(0).child(0)

        assertTrue("ts-block--grid" in block.classes())
        assertTrue("ts-block__body--grid" in block.find("ts-block__body").classes())
        assertTrue("ts-block__row" in block.find("ts-block__body").child(0).classes())
    }

    @Test
    fun `should omit head without title, subtitle and actions`() {
        val block = buildTestPage { row { block { row { text("…") } } } }.child(0).child(0)

        assertTrue(block.findAll("ts-block__head").isEmpty())
    }

    @Test
    fun `should render head with actions only`() {
        val block = buildTestPage { row { block { actions { text("фильтр") } } } }.child(0).child(0)

        assertEquals("фильтр", block.find("ts-block__actions").element.textRecursively)
        assertTrue("ts-block__titles" in block.find("ts-block__head").child(0).classes())
    }

    @Test
    fun `should omit body without rows`() {
        val block = buildTestPage { row { block(title = "Пусто") {} } }.child(0).child(0)

        assertTrue(block.findAll("ts-block__body").isEmpty())
    }

    @Test
    fun `should omit body with empty rows only`() {
        val block = buildTestPage { row { block(title = "Пусто") { row {} } } }.child(0).child(0)

        assertTrue(block.findAll("ts-block__body").isEmpty())
    }

    @Test
    fun `should put footer after body`() {
        val block = buildTestPage {
            row {
                block {
                    row { text("тело") }
                    footer { text("подвал") }
                }
            }
        }.child(0).child(0)

        assertTrue("ts-block__body" in block.child(0).classes())
        assertTrue("ts-block__foot" in block.child(1).classes())
    }

    @Test
    fun `should render body without padding if content requests it`() {
        val block = buildTestPage {
            row {
                block {
                    row { text("таблица") }
                    requestFlushBody()
                }
            }
        }.child(0).child(0)

        assertTrue("ts-block__body--flush" in block.find("ts-block__body").classes())
    }

    @Test
    fun `should render highlight block dark`() {
        val block = buildTestPage { row { highlightBlock { row { text("идёт") } } } }.child(0).child(0)

        assertTrue("ts-block--dark" in block.classes())
    }

    @Test
    fun `should hide the block through its handle`() {
        lateinit var handle: BlockHandle
        val main = buildTestPage { row { handle = block { row { text("…") } } } }

        handle.isVisible = false

        assertFalse(main.child(0).child(0).isVisible)
    }

    @Test
    fun `should add semantic content through a horizontal group`() {
        val group = buildTestPage { row { block { row { horizontal { text("Содержимое") } } } } }.child(0).find("ts-hstack")

        assertEquals("Содержимое", group.child(0).element.textRecursively)
    }
}
