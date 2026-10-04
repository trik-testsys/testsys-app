package tech.testsys.infra.localization.codegen.mf2.validator.completeness

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.generate
import tech.testsys.infra.localization.codegen.mf2.model.CatchAllKey
import tech.testsys.infra.localization.codegen.mf2.model.LiteralKey
import tech.testsys.infra.localization.codegen.mf2.model.Pattern
import tech.testsys.infra.localization.codegen.mf2.model.Variant
import tech.testsys.infra.localization.codegen.ruErrors

class PluralCompletenessTests {

    @Test
    fun `should reject a reachable category that falls back to the catch-all`() {
        assertEquals(
            listOf("RU / task.a: category 'many' of '\$n' has no explicit variant (falls back to '*')"),
            ruErrors("task.a=.input {\$n :integer} .match \$n one {{a}} few {{b}} * {{c}}"),
        )
    }

    @Test
    fun `should report the other selectors of a missing combination`() {
        assertEquals(
            listOf(
                "RU / task.a: category 'few' of '\$days' has no explicit variant when \$phase = end (falls back to '*')",
                "RU / task.a: category 'few' of '\$days' has no explicit variant when \$phase is any other value (falls back to '*')",
            ),
            ruErrors(
                "task.a=.input {\$phase :string} .input {\$days :integer} .match \$phase \$days " +
                    "start one {{a}} start few {{b}} start many {{c}} end one {{d}} end many {{e}} " +
                    "* one {{f}} * many {{h}} * * {{i}}",
            ),
        )
    }

    @Test
    fun `should accept a category covered by a catch-all key of another selector`() {
        val generated = generate(
            "regions.properties" to "RU=ru-RU",
            "ru-RU/tour.properties" to "tour.a=.input {\$phase :string} .input {\$days :integer} .match \$phase \$days " +
                "end one {{a}} * one {{b}} * few {{c}} * many {{d}} * * {{e}}",
        )

        assertEquals(true, generated.any { it.name == "Tour" })
    }

    @Test
    fun `should require other for a number selector`() {
        assertEquals(
            listOf("RU / task.a: category 'other' of '\$p' has no explicit variant (falls back to '*')"),
            ruErrors("task.a=.input {\$p :number} .match \$p one {{a}} few {{b}} many {{c}} * {{d}}"),
        )
    }

    @Test
    fun `should require other for a percent selector`() {
        assertEquals(
            listOf("RU / task.a: category 'other' of '\$p' has no explicit variant (falls back to '*')"),
            ruErrors("task.a=.input {\$p :percent} .match \$p one {{a}} few {{b}} many {{c}} * {{d}}"),
        )
    }

    @Test
    fun `should require the variant of other and the catch-all to render the same pattern`() {
        assertEquals(
            listOf(
                "RU / task.a: ICU4J 78.1 never matches the key 'other': when \$p in category 'other' it renders the variant '*' " +
                    "instead of 'other'; give both the same pattern",
            ),
            ruErrors("task.a=.input {\$p :number} .match \$p one {{a}} few {{b}} many {{c}} other {{d}} * {{e}}"),
        )
    }

    @Test
    fun `should not require categories if selection is exact`() {
        val generated = generate(
            "regions.properties" to "RU=ru-RU",
            "ru-RU/tour.properties" to "tour.a=.input {\$n :integer select=exact} .match \$n 1 {{a}} * {{b}}",
        )

        assertEquals(true, generated.any { it.name == "Tour" })
    }

    @Test
    fun `should not require a category fully covered by exact keys`() {
        val generated = generate(
            "regions.properties" to "EN=en-US",
            "en-US/tour.properties" to "tour.a=.input {\$n :integer} .match \$n 1 {{one}} other {{many}} * {{many}}",
        )

        assertEquals(true, generated.any { it.name == "Tour" })
    }

    @Test
    fun `should reject selectors with too many value combinations`() {
        val errors = ruErrors("task.a=$WIDE_MESSAGE")

        assertEquals(listOf("RU / task.a: the selectors have more than 10000 value combinations to check completeness"), errors)
    }

    @Test
    fun `should reject combinations whose count exceeds the range of Long`() {
        val selectors = List(64) { SelectorSpace.Text("s$it") }
        val variants = listOf(
            Variant(List(64) { LiteralKey("a") }, Pattern(emptyList())),
            Variant(List(64) { CatchAllKey }, Pattern(emptyList())),
        )

        val problems = PluralCompleteness().findProblems(selectors, variants)

        assertEquals(
            listOf("the selectors have more than 10000 value combinations to check completeness"),
            problems,
        )
    }

    private companion object {
        // Five string selectors with six keys each: 7^5 = 16807 value combinations.
        const val WIDE_MESSAGE = ".input {\$s1 :string} .input {\$s2 :string} .input {\$s3 :string} .input {\$s4 :string} " +
            ".input {\$s5 :string} .match \$s1 \$s2 \$s3 \$s4 \$s5 " +
            "a * * * * {{x}} b * * * * {{x}} c * * * * {{x}} d * * * * {{x}} e * * * * {{x}} f * * * * {{x}} " +
            "* a * * * {{x}} * b * * * {{x}} * c * * * {{x}} * d * * * {{x}} * e * * * {{x}} * f * * * {{x}} " +
            "* * a * * {{x}} * * b * * {{x}} * * c * * {{x}} * * d * * {{x}} * * e * * {{x}} * * f * * {{x}} " +
            "* * * a * {{x}} * * * b * {{x}} * * * c * {{x}} * * * d * {{x}} * * * e * {{x}} * * * f * {{x}} " +
            "* * * * a {{x}} * * * * b {{x}} * * * * c {{x}} * * * * d {{x}} * * * * e {{x}} * * * * f {{x}} " +
            "* * * * * {{y}}"
    }
}
