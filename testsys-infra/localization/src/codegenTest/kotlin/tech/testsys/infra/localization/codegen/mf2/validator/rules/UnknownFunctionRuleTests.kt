package tech.testsys.infra.localization.codegen.mf2.validator.rules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.ruErrors

class UnknownFunctionRuleTests {

    @Test
    fun `should reject icu gender`() {
        assertEquals(
            listOf("RU / task.a: function ':icu:gender' is not supported: it duplicates ':string'; use ':string'"),
            ruErrors("task.a={\$x :icu:gender}"),
        )
    }

    @Test
    fun `should reject a namespaced function`() {
        assertEquals(
            listOf("RU / task.a: function ':foo:bar' is not supported: namespaced functions other than the supported ones are rejected"),
            ruErrors("task.a={\$x :foo:bar}"),
        )
    }

    @Test
    fun `should reject an unknown function`() {
        assertEquals(listOf("RU / task.a: unknown function ':money'"), ruErrors("task.a={\$x :money}"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["{\$x :number}", "{\$x :string}", "{\$n :spellout rules=spellout-numbering}", "{\$m :unit unit=meter}"])
    fun `should accept a built-in or custom function`(message: String) {
        val problems = UnknownFunctionRule.problemsIn(message)

        assertEquals(emptyList<String>(), problems)
    }
}
