package tech.testsys.infra.localization.codegen.signature

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.mf2.parser.Mf2Parser
import tech.testsys.infra.localization.codegen.mf2.validator.Resolver
import tech.testsys.infra.localization.codegen.ruErrors

class TypeInferenceTests {

    @Test
    fun `should infer a string selector with all literal keys`() {
        val inference = infer(
            ".input {\$status :string} .match \$status accepted {{A}} pending {{P}} rejected {{R}} * {{Other}}",
        )

        val selector = assertInstanceOf(
            Placeholder.SelectPlaceholder::class.java,
            inference.placeholders.getValue("status"),
        )
        assertEquals("status", selector.name)
        assertEquals(setOf("accepted", "pending", "rejected"), selector.selectVariants)
        assertEquals(emptyList<String>(), inference.problems)
    }

    @Test
    fun `should infer ZonedDateTime without a zone override for input time zone`() {
        val inference = infer("{\$at :datetime timeZone=input}")

        assertInstanceOf(Placeholder.ZonedDateTimePlaceholder::class.java, inference.placeholders.getValue("at"))
        assertEquals(emptyMap<String, DateZoneSpec>(), inference.dateZones)
        assertEquals(emptyList<String>(), inference.problems)
    }

    @Test
    fun `should infer ZoneId and an argument zone specification`() {
        val inference = infer("{\$at :datetime timeZone=\$zone}")

        assertInstanceOf(Placeholder.InstantPlaceholder::class.java, inference.placeholders.getValue("at"))
        assertInstanceOf(Placeholder.ZoneIdPlaceholder::class.java, inference.placeholders.getValue("zone"))
        assertEquals(mapOf("at" to DateZoneSpec.Argument("zone")), inference.dateZones)
        assertEquals(emptyList<String>(), inference.problems)
    }

    @Test
    fun `should infer fixed zones for named and UTC time zones`() {
        val inference = infer("{\$start :date timeZone=|Europe/Moscow|} {\$end :time timeZone=UTC}")

        assertEquals(mapOf("start" to DateZoneSpec.Fixed("Europe/Moscow"), "end" to DateZoneSpec.Fixed("UTC")), inference.dateZones)
        assertEquals(emptyList<String>(), inference.problems)
    }

    @Test
    fun `should omit zone specifications for context time zones`() {
        val inference = infer("{\$at :datetime}")

        assertInstanceOf(Placeholder.InstantPlaceholder::class.java, inference.placeholders.getValue("at"))
        assertEquals(emptyMap<String, DateZoneSpec>(), inference.dateZones)
    }

    @Test
    fun `should keep spellout Number and ordinal Int distinct`() {
        val inference = infer(
            "{\$cardinal :spellout rules=spellout-numbering} {\$ordinal :ordinal rules=digits-ordinal-masculine}",
        )

        assertEquals(mapOf("cardinal" to "Number", "ordinal" to "Int"), inference.placeholders.mapValues { it.value.typeName })
        assertEquals(emptyList<String>(), inference.problems)
    }

    private fun infer(source: String): Inference = TypeInference.infer(Resolver.resolve(Mf2Parser.parse(source)))

    @Test
    fun `should reject one variable used with incompatible types`() {
        assertEquals(
            listOf("RU / task.a: conflicting types for '\$x': Number vs Instant"),
            ruErrors("task.a={\$x :number} {\$x :date}"),
        )
    }

    @Test
    fun `should type every argument by the function that formats it`() {
        val inference = infer("{\$count :integer} {\$amount :number} {\$at :datetime} {\$name}")

        assertEquals(
            mapOf("count" to "Int", "amount" to "Number", "at" to "Instant", "name" to "String"),
            inference.placeholders.mapValues { it.value.typeName },
        )
        assertEquals(emptyList<String>(), inference.problems)
    }

    @Test
    fun `should merge Number and Int uses of one variable into Int`() {
        val inference = infer("{\$n :spellout rules=spellout-numbering} {\$n :integer}")

        assertEquals(mapOf("n" to "Int"), inference.placeholders.mapValues { it.value.typeName })
    }

    @Test
    fun `should type a digit option given as an argument as Int`() {
        val inference = infer("{\$x :number maximumFractionDigits=\$p}")

        assertEquals(mapOf("x" to "Number", "p" to "Int"), inference.placeholders.mapValues { it.value.typeName })
    }

    @Test
    fun `should type the operand of currency without the option currency as CurrencyAmount`() {
        val inference = infer("{\$x :currency}")

        assertEquals(mapOf("x" to "CurrencyAmount"), inference.placeholders.mapValues { it.value.typeName })
    }

    @Test
    fun `should keep the source order of the arguments if an option value is typed before its operand`() {
        val inference = infer("Взнос: {\$fee :currency currency=\$currency}")

        assertEquals(listOf("fee", "currency"), inference.placeholders.keys.toList())
    }
}
