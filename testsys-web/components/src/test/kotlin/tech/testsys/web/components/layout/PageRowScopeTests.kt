package tech.testsys.web.components.layout

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.child
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll

class PageRowScopeTests : MockVaadinTests() {
    @Test
    fun `should size a block without size to the rest of the row`() {
        val main = buildTestPage {
            row {
                block(size = 16) {}
                block {}
            }
        }

        assertEquals("span 8", main.child(0).child(1).element.style.get("grid-column"))
    }

    @Test
    fun `should reject a block after a block that took the rest of the row`() {
        val error = assertThrows<IllegalStateException> {
            buildTestPage {
                row {
                    block {}
                    block(size = 8) {}
                }
            }
        }

        assertTrue(requireNotNull(error.message).contains("takes the rest of the row"))
    }

    @Test
    fun `should place blocks of a row side by side`() {
        val main = buildTestPage {
            row {
                block(size = 16) {}
                highlightBlock(size = 8) {}
            }
        }

        val spans = main.child(0).children.map { block -> block.element.style.get("grid-column") }.toList()
        assertEquals(listOf("span 16", "span 8"), spans)
    }

    @Test
    fun `should set page columns of a block to its size`() {
        val main = buildTestPage { row { block(size = 8) {} } }

        assertEquals("8.0", main.find("ts-block").element.style.get("--ts-page-columns"))
    }

    @Test
    fun `should accept a row that is not full`() {
        val main = buildTestPage { row { block(size = 16) {} } }

        assertEquals(1, main.child(0).children.toList().size)
    }

    @Test
    fun `should reject blocks that overflow the row`() {
        val error = assertThrows<IllegalStateException> {
            buildTestPage {
                row {
                    block(size = 16) {}
                    block(size = 12) {}
                }
            }
        }

        assertTrue(requireNotNull(error.message).contains("16+12 = 28"))
    }

    @Test
    fun `should reject a block of zero columns`() {
        assertThrows<IllegalArgumentException> { buildTestPage { row { block(size = 0) {} } } }
    }

    @Test
    fun `should reject a block wider than the page`() {
        assertThrows<IllegalArgumentException> { buildTestPage { row { highlightBlock(size = 25) {} } } }
    }

    @Test
    fun `should reject a second highlight block in one row`() {
        assertThrows<IllegalStateException> {
            buildTestPage {
                row {
                    highlightBlock(size = 12) {}
                    highlightBlock(size = 12) {}
                }
            }
        }
    }

    @Test
    fun `should allow highlight blocks in different rows`() {
        val main = buildTestPage {
            row { highlightBlock(size = 12) {} }
            row { highlightBlock(size = 12) {} }
        }

        assertEquals(2, main.findAll("ts-block--dark").size)
    }
}
