package tech.testsys.infra.localization.runtime.function

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import tech.testsys.infra.localization.InternalLocalizationApi
import tech.testsys.infra.localization.runtime.MOSCOW
import tech.testsys.infra.localization.runtime.ruRuntime
import java.math.BigDecimal
import java.math.BigInteger
import java.util.stream.Stream

@OptIn(InternalLocalizationApi::class)
class RuleSetFunctionTests {

    @ParameterizedTest
    @CsvSource(
        "spellout, spellout-numbering, NaN",
        "spellout, spellout-numbering, Infinity",
        "spellout, spellout-numbering, -Infinity",
        "ordinal, digits-ordinal-masculine, NaN",
        "ordinal, digits-ordinal-masculine, Infinity",
        "ordinal, digits-ordinal-masculine, -Infinity",
    )
    fun `should reject non-finite operands with function context`(function: String, rules: String, value: String) {
        val runtime = ruRuntime("key" to "{\$n :$function rules=$rules}")

        val error = assertThrows(IllegalArgumentException::class.java) {
            runtime.format("key", MOSCOW, "n" to value.toDouble())
        }

        assertEquals("':$function' operand '$value' must be finite (NaN and infinity are not supported)", error.message)
    }

    @ParameterizedTest
    @MethodSource("invalidIntegers")
    fun `should reject integers outside Long with function context`(function: String, rules: String, value: Number) {
        val runtime = ruRuntime("key" to "{\$n :$function rules=$rules}")

        val error = assertThrows(IllegalArgumentException::class.java) {
            runtime.format("key", MOSCOW, "n" to value)
        }

        assertEquals("':$function' operand '$value' is an integer outside the Long range", error.message)
    }

    @ParameterizedTest
    @MethodSource("boundaryOperands")
    fun `should format Long boundaries and their exact decimal representations`(
        function: String,
        rules: String,
        value: Number,
        expected: String,
    ) {
        val runtime = ruRuntime("key" to "{\$n :$function rules=$rules}")

        val actual = runtime.format("key", MOSCOW, "n" to value)

        assertEquals(expected, actual)
    }

    @ParameterizedTest
    @CsvSource(
        "spellout, spellout-numbering, 9223372036854775807, add, 9223372036854775808",
        "spellout, spellout-numbering, -9223372036854775808, subtract, -9223372036854775809",
        "ordinal, digits-ordinal-masculine, 9223372036854775807, add, 9223372036854775808",
        "ordinal, digits-ordinal-masculine, -9223372036854775808, subtract, -9223372036854775809",
    )
    fun `should check the Long range after offset`(function: String, rules: String, value: Long, option: String, shifted: String) {
        val runtime = ruRuntime(
            "key" to ".local \$shifted = {\$n :offset $option=1} {{{\$shifted :$function rules=$rules}}}",
        )

        val error = assertThrows(IllegalArgumentException::class.java) { runtime.format("key", MOSCOW, "n" to value) }

        assertEquals("':$function' operand '$shifted' is an integer outside the Long range", error.message)
    }

    @Test
    fun `should allow an out-of-range integer shifted into Long`() {
        val runtime = ruRuntime(
            "shifted" to ".local \$shifted = {\$n :offset subtract=1} {{{\$shifted :spellout rules=spellout-numbering}}}",
        )

        val actual = runtime.format("shifted", MOSCOW, "n" to BigInteger("9223372036854775808"))

        assertEquals(MAX_TEXT, actual)
    }

    @ParameterizedTest
    @CsvSource("spellout, spellout-numbering", "ordinal, digits-ordinal-masculine")
    fun `should reject a finite fraction that overflows Double`(function: String, rules: String) {
        val runtime = ruRuntime("key" to "{\$n :$function rules=$rules}")
        val value = BigDecimal("1" + "0".repeat(400) + ".5")

        val error = assertThrows(IllegalArgumentException::class.java) { runtime.format("key", MOSCOW, "n" to value) }

        assertEquals("':$function' operand '$value' is a fraction outside the finite Double range", error.message)
    }

    @Test
    fun `should spell out a BigDecimal fraction`() {
        val runtime = ruRuntime("key" to "{\$n :spellout rules=spellout-numbering}")

        assertEquals("одна целая пять десятых", runtime.format("key", MOSCOW, "n" to BigDecimal("1.5")))
    }

    @ParameterizedTest
    @CsvSource(
        "spellout-cardinal-feminine, 2, две",
        "spellout-cardinal-masculine, 2, два",
        "spellout-cardinal-feminine-genitive, 2, двух",
        "spellout-numbering, 21, двадцать один",
    )
    fun `should spell out the number with the given rule set`(rules: String, number: Int, expected: String) {
        val runtime = ruRuntime("key" to "{\$n :spellout rules=$rules}")

        assertEquals(expected, runtime.format("key", MOSCOW, "n" to number))
    }

    @Test
    fun `should spell out a fraction`() {
        val runtime = ruRuntime("key" to "{\$n :spellout rules=spellout-numbering}")

        assertEquals("одна целая пять десятых", runtime.format("key", MOSCOW, "n" to 1.5))
    }

    @ParameterizedTest
    @CsvSource("digits-ordinal-masculine, 1, 1-й", "digits-ordinal-feminine, 1, 1-я", "digits-ordinal-neuter, 3, 3-е")
    fun `should render the ordinal with the given rule set`(rules: String, number: Int, expected: String) {
        val runtime = ruRuntime("key" to "{\$n :ordinal rules=$rules}")

        assertEquals(expected, runtime.format("key", MOSCOW, "n" to number))
    }

    @ParameterizedTest
    @CsvSource("spellout, spellout-numbering, четыре", "ordinal, digits-ordinal-masculine, 4-й")
    fun `should apply the inherited subtract of an offset operand`(function: String, rules: String, expected: String) {
        val runtime = ruRuntime(
            "key" to ".input {\$count :integer} .local \$others = {\$count :offset subtract=1} " +
                "{{{\$others :$function rules=$rules}}}",
        )

        assertEquals(expected, runtime.format("key", MOSCOW, "count" to 5))
    }

    companion object {
        private const val MIN_TEXT = "-9\u00a0223\u00a0372\u00a0036\u00a0854\u00a0775\u00a0808"
        private const val MAX_TEXT = "9\u00a0223\u00a0372\u00a0036\u00a0854\u00a0775\u00a0807"

        @JvmStatic
        fun invalidIntegers(): Stream<Arguments> = listOf(
            Arguments.of("spellout", "spellout-numbering", BigInteger("9223372036854775808")),
            Arguments.of("spellout", "spellout-numbering", BigDecimal("-9223372036854775809")),
            Arguments.of("spellout", "spellout-numbering", BigInteger.TEN.pow(100)),
            Arguments.of("ordinal", "digits-ordinal-masculine", BigInteger("9223372036854775808")),
            Arguments.of("ordinal", "digits-ordinal-masculine", BigDecimal("-9223372036854775809")),
            Arguments.of("ordinal", "digits-ordinal-masculine", BigInteger.TEN.pow(100)),
        ).stream()

        @JvmStatic
        fun boundaryOperands(): Stream<Arguments> = listOf(
            Arguments.of("spellout", "spellout-numbering", Long.MIN_VALUE, MIN_TEXT),
            Arguments.of("spellout", "spellout-numbering", BigInteger("-9223372036854775808"), MIN_TEXT),
            Arguments.of("spellout", "spellout-numbering", BigDecimal("-9223372036854775808.00"), MIN_TEXT),
            Arguments.of("spellout", "spellout-numbering", Long.MAX_VALUE, MAX_TEXT),
            Arguments.of("spellout", "spellout-numbering", BigInteger("9223372036854775807"), MAX_TEXT),
            Arguments.of("spellout", "spellout-numbering", BigDecimal("9223372036854775807.00"), MAX_TEXT),
            Arguments.of("ordinal", "digits-ordinal-masculine", Long.MIN_VALUE, MIN_TEXT),
            Arguments.of("ordinal", "digits-ordinal-masculine", BigInteger("-9223372036854775808"), MIN_TEXT),
            Arguments.of("ordinal", "digits-ordinal-masculine", BigDecimal("-9223372036854775808.00"), MIN_TEXT),
            Arguments.of("ordinal", "digits-ordinal-masculine", Long.MAX_VALUE, "$MAX_TEXT-й"),
            Arguments.of("ordinal", "digits-ordinal-masculine", BigInteger("9223372036854775807"), "$MAX_TEXT-й"),
            Arguments.of("ordinal", "digits-ordinal-masculine", BigDecimal("9223372036854775807.00"), "$MAX_TEXT-й"),
        ).stream()
    }
}
