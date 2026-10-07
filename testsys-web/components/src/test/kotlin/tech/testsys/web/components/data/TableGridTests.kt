package tech.testsys.web.components.data

import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.html.Table
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.find

class TableGridTests : MockVaadinTests() {
    @Test
    fun `should split the 24 fractions of a block inside a slot`() {
        val page = buildTestPage {
            row {
                slot(size = 8) {
                    row {
                        block {
                            table(key = { row: Int -> row }, selectable = true, selectionSize = 3, fetch = { Page(listOf(1), 1) }) {
                                textColumn("Name", size = 6) { "Name" }
                                numberColumn("Score") { 50 }
                                menuColumn(size = 3) { item("Open") {} }
                            }
                        }
                    }
                }
            }
        }

        val table = _get<Table>().element
        assertEquals("24", table.style.get("--ts-table-used"))
        assertEquals("100.0%", table.style.get("--ts-table-width"))
        assertEquals(
            listOf("12.5%", "25.0%", "50.0%", "12.5%"),
            table.getChild(0).children.toList().map { col -> col.style.get("width") },
        )
        assertEquals("8.0", page.find("ts-block").element.style.get("--ts-page-columns"))
    }

    @Test
    fun `should retain an unused part of the logical grid`() {
        buildTestPage {
            block {
                table(key = { row: Int -> row }, gridColumns = 8, fetch = { Page(emptyList(), 0) }) {
                    textColumn("Name", size = 2) { "Name" }
                }
            }
        }

        val table = _get<Table>().element
        assertEquals("25.0%", table.style.get("--ts-table-width"))
        assertEquals("1", table.getChild(2).getChild(0).getChild(0).getAttribute("colspan"))
    }
}
