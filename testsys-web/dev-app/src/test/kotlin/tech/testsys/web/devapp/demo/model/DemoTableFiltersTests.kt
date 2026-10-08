package tech.testsys.web.devapp.demo.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.web.components.data.PageRequest
import tech.testsys.web.components.forms.DateRange
import tech.testsys.web.devapp.demo.ui.DEMO_CABINETS
import tech.testsys.web.devapp.demo.ui.demoScreen
import java.time.LocalDate

class DemoTableFiltersTests {
    private val rows = (1..12).map { number -> DemoRow(id = "p$number", title = "Участник $number") }

    @Test
    fun `should return the requested page and the total of all rows`() {
        val second = demoPage(rows, DemoCriteria(), PageRequest(offset = 10, limit = 10, sort = null))

        assertEquals(listOf("p11", "p12"), second.rows.map { row -> row.id })
        assertEquals(12, second.total)
    }

    @Test
    fun `should filter supplied rows by a trimmed query`() {
        val filtered = filteredDemoRows(rows, DemoCriteria(query = " 12 "))

        assertEquals(listOf("p12"), filtered.map { row -> row.id })
    }

    @Test
    fun `should combine literal query category and inclusive date range`() {
        val date = LocalDate.parse("2026-10-01")
        val dated = listOf(
            DemoRow(id = "r1", title = "Условие", category = "TXT", date = date),
            DemoRow(id = "r2", title = "Условие", category = "XML", date = date.plusDays(1)),
        )

        val criteria = DemoCriteria(query = "условие", category = "TXT", period = DateRange(date, date))

        val filtered = filteredDemoRows(dated, criteria)

        assertEquals(listOf("r1"), filtered.map { row -> row.id })
    }

    @ParameterizedTest
    @CsvSource(
        "organizer.participants, organizer.participants",
        "judge.result, judge.result",
        "org, organizer.overview",
        "developer.missing, home",
    )
    fun `should resolve only known cabinet sections and legacy demo aliases`(key: String, expected: String) {
        assertEquals(expected, demoScreen(key))
    }

    @Test
    fun `should declare eight cabinets`() {
        assertEquals(8, DEMO_CABINETS.size)
    }
}
