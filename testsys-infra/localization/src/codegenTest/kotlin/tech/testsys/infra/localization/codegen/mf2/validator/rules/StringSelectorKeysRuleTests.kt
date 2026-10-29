package tech.testsys.infra.localization.codegen.mf2.validator.rules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.ruErrors

class StringSelectorKeysRuleTests {

    @Test
    fun `should reject the literal key other on a string selector`() {
        assertEquals(
            listOf("RU / task.a: the key 'other' of '\$s' collides with the generated OTHER constant and the '*' variant; use '*'"),
            ruErrors("task.a=.input {\$s :string} .match \$s other {{a}} * {{b}}"),
        )
    }

    @Test
    fun `should reject a string key that is not an identifier`() {
        assertEquals(
            listOf("RU / task.a: the key 'Female' of '\$s' must match [a-z][a-z0-9_]*"),
            ruErrors("task.a=.input {\$s :string} .match \$s Female {{a}} * {{b}}"),
        )
    }

    @Test
    fun `should accept identifier keys and the catch-all key`() {
        val problems = StringSelectorKeysRule.problemsIn(".input {\$s :string} .match \$s female {{a}} male_2 {{b}} * {{c}}")

        assertEquals(emptyList<String>(), problems)
    }
}
