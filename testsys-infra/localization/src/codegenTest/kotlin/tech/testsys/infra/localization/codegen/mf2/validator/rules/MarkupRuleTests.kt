package tech.testsys.infra.localization.codegen.mf2.validator.rules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.ruErrors

class MarkupRuleTests {

    @Test
    fun `should reject markup`() {
        assertEquals(
            listOf("RU / task.a: markup 'b' is not supported: the API returns plain text and ICU drops markup silently"),
            ruErrors("task.a={#b}важно{/b}"),
        )
    }

    @Test
    fun `should reject closing markup without naming it as opening`() {
        assertEquals(
            listOf("RU / task.a: markup 'b' is not supported: the API returns plain text and ICU drops markup silently"),
            ruErrors("task.a=важно{/b}"),
        )
    }

    @Test
    fun `should accept a message without markup`() {
        val problems = MarkupRule.problemsIn("Важно: {\$x}")

        assertEquals(emptyList<String>(), problems)
    }
}
