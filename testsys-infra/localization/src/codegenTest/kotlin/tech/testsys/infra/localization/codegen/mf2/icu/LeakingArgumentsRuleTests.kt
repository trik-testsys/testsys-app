package tech.testsys.infra.localization.codegen.mf2.icu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.ruErrors

class LeakingArgumentsRuleTests {

    @Test
    fun `should reject an argument named like a number option`() {
        assertEquals(
            listOf(
                "RU / task.a: the argument '\$notation' is named like a number option: ICU4J 78.1 passes every argument to " +
                    "number functions as an option; rename it",
            ),
            ruErrors("task.a={\$n :number} {\$notation}"),
        )
    }

    @Test
    fun `should reject the value of an option if another number expression sets that option literally`() {
        assertEquals(
            listOf(
                "RU / contest.a: the argument '\$currency' is named like a number option: ICU4J 78.1 passes every argument " +
                    "to number functions as an option; rename it",
            ),
            ruErrors("contest.a=Взнос: {\$fee :currency currency=\$currency}, приз: {\$prize :currency currency=EUR}"),
        )
    }

    @Test
    fun `should reject the value of an option if another number expression lacks that option`() {
        assertEquals(
            listOf(
                "RU / task.a: the argument '\$maximumFractionDigits' is named like a number option: ICU4J 78.1 passes every " +
                    "argument to number functions as an option; rename it",
            ),
            ruErrors("task.a={\$a :number maximumFractionDigits=\$maximumFractionDigits} {\$b :number}"),
        )
    }

    @Test
    fun `should accept an argument named like the option whose value it is in the only number expression`() {
        val problems = LeakingArgumentsRule.problemsIn("Взнос: {\$fee :currency currency=\$currency}")

        assertEquals(emptyList<String>(), problems)
    }
}
