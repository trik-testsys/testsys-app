package tech.testsys.web.components.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.web.components.testTexts
import java.time.LocalDate
import java.time.LocalDateTime

class CellFormatTests {
    @Test
    fun `should group number digits by the locale`() {
        assertEquals("1 412", formatNumber(1412, testTexts))
    }

    @Test
    fun `should format date by the calendar pattern`() {
        assertEquals("12.09.2026", formatDate(LocalDate.parse("2026-09-12"), testTexts))
    }

    @Test
    fun `should format date and time by the calendar pattern and hours and minutes`() {
        assertEquals("12.09.2026 14:05", formatDateTime(LocalDateTime.parse("2026-09-12T14:05:33"), testTexts))
    }

    @Test
    fun `should show a dash for a missing value`() {
        assertEquals(EMPTY_CELL, formatNumber(null, testTexts))
    }
}
