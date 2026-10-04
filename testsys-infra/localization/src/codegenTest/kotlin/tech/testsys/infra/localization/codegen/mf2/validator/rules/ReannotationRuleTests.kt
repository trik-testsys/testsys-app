package tech.testsys.infra.localization.codegen.mf2.validator.rules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.RU_TASK_TERM
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.ruErrors

class ReannotationRuleTests {

    @Test
    fun `should reject a term over a value declared with number`() {
        assertEquals(
            listOf(
                "RU / task.a: ':term' cannot annotate a value declared with ':number': ICU4J 78.1 would apply it to the " +
                    "declaration's operand and options",
            ),
            ruErrors(RU_TASK_TERM, "task.a=.input {\$n :number} {{{\$n :term name=task case=nom}}}"),
        )
    }

    @Test
    fun `should accept a term over a value declared with integer`() {
        val problems = ReannotationRule.problemsIn(".input {\$n :integer} {{{\$n :term name=task case=nom}}}")

        assertEquals(emptyList<String>(), problems)
    }
}
