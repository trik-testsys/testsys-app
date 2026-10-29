package tech.testsys.infra.localization.codegen.mf2.icu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.ruErrors

class ReannotationLimitsRuleTests {

    @Test
    fun `should reject a built-in function over an offset value`() {
        assertEquals(
            listOf("RU / task.a: ':integer' cannot annotate an ':offset' value: ICU4J 78.1 formats the unshifted operand"),
            ruErrors("task.a=.input {\$n :integer} .local \$o = {\$n :offset subtract=1} {{{\$o :integer}}}"),
        )
    }

    @Test
    fun `should reject an offset of an offset`() {
        assertEquals(
            listOf("RU / task.a: an ':offset' of an ':offset' is not supported: ICU4J 78.1 keeps only the last shift"),
            ruErrors(
                "task.a=.input {\$n :integer} .local \$a = {\$n :offset subtract=1} .local \$b = {\$a :offset subtract=1} {{{\$b}}}",
            ),
        )
    }

    @Test
    fun `should reject a built-in function over a value declared with another function`() {
        assertEquals(
            listOf(
                "RU / task.a: ':integer' cannot annotate a value declared with ':number': ICU4J 78.1 would apply it to the " +
                    "declaration's operand and options",
            ),
            ruErrors("task.a=.input {\$x :number signDisplay=always} {{{\$x :integer}}}"),
        )
    }

    @Test
    fun `should reject an offset of a number value with an option that the offset drops`() {
        assertEquals(
            listOf(
                "RU / task.a: an ':offset' of a ':number' value drops the option 'minimumFractionDigits': ICU4J 78.1 applies " +
                    "it only to ':number'",
            ),
            ruErrors("task.a=.input {\$x :number minimumFractionDigits=1} .local \$o = {\$x :offset subtract=1} {{{\$o}}}"),
        )
    }

    @Test
    fun `should accept an offset of a number value with an option that the offset keeps`() {
        val message = ".input {\$x :number signDisplay=always} .local \$o = {\$x :offset subtract=1} {{{\$o}}}"

        val problems = ReannotationLimitsRule.problemsIn(message)

        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun `should accept an offset of a value declared with integer`() {
        val problems = ReannotationLimitsRule.problemsIn(".input {\$n :integer} .local \$o = {\$n :offset subtract=1} {{{\$o}}}")

        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun `should accept a built-in function over a value declared with the same function`() {
        val problems = ReannotationLimitsRule.problemsIn(".input {\$x :number} {{{\$x :number maximumFractionDigits=2}}}")

        assertEquals(emptyList<String>(), problems)
    }
}
