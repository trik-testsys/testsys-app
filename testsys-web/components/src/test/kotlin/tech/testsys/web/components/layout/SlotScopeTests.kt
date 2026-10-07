package tech.testsys.web.components.layout

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.child
import tech.testsys.web.components.classes
import tech.testsys.web.components.display.statCard
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll

class SlotScopeTests : MockVaadinTests() {
    @Test
    fun `should size a block to the whole slot by default`() {
        val main = buildTestPage { row { slot(size = 16) { row { block {} } } } }

        val slotRow = main.child(0).child(0).child(0)
        assertTrue("ts-slot__row" in slotRow.classes())
        assertEquals("span 24", slotRow.child(0).element.style.get("grid-column"))
    }

    @Test
    fun `should place blocks of a slot row side by side`() {
        val main = buildTestPage {
            row {
                slot(size = 16) {
                    row {
                        block(size = 12) {}
                        block(size = 12) {}
                    }
                }
            }
        }

        val slotRow = main.child(0).child(0).child(0)
        assertEquals(2, slotRow.children.toList().size)
        assertEquals("span 12", slotRow.child(0).element.style.get("grid-column"))
    }

    @Test
    fun `should accept half of a narrow slot as 12 columns`() {
        val main = buildTestPage {
            row {
                slot(size = 6) {
                    row {
                        block(size = 12) {}
                        block(size = 12) {}
                    }
                }
            }
        }

        val slotRow = main.child(0).child(0).child(0)
        assertEquals(listOf("span 12", "span 12"), slotRow.children.map { block -> block.element.style.get("grid-column") }.toList())
    }

    @Test
    fun `should set page columns of a block from its share of the slot`() {
        val main = buildTestPage { row { slot(size = 8) { row { block(size = 12) {} } } } }

        assertEquals("4.0", main.find("ts-block").element.style.get("--ts-page-columns"))
    }

    @Test
    fun `should not set page columns of a block placed in the page`() {
        val main = buildTestPage { block {} }

        assertNull(main.find("ts-block").element.style.get("--ts-page-columns"))
    }

    @Test
    fun `should accept a slot row block exactly one page column wide`() {
        val main = buildTestPage { row { slot(size = 6) { row { block(size = 4) {} } } } }

        assertEquals("1.0", main.find("ts-block").element.style.get("--ts-page-columns"))
    }

    @Test
    fun `should reject a slot row block narrower than one page column`() {
        val error = assertThrows<IllegalArgumentException> {
            buildTestPage { row { slot(size = 6) { row { block(size = 3) {} } } } }
        }

        assertTrue(requireNotNull(error.message).contains("slot 6 × size 3 < 24"))
    }

    @Test
    fun `should reject a slot row stat card narrower than one page column`() {
        assertThrows<IllegalArgumentException> {
            buildTestPage { row { slot(size = 6) { row { statCard(label = "Решено", value = "42", size = 3) } } } }
        }
    }

    @Test
    fun `should reject blocks that overflow the slot row`() {
        val error = assertThrows<IllegalStateException> {
            buildTestPage {
                row {
                    slot(size = 16) {
                        row {
                            block(size = 12) {}
                            block(size = 18) {}
                        }
                    }
                }
            }
        }

        assertTrue(requireNotNull(error.message).contains("12+18 = 30 exceed 24"))
    }

    @Test
    fun `should reject a second highlight block in one page row`() {
        assertThrows<IllegalStateException> {
            buildTestPage {
                row {
                    slot(size = 12) { row { highlightBlock {} } }
                    slot(size = 12) { row { highlightBlock {} } }
                }
            }
        }
    }

    @Test
    fun `should allow highlight blocks in different page rows`() {
        val main = buildTestPage {
            row { slot(size = 12) { row { highlightBlock {} } } }
            row { slot(size = 12) { row { highlightBlock {} } } }
        }

        assertEquals(2, main.findAll("ts-block--dark").size)
    }
}
