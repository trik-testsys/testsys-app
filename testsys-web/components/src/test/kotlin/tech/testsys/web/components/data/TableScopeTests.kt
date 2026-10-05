@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.testTexts

class TableScopeTests {
    private class Row(val id: String, val score: Int?)

    @Test
    fun `should render both civil date column kinds in monospace`() {
        val columns = TableScope<Row>(testTexts).apply {
            dateColumn("Date", size = 1) { java.time.LocalDate.of(2026, 10, 4) }
            dateTimeColumn("Time", size = 1) { java.time.LocalDateTime.of(2026, 10, 4, 12, 0) }
        }.spec().columns
        assertEquals(listOf(CellKind.Date, CellKind.Date), columns.map { column -> column.kind })
        assertEquals(listOf("ts-num"), CellKind.Date.cssClasses.map { cssClass -> cssClass.value })
    }

    @Test
    fun `should keep columns in declaration order with their sort keys`() {
        val spec = TableScope<Row>(testTexts).apply {
            codeColumn("ID", size = 1) { row -> row.id }
            numberColumn("Баллы", sortKey = "score", size = 1) { row -> row.score }
        }.spec()

        assertEquals(listOf("ID", "Баллы"), spec.columns.map { column -> column.title })
        assertEquals(listOf(null, "score"), spec.columns.map { column -> column.sortKey })
    }

    @Test
    fun `should default the empty text to the table texts`() {
        assertEquals(testTexts.table.empty, TableScope<Row>(testTexts).spec().empty.title)
    }

    @Test
    fun `should use the given empty text over the default`() {
        assertEquals("Посылок нет", TableScope<Row>(testTexts).apply { empty("Посылок нет") }.spec().empty.title)
    }
}
