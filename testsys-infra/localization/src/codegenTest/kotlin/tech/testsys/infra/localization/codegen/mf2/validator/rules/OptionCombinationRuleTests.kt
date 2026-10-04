package tech.testsys.infra.localization.codegen.mf2.validator.rules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.ruErrors

class OptionCombinationRuleTests {

    @Test
    fun `should reject an offset with both add and subtract`() {
        assertEquals(
            listOf("RU / task.a: ':offset' takes exactly one of the options 'add' and 'subtract'"),
            ruErrors("task.a={\$n :offset add=1 subtract=1}"),
        )
    }

    @Test
    fun `should reject an offset without add and subtract`() {
        assertEquals(
            listOf("RU / task.a: ':offset' takes exactly one of the options 'add' and 'subtract'"),
            ruErrors("task.a={\$n :offset}"),
        )
    }

    @Test
    fun `should accept an offset with only subtract`() {
        val problems = OptionCombinationRule.problemsIn(".input {\$n :integer} .local \$o = {\$n :offset subtract=1} {{{\$o}}}")

        assertEquals(emptyList<String>(), problems)
    }
}
