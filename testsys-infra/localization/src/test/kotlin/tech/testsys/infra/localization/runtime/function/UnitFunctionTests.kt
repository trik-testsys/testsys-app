package tech.testsys.infra.localization.runtime.function

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.infra.localization.InternalLocalizationApi
import tech.testsys.infra.localization.runtime.MOSCOW
import tech.testsys.infra.localization.runtime.ruRuntime

@OptIn(InternalLocalizationApi::class)
class UnitFunctionTests {

    @ParameterizedTest
    @CsvSource("NaN, не\u00a0число м", "Infinity, ∞ м", "-Infinity, -∞ м")
    fun `should preserve ICU formatting of non-finite operands after offset`(value: String, expected: String) {
        val runtime = ruRuntime(
            "shifted" to ".local \$shifted = {\$n :offset add=1} {{{\$shifted :unit unit=meter}}}",
        )
        val number = value.toDouble()

        val actual = runtime.format("shifted", MOSCOW, "n" to number)

        assertEquals(expected, actual)
    }

    @Test
    fun `should format the unit through the skeleton`() {
        val runtime = ruRuntime("key" to "{\$m :unit unit=kilometer-per-hour unitDisplay=long maximumFractionDigits=0}")

        assertEquals("13 километров в час", runtime.format("key", MOSCOW, "m" to 12.7))
    }

    @Test
    fun `should take the unit from a variable option`() {
        val runtime = ruRuntime("key" to "{\$m :unit unit=\$u}")

        assertEquals("5 кг", runtime.format("key", MOSCOW, "m" to 5, "u" to "kilogram"))
    }

    @Test
    fun `should reject a unit argument that adds skeleton stems`() {
        val runtime = ruRuntime("key" to "{\$m :unit unit=\$u}")

        val exception = assertThrows<IllegalArgumentException> {
            runtime.format("key", MOSCOW, "m" to 5, "u" to "meter scale/1000")
        }

        assertEquals("':unit' option 'unit' has value 'meter scale/1000', expected one ICU unit identifier", exception.message)
    }

    @ParameterizedTest
    @CsvSource("morePrecision, '123,46 м'", "lessPrecision, 123 м")
    fun `should resolve fraction and significant digits by the rounding priority`(priority: String, expected: String) {
        val runtime = ruRuntime(
            "key" to "{\$m :unit unit=meter maximumFractionDigits=2 maximumSignificantDigits=3 roundingPriority=$priority}",
        )

        assertEquals(expected, runtime.format("key", MOSCOW, "m" to 123.456))
    }

    @Test
    fun `should apply the inherited subtract of an offset operand`() {
        val runtime = ruRuntime(
            "key" to ".input {\$count :integer} .local \$others = {\$count :offset subtract=1} {{{\$others :unit unit=meter}}}",
        )

        assertEquals("4 м", runtime.format("key", MOSCOW, "count" to 5))
    }

    @Test
    fun `should format a Short operand`() {
        val runtime = ruRuntime("key" to "{\$m :unit unit=megabyte}")

        assertEquals("12 МБ", runtime.format("key", MOSCOW, "m" to 12.toShort()))
    }
}
