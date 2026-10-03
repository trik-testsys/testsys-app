package tech.testsys.infra.localization.codegen.mf2.validator.rules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.ruErrors

class VariableNamesRuleTests {

    @Test
    fun `should reject a variable name that is not a Kotlin parameter name`() {
        assertEquals(
            listOf("RU / task.a: the variable name '\$user-name' must match [a-z][A-Za-z0-9]* and not be a Kotlin keyword"),
            ruErrors("task.a={\$user-name}"),
        )
    }

    @Test
    fun `should reject a variable named like a Kotlin keyword`() {
        assertEquals(
            listOf("RU / task.a: the variable name '\$in' must match [a-z][A-Za-z0-9]* and not be a Kotlin keyword"),
            ruErrors("task.a={\$in}"),
        )
    }

    @Test
    fun `should accept camel case variable names`() {
        val problems = VariableNamesRule.problemsIn(".input {\$userName :string} {{{\$userName}: {\$score2}}}")

        assertEquals(emptyList<String>(), problems)
    }
}
