package tech.testsys.infra.localization.codegen.mf2.validator.rules

import com.ibm.icu.util.ULocale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.INTEGER_PLURAL_VARIANTS
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.mf2.validator.ruContext
import tech.testsys.infra.localization.codegen.ruErrors

class NumericSelectorKeysRuleTests {

    @Test
    fun `should reject a key that is not a plural category`() {
        assertEquals(
            listOf("RU / task.a: the key 'abc' of '\$n' is not a plural category of ru-RU (expected one of: few, many, one, other)"),
            ruErrors("task.a=.input {\$n :integer} .match \$n abc {{x}} $INTEGER_PLURAL_VARIANTS"),
        )
    }

    @Test
    fun `should reject a category key if selection is exact`() {
        assertEquals(
            listOf("RU / task.a: the key 'one' of '\$n' is not a number; select=exact allows only exact keys"),
            ruErrors("task.a=.input {\$n :integer select=exact} .match \$n 1 {{a}} one {{b}} * {{c}}"),
        )
    }

    @Test
    fun `should reject an exact key with digit options`() {
        assertEquals(
            listOf(
                "RU / task.a: the exact key '1' of '\$n' is not allowed with digit options: exact matching is " +
                    "implementation-defined there",
            ),
            ruErrors(
                "task.a=.input {\$n :number maximumFractionDigits=1} .match \$n " +
                    "1 {{a}} one {{b}} few {{c}} many {{d}} other {{e}} * {{e}}",
            ),
        )
    }

    @Test
    fun `should reject an exact key with icu skeleton`() {
        assertEquals(
            listOf(
                "RU / task.a: the exact key '1' of '\$n' is not allowed with digit options: exact matching is " +
                    "implementation-defined there",
            ),
            ruErrors(
                "task.a=.input {\$n :number icu:skeleton=.0} .match \$n " +
                    "1 {{a}} one {{b}} few {{c}} many {{d}} other {{e}} * {{e}}",
            ),
        )
    }

    @Test
    fun `should reject an exact key that is not an integer`() {
        assertEquals(
            listOf("RU / task.a: the exact key '1.5' of '\$n' must be an integer: 0 or -?[1-9][0-9]*"),
            ruErrors("task.a=.input {\$n :integer} .match \$n 1.5 {{x}} $INTEGER_PLURAL_VARIANTS"),
        )
    }

    @Test
    fun `should accept integer exact keys and plural categories of the region`() {
        val problems = NumericSelectorKeysRule.problemsIn(".input {\$n :integer} .match \$n 0 {{z}} $INTEGER_PLURAL_VARIANTS")

        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun `should accept integer keys if selection is exact`() {
        val problems = NumericSelectorKeysRule.problemsIn(".input {\$n :integer select=exact} .match \$n 1 {{a}} 2 {{b}} * {{c}}")

        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun `should accept English ordinal categories in the region EN`() {
        val en = RuleContext(ULocale.forLanguageTag("en-US"), ruContext().glossary)

        val problems = NumericSelectorKeysRule.problemsIn(
            ".input {\$n :integer select=ordinal} .match \$n one {{st}} two {{nd}} few {{rd}} * {{th}}",
            en,
        )

        assertEquals(emptyList<String>(), problems)
    }
}
