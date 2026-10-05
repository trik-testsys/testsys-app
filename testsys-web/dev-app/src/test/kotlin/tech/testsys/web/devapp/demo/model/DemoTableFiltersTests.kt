package tech.testsys.web.devapp.demo.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.web.components.data.PageRequest
import tech.testsys.web.components.forms.DateRange
import tech.testsys.web.devapp.demo.ui.DEMO_CABINETS
import tech.testsys.web.devapp.demo.ui.demoScreen
import java.time.LocalDate

class DemoTableFiltersTests {
    @Test
    fun `should filter only supplied scoped rows and retain total across pagination`() {
        val rows = (1..12).map { DemoRow("p$it", "Участник $it") }

        val second = demoPage(rows, DemoCriteria(), PageRequest(offset = 10, limit = 10, sort = null))
        assertEquals(listOf("p11", "p12"), second.rows.map { it.id })
        assertEquals(12, second.total)
        assertEquals(listOf("p12"), filteredDemoRows(rows, DemoCriteria(" 12 ")).map { it.id })
    }

    @Test
    fun `should keep draft changes separate until applied and reset to empty defaults`() {
        val state = DemoTableState()
        state.draft.query = "Анна"

        assertEquals("", state.applied.query)
        state.applied = DemoCriteria(state.draft.query)
        state.draft.query = "Иван"
        assertEquals("Анна", state.applied.query)
        state.applied = DemoCriteria()
        assertEquals("", state.applied.query)
    }

    @Test
    fun `should combine literal query category and inclusive date range`() {
        val date = LocalDate.parse("2026-10-01")
        val rows = listOf(DemoRow("r1", "Условие", "TXT", date), DemoRow("r2", "Условие", "XML", date.plusDays(1)))

        val filtered = filteredDemoRows(rows, DemoCriteria("условие", "TXT", DateRange(date, date)))

        assertEquals(listOf("r1"), filtered.map { it.id })
    }

    @Test
    fun `should resolve only known cabinet sections and legacy demo aliases`() {
        assertEquals("organizer.participants", demoScreen("organizer.participants"))
        assertEquals("organizer.overview", demoScreen("org"))
        assertEquals("home", demoScreen("developer.missing"))
        assertEquals(8, DEMO_CABINETS.size)
        assertEquals("judge.result", demoScreen("judge.result"))
    }
}
