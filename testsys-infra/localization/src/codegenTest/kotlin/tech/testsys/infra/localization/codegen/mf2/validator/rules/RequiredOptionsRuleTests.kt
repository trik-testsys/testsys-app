package tech.testsys.infra.localization.codegen.mf2.validator.rules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn

class RequiredOptionsRuleTests {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "{\$m :unit unit=meter}",
            "{\$n :spellout rules=spellout-numbering}",
            "{\$n :ordinal rules=digits-ordinal-feminine}",
            "{:term name=task case=nom number=sg}",
        ],
    )
    fun `should accept an expression with the required options of its function`(message: String) {
        val problems = RequiredOptionsRule.problemsIn(message)

        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun `should accept a required option inherited from the declaration`() {
        val problems = RequiredOptionsRule.problemsIn(".local \$m = {\$distance :unit unit=meter} {{{\$m :unit unitDisplay=long}}}")

        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun `should reject an expression without a required option`() {
        val problems = RequiredOptionsRule.problemsIn("{\$m :unit}")

        assertEquals(listOf("':unit' needs the option 'unit'"), problems)
    }

    @Test
    fun `should reject a term without name and case`() {
        val problems = RequiredOptionsRule.problemsIn("{:term number=sg}")

        assertEquals(listOf("':term' needs the option 'name'", "':term' needs the option 'case'"), problems)
    }
}
