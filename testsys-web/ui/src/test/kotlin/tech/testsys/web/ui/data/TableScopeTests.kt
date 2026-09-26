package tech.testsys.web.ui.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.web.ui.testTexts

class TableScopeTests {
    private class Row(val id: String, val score: Int?)

    @Test
    fun `should keep columns in declaration order with their sort keys`() {
        val spec = TableScope<Row>(testTexts).apply {
            codeColumn("ID") { row -> row.id }
            numberColumn("Баллы", sortKey = "score") { row -> row.score }
        }.spec()

        assertEquals(listOf("ID", "Баллы"), spec.columns.map { column -> column.title })
        assertEquals(listOf(null, "score"), spec.columns.map { column -> column.sortKey })
    }

    @Test
    fun `should default the empty text to the table texts`() {
        assertEquals(testTexts.table.empty, TableScope<Row>(testTexts).spec().emptyText)
    }

    @Test
    fun `should use the given empty text over the default`() {
        assertEquals("Посылок нет", TableScope<Row>(testTexts).apply { empty("Посылок нет") }.spec().emptyText)
    }
}
