package tech.testsys.web.components.layout

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.child
import tech.testsys.web.components.classes

class PageScopeTests : MockVaadinTests() {
    @Test
    internal fun `should reject repeated footer configuration even if its first configuration is empty`() {
        val error = assertThrows<IllegalStateException> {
            buildTestPage {
                footer {}
                footer {}
            }
        }

        assertTrue(error.message.orEmpty().contains("footer() once"))
    }

    @Test
    internal fun `should reject a page head declared after footer configuration`() {
        assertThrows<IllegalStateException> {
            buildTestPage {
                footer {}
                head("Late head")
            }
        }
    }

    @Test
    fun `should stack rows in order`() {
        val main = buildTestPage {
            row { block(title = "Профиль") {} }
            row { highlightBlock {} }
        }

        assertEquals(listOf(true, true), main.children.map { row -> "ts-row" in row.classes() }.toList())
        assertEquals("span 24", main.child(0).child(0).element.style.get("grid-column"))
        assertTrue("ts-block--dark" in main.child(1).child(0).classes())
    }
}
