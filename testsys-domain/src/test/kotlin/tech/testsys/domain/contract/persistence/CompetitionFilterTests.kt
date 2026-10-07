package tech.testsys.domain.contract.persistence

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant

class CompetitionFilterTests {

    @Test
    fun `should leave all selection criteria unrestricted by default`() {
        val filter = CompetitionFilter()

        assertEquals(null, filter.name)
        assertEquals(null, filter.createdFrom)
        assertEquals(null, filter.createdTo)
    }

    @ParameterizedTest
    @CsvSource("10,", ",20", "10,20", "20,20")
    fun `should accept one sided ordered and equal creation bounds`(from: Long?, to: Long?) {
        val lower = from?.let { Instant.ofEpochSecond(it) }
        val upper = to?.let { Instant.ofEpochSecond(it) }

        val filter = CompetitionFilter(name = "  Alpha%_  ", createdFrom = lower, createdTo = upper)

        assertEquals("  Alpha%_  ", filter.name)
        assertEquals(lower, filter.createdFrom)
        assertEquals(upper, filter.createdTo)
    }

    @Test
    fun `should reject a lower creation bound after the upper bound`() {
        assertThrows(IllegalArgumentException::class.java) {
            CompetitionFilter(createdFrom = Instant.ofEpochSecond(21), createdTo = Instant.ofEpochSecond(20))
        }
    }
}
