package tech.testsys.infra.localization.codegen.mf2.function

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class LiteralsTests {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "0000-01-01", "9999-12-31", "2026-02-31", "2026-10-03T00:00:00", "2026-10-03T23:59:59",
            "2026-10-03T12:30:45.1", "2026-10-03T12:30:45.12", "2026-10-03T12:30:45.123",
            "2026-10-03T12:30:45Z", "2026-10-03T12:30:45.123Z", "2026-10-03T12:30:45+00:00",
            "2026-10-03T12:30:45-00:00", "2026-10-03T12:30:45+13:59", "2026-10-03T12:30:45-13:59",
            "2026-10-03T12:30:45+14:00", "2026-10-03T12:30:45-14:00",
        ],
    )
    fun `should accept supported ISO date literal shapes`(value: String) {
        assertNull(Literals.isoDateProblem(value, "date"))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "026-10-03", "02026-10-03", "-2026-10-03", "2026-1-03", "2026-10-3", "2026-00-03", "2026-13-03",
            "2026-10-00", "2026-10-32", "2026-10-03Z", "2026-10-03+03:00", "2026-10-03t12:30:45",
            "2026-10-03T12:30", "2026-10-03T24:00:00", "2026-10-03T12:60:00", "2026-10-03T12:30:60",
            "2026-10-03T12:30:45.", "2026-10-03T12:30:45.1234", "2026-10-03T12:30:45z",
            "2026-10-03T12:30:45+14:01", "2026-10-03T12:30:45-14:01", "2026-10-03T12:30:45+15:00",
            "2026-10-03T12:30:45-15:00", "2026-10-03T12:30:45+13:60", "2026-10-03T12:30:45+1400",
            "2026-10-03T12:30:45+3:00", " 2026-10-03", "2026-10-03 ",
        ],
    )
    fun `should reject unsupported ISO date literal shapes`(value: String) {
        assertEquals("the literal operand '|$value|' of ':date' is not an ISO 8601 date", Literals.isoDateProblem(value, "date"))
    }
}
