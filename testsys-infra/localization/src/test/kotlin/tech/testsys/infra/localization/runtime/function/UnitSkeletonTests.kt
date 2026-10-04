package tech.testsys.infra.localization.runtime.function

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.infra.localization.InternalLocalizationApi

@OptIn(InternalLocalizationApi::class)
class UnitSkeletonTests {

    @ParameterizedTest
    @MethodSource("tech.testsys.infra.localization.runtime.function.UnitSkeletonTests#unitSkeletons")
    fun `should map unit options to a number skeleton`(options: Map<String, Any>, expected: String) {
        assertEquals(expected, UnitSkeleton.of(options))
    }

    @ParameterizedTest
    @ValueSource(strings = ["meter scale/1000", "meter usage/road"])
    fun `should reject a unit that is not one ICU unit identifier`(unit: String) {
        val exception = assertThrows<IllegalArgumentException> { UnitSkeleton.of(mapOf("unit" to unit)) }

        assertEquals("':unit' option 'unit' has value '$unit', expected one ICU unit identifier", exception.message)
    }

    @Test
    fun `should cover every supported unit option by a mapping case`() {
        val covered = unitSkeletons().flatMap { (it.get()[0] as? Map<*, *>).orEmpty().keys }.map { it.toString() }.toSet()

        assertEquals(UnitSkeleton.options, covered)
    }

    companion object {
        @JvmStatic
        fun unitSkeletons(): List<Arguments> = listOf(
            Arguments.of(mapOf("unit" to "meter"), "unit/meter"),
            Arguments.of(mapOf("unit" to "meter", "usage" to "road"), "unit/meter usage/road"),
            Arguments.of(mapOf("unit" to "meter", "unitDisplay" to "long"), "unit/meter unit-width-full-name"),
            Arguments.of(mapOf("unit" to "meter", "notation" to "scientific"), "unit/meter scientific"),
            Arguments.of(mapOf("unit" to "meter", "notation" to "compact", "compactDisplay" to "long"), "unit/meter compact-long"),
            Arguments.of(mapOf("unit" to "meter", "numberingSystem" to "arab"), "unit/meter numbering-system/arab"),
            Arguments.of(mapOf("unit" to "meter", "signDisplay" to "exceptZero"), "unit/meter sign-except-zero"),
            Arguments.of(mapOf("unit" to "meter", "useGrouping" to "never"), "unit/meter group-off"),
            Arguments.of(mapOf("unit" to "meter", "minimumIntegerDigits" to "3"), "unit/meter integer-width/*000"),
            Arguments.of(mapOf("unit" to "meter", "minimumFractionDigits" to "1"), "unit/meter .0##"),
            Arguments.of(mapOf("unit" to "meter", "maximumFractionDigits" to 2), "unit/meter .##"),
            Arguments.of(mapOf("unit" to "meter", "minimumSignificantDigits" to "2"), "unit/meter @@###################"),
            Arguments.of(mapOf("unit" to "meter", "maximumSignificantDigits" to "3"), "unit/meter @##"),
            Arguments.of(
                mapOf(
                    "unit" to "meter",
                    "maximumFractionDigits" to "2",
                    "maximumSignificantDigits" to "3",
                    "roundingPriority" to "morePrecision",
                ),
                "unit/meter .##/@##r",
            ),
            Arguments.of(
                mapOf(
                    "unit" to "meter",
                    "maximumFractionDigits" to "2",
                    "maximumSignificantDigits" to "3",
                    "roundingPriority" to "lessPrecision",
                ),
                "unit/meter .##/@##s",
            ),
            Arguments.of(
                mapOf("unit" to "meter", "maximumFractionDigits" to "2", "maximumSignificantDigits" to "3"),
                "unit/meter @##",
            ),
            Arguments.of(mapOf("unit" to "meter", "maximumFractionDigits" to "0"), "unit/meter precision-integer"),
            Arguments.of(
                mapOf("unit" to "meter", "maximumFractionDigits" to "2", "roundingIncrement" to "5"),
                "unit/meter precision-increment/0.05",
            ),
            Arguments.of(mapOf("unit" to "meter", "roundingMode" to "halfEven"), "unit/meter rounding-mode-half-even"),
            Arguments.of(
                mapOf("unit" to "meter", "minimumFractionDigits" to "2", "trailingZeroDisplay" to "stripIfInteger"),
                "unit/meter .00#/w",
            ),
        )
    }
}
