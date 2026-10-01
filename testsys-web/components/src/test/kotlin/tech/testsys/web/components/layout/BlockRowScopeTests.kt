package tech.testsys.web.components.layout

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.child
import tech.testsys.web.components.classes
import tech.testsys.web.components.display.text
import tech.testsys.web.components.find

class BlockRowScopeTests : MockVaadinTests() {
    @Test
    fun `should span an element over its size`() {
        val row = buildTestRow {
            text("a", size = 8)
            text("b", size = 16)
        }

        assertEquals("span 8", row.child(0).element.style.get("grid-column"))
        assertEquals("span 16", row.child(1).element.style.get("grid-column"))
    }

    @Test
    fun `should give an element without a size the rest of the row`() {
        val row = buildTestRow {
            text("a", size = 8)
            text("b")
        }

        assertEquals("span 16", row.child(1).element.style.get("grid-column"))
    }

    @Test
    fun `should reject an element after the one that took the rest`() {
        val error = assertThrows<IllegalStateException> {
            buildTestRow {
                text("a")
                text("b", size = 1)
            }
        }

        assertTrue(error.message!!.startsWith("Block row is full"))
    }

    @Test
    fun `should size rows on the columns of a block inside a slot`() {
        val page = buildTestPage { row { slot(size = 12) { row { block(size = 8) { row { text("x") } } } } } }

        assertEquals("span 8", page.find("ts-block__row").child(0).element.style.get("grid-column"))
    }

    @Test
    fun `should reject elements that overflow the block`() {
        val error = assertThrows<IllegalStateException> {
            buildTestPage {
                row {
                    slot(size = 8) {
                        row {
                            block {
                                row {
                                    text("a", size = 6)
                                    text("b", size = 4)
                                }
                            }
                        }
                    }
                }
            }
        }

        assertTrue(error.message!!.contains("6+4 = 10 exceed 8"))
    }

    @Test
    fun `should lay groups over their size`() {
        val row = buildTestRow {
            horizontal(size = 12) { text("a") }
            vertical { text("b") }
        }

        assertTrue("ts-hstack" in row.child(0).classes())
        assertEquals("span 12", row.child(0).element.style.get("grid-column"))
        assertTrue("ts-vstack" in row.child(1).classes())
        assertEquals("span 12", row.child(1).element.style.get("grid-column"))
    }
}
