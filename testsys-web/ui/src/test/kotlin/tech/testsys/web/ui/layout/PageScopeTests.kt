package tech.testsys.web.ui.layout

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.ui.MockVaadinTests
import tech.testsys.web.ui.buildTestPage
import tech.testsys.web.ui.child
import tech.testsys.web.ui.classes

class PageScopeTests : MockVaadinTests() {
    @Test
    fun `should stack full-width blocks and rows in order`() {
        val main = buildTestPage {
            block(title = "Профиль") {}
            row { slot(24) { row { block {} } } }
        }

        assertTrue("ts-block" in main.child(0).classes())
        assertTrue("ts-row" in main.child(1).classes())
    }

    @Test
    fun `should span a slot over its size`() {
        val main = buildTestPage {
            row {
                slot(16) {}
                slot(8) {}
            }
        }

        val row = main.child(0)
        assertEquals("span 16", row.child(0).element.style.get("grid-column"))
        assertEquals("span 8", row.child(1).element.style.get("grid-column"))
        assertTrue("ts-slot" in row.child(0).classes())
    }

    @Test
    fun `should accept a row that is not full`() {
        val main = buildTestPage { row { slot(16) {} } }

        assertEquals(1, main.child(0).children.toList().size)
    }

    @Test
    fun `should reject a slot that overflows the row`() {
        val error = assertThrows<IllegalStateException> {
            buildTestPage {
                row {
                    slot(16) {}
                    slot(12) {}
                }
            }
        }

        assertTrue(error.message!!.contains("16+12 = 28"))
    }

    @Test
    fun `should reject a slot wider than the page`() {
        assertThrows<IllegalArgumentException> { buildTestPage { row { slot(25) {} } } }
    }
}
