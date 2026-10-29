package tech.testsys.infra.localization.codegen.mf2.icu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.ruErrors

class CompactDisplayRuleTests {

    @Test
    fun `should reject compactDisplay without compact notation`() {
        assertEquals(
            listOf("RU / task.a: option 'compactDisplay' of ':number' is not honoured by ICU4J 78.1 without notation=compact"),
            ruErrors("task.a={\$x :number compactDisplay=long}"),
        )
    }

    @Test
    fun `should accept compactDisplay with compact notation`() {
        val problems = CompactDisplayRule.problemsIn("{\$x :number notation=compact compactDisplay=long}")

        assertEquals(emptyList<String>(), problems)
    }
}
