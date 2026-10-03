package tech.testsys.infra.localization.runtime.function

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.infra.localization.InternalLocalizationApi
import java.math.BigDecimal
import java.math.BigInteger

@OptIn(InternalLocalizationApi::class)
class OperandsTests {

    @ParameterizedTest
    @CsvSource("2147483647, -2147483648, 4294967295", "-2147483648, 2147483647, -4294967295")
    fun `should subtract extreme Int offsets without overflow`(add: Int, subtract: Int, expected: String) {
        val number = operandValue(0, mapOf("add" to add, "subtract" to subtract))

        assertEquals(BigDecimal(expected), number)
    }

    @ParameterizedTest
    @CsvSource("9223372036854775807, add, 9223372036854775808", "-9223372036854775808, subtract, -9223372036854775809")
    fun `should shift Long boundaries without overflow`(input: Long, option: String, expected: String) {
        val number = operandValue(input, mapOf(option to 1))

        assertEquals(BigDecimal(expected), number)
    }

    @Test
    fun `should keep a BigInteger larger than Long when shifted`() {
        val number = operandValue(BigInteger("9223372036854775808"), mapOf("subtract" to 1))

        assertEquals(BigDecimal("9223372036854775807"), number)
    }

    @Test
    fun `should shift a decimal fraction exactly`() {
        val number = operandValue(BigDecimal("9223372036854775807.125"), mapOf("add" to 1))

        assertEquals(BigDecimal("9223372036854775808.125"), number)
    }

    @Test
    fun `should keep the original Number when the shift is zero`() {
        val input = BigInteger("9223372036854775808")

        val number = operandValue(input, mapOf("add" to 3, "subtract" to 3))

        assertSame(input, number)
    }

    @Test
    fun `should shift the input of a formatted placeholder`() {
        val input = placeholder(5, "five")

        val number = operandValue(input, mapOf("subtract" to 1))

        assertEquals(BigDecimal("4"), number)
    }

    @ParameterizedTest
    @ValueSource(strings = ["NaN", "Infinity", "-Infinity"])
    fun `should preserve non-finite numbers when shifted`(value: String) {
        val input = value.toDouble()

        val number = operandValue(input, mapOf("add" to 1))

        assertEquals(input, number)
    }
}
