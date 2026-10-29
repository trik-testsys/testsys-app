package tech.testsys.infra.localization.codegen.mf2.icu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.ruErrors

class PrecisionOptionsRuleTests {

    @Test
    fun `should reject a precision option overridden by a later one`() {
        assertEquals(
            listOf(
                "RU / task.a: option 'minimumFractionDigits' of ':number' is not honoured by ICU4J 78.1 together with " +
                    "'maximumFractionDigits'",
            ),
            ruErrors("task.a={\$x :number minimumFractionDigits=1 maximumFractionDigits=2}"),
        )
    }

    @Test
    fun `should reject an inherited precision option overridden by a later one`() {
        assertEquals(
            listOf(
                "RU / task.a: option 'minimumFractionDigits' of ':number' is not honoured by ICU4J 78.1 together with " +
                    "'maximumFractionDigits'",
            ),
            ruErrors("task.a=.input {\$x :number maximumFractionDigits=2} {{{\$x :number minimumFractionDigits=1}}}"),
        )
    }

    @Test
    fun `should accept one precision option`() {
        val problems = PrecisionOptionsRule.problemsIn("{\$x :number maximumFractionDigits=2}")

        assertEquals(emptyList<String>(), problems)
    }
}
