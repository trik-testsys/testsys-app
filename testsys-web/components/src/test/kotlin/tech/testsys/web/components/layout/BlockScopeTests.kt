package tech.testsys.web.components.layout

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.TextHandle
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.child
import tech.testsys.web.components.classes
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.emptyState
import tech.testsys.web.components.feedback.load
import tech.testsys.web.components.find

class BlockScopeTests : MockVaadinTests() {
    @Test
    fun `should interleave semantic headings and separators with rows in a grid body`() {
        val page = buildTestPage {
            row {
                block {
                    sectionTitle("Группа")
                    row { text("Первое поле") }
                    separator()
                    row { text("Второе поле") }
                }
            }
        }

        val body = page.find("ts-block__body")
        assertEquals(listOf("h3", "div", "hr", "div"), body.children.map { child -> child.element.tag }.toList())
        assertEquals("Группа", body.child(0).element.text)
        assertTrue("ts-block__section-title" in body.child(0).classes())
        assertTrue("ts-block__separator" in body.child(2).classes())
        assertTrue("ts-block__body--grid" in body.classes())
        assertFalse("ts-block__body--flush" in body.classes())
    }

    @Test
    fun `should update section title and hide both elements through their handles`() {
        lateinit var title: TextHandle
        lateinit var separator: ElementHandle
        val page = buildTestPage {
            row {
                block {
                    title = sectionTitle("Группа")
                    separator = separator()
                }
            }
        }

        title.text = "Новая группа"
        title.isVisible = false
        separator.isVisible = false

        assertEquals("Новая группа", page.find("ts-block__section-title").element.text)
        assertFalse(page.find("ts-block__section-title").isVisible)
        assertFalse(page.find("ts-block__separator").isVisible)
    }

    @ParameterizedTest
    @CsvSource("title,table", "title,empty", "title,load", "separator,table", "separator,empty", "separator,load")
    fun `should reject a structural element after content taking the whole body`(structure: String, whole: String) {
        assertThrows<IllegalStateException> {
            buildTestPage {
                row {
                    block {
                        addWhole(whole)
                        addStructure(structure)
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @CsvSource("title,table", "title,empty", "title,load", "separator,table", "separator,empty", "separator,load")
    fun `should reject content taking the whole body after a structural element`(structure: String, whole: String) {
        assertThrows<IllegalStateException> {
            buildTestPage {
                row {
                    block {
                        addStructure(structure)
                        addWhole(whole)
                    }
                }
            }
        }
    }

    private fun BlockScope.addStructure(structure: String) {
        when (structure) {
            "title" -> sectionTitle("Группа")
            "separator" -> separator()
            else -> error("Unknown structure: $structure")
        }
    }

    private fun BlockScope.addWhole(whole: String) {
        when (whole) {
            "table" -> table(key = { value: Int -> value }, fetch = { Page(listOf(1), 1) }) { column("Число") { value -> text("$value") } }
            "empty" -> emptyState("Пусто")
            "load" -> load({ 1 }) { value -> row { text("$value") } }
            else -> error("Unknown whole-body content: $whole")
        }
    }
}
