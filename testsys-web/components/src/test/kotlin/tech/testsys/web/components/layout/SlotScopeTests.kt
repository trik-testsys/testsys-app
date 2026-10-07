package tech.testsys.web.components.layout

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.child
import tech.testsys.web.components.classes
import tech.testsys.web.components.findAll

class SlotScopeTests : MockVaadinTests() {
    @Test
    fun `should size a block to the whole slot by default`() {
        val main = buildTestPage { row { slot(size = 16) { row { block {} } } } }

        val slotRow = main.child(0).child(0).child(0)
        assertTrue("ts-slot__row" in slotRow.classes())
        assertEquals("span 16", slotRow.child(0).element.style.get("grid-column"))
    }

    @Test
    fun `should place blocks of a slot row side by side`() {
        val main = buildTestPage {
            row {
                slot(size = 16) {
                    row {
                        block(size = 8) {}
                        block(size = 8) {}
                    }
                }
            }
        }

        val slotRow = main.child(0).child(0).child(0)
        assertEquals(2, slotRow.children.toList().size)
        assertEquals("span 8", slotRow.child(0).element.style.get("grid-column"))
    }

    @Test
    fun `should reject blocks that overflow the slot row`() {
        val error = assertThrows<IllegalStateException> {
            buildTestPage {
                row {
                    slot(size = 16) {
                        row {
                            block(size = 8) {}
                            block(size = 12) {}
                        }
                    }
                }
            }
        }

        assertTrue(requireNotNull(error.message).contains("8+12 = 20 exceed 16"))
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
