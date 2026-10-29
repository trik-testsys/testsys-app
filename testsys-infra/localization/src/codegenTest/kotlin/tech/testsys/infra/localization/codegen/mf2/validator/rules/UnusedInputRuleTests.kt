package tech.testsys.infra.localization.codegen.mf2.validator.rules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.ruErrors

class UnusedInputRuleTests {

    @Test
    fun `should reject a declared input that is never used`() {
        assertEquals(
            listOf("RU / task.a: the declared input '\$count' is never used"),
            ruErrors("task.a=.input {\$count :integer} {{Всего}}"),
        )
    }

    @Test
    fun `should accept a declared input used in the pattern`() {
        val problems = UnusedInputRule.problemsIn(".input {\$count :integer} {{Всего: {\$count}}}")

        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun `should accept a declared input used only as a selector`() {
        val problems = UnusedInputRule.problemsIn(".input {\$count :integer} .match \$count one {{Один}} * {{Много}}")

        assertEquals(emptyList<String>(), problems)
    }
}
