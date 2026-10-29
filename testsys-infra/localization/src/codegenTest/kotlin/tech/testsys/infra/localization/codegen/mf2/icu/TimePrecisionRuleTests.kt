package tech.testsys.infra.localization.codegen.mf2.icu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.ruErrors

class TimePrecisionRuleTests {

    @Test
    fun `should reject timeZoneStyle without precision`() {
        assertEquals(
            listOf("RU / task.a: option 'timeZoneStyle' of ':time' is not honoured by ICU4J 78.1 without 'precision'"),
            ruErrors("task.a={\$d :time timeZoneStyle=long}"),
        )
    }

    @Test
    fun `should reject hour12 without precision`() {
        assertEquals(
            listOf("RU / task.a: option 'hour12' of ':time' is not honoured by ICU4J 78.1 without 'precision'"),
            ruErrors("task.a={\$d :time hour12=false}"),
        )
    }

    @Test
    fun `should reject hour12 of datetime without timePrecision`() {
        assertEquals(
            listOf("RU / task.a: option 'hour12' of ':datetime' is not honoured by ICU4J 78.1 without 'timePrecision'"),
            ruErrors("task.a={\$d :datetime hour12=false}"),
        )
    }

    @Test
    fun `should reject timeZoneStyle of datetime without timePrecision and date`() {
        assertEquals(
            listOf("RU / task.a: option 'timeZoneStyle' of ':datetime' is not honoured by ICU4J 78.1 without 'timePrecision'"),
            ruErrors("task.a={\$d :datetime timeZoneStyle=short}"),
        )
    }

    @Test
    fun `should accept timeZoneStyle of datetime with date and without timePrecision`() {
        val problems = TimePrecisionRule.problemsIn("{\$d :datetime dateFields=year-month-day dateLength=long timeZoneStyle=short}")

        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun `should accept timeZoneStyle with precision`() {
        val problems = TimePrecisionRule.problemsIn("{\$d :time precision=minute timeZoneStyle=long}")

        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun `should accept hour12 of datetime with timePrecision`() {
        val problems = TimePrecisionRule.problemsIn("{\$d :datetime timePrecision=minute hour12=false}")

        assertEquals(emptyList<String>(), problems)
    }
}
