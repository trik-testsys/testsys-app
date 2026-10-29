package tech.testsys.infra.localization.codegen.mf2.function

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.infra.localization.codegen.RU_TASK_TERM
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.mf2.validator.rules.OperandRule
import tech.testsys.infra.localization.codegen.ruErrors

class TermFunctionTests {

    @Test
    fun `should reject a bare reference without a number`() {
        assertEquals(
            listOf("RU / task.a: ':term' without an operand needs the option 'number' (sg or pl)"),
            ruErrors(RU_TASK_TERM, "task.a={:term name=task case=nom}", "task.b={:term name=task case=nom number=sg}"),
        )
    }

    @Test
    fun `should reject a counted reference with a number`() {
        assertEquals(
            listOf("RU / task.a: ':term' with an operand takes its form from the number; remove the option 'number'"),
            ruErrors(RU_TASK_TERM, "task.a={\$n :term name=task case=nom number=sg}"),
        )
    }

    @Test
    fun `should reject a literal operand`() {
        assertEquals(
            listOf("RU / task.a: ':term' takes an integer variable or no operand, not a literal"),
            ruErrors(RU_TASK_TERM, "task.a={5 :term name=task case=nom}", "task.b={:term name=task case=nom number=sg}"),
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["{\$n :term name=task case=nom}", "{:term name=task case=nom number=sg}"])
    fun `should accept a counted reference with an operand and a bare reference with a number`(message: String) {
        val problems = OperandRule.problemsIn(message)

        assertEquals(emptyList<String>(), problems)
    }
}
