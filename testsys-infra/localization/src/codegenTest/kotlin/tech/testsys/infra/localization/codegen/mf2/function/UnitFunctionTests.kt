package tech.testsys.infra.localization.codegen.mf2.function

import com.ibm.icu.number.NumberFormatter
import com.ibm.icu.number.SkeletonSyntaxException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.mf2.validator.rules.OptionCombinationRule
import tech.testsys.infra.localization.codegen.ruErrors

class UnitFunctionTests {

    @ParameterizedTest
    @CsvSource(
        delimiter = '#',
        value = [
            "{\$m :unit unit=meter compactDisplay=long}#option 'compactDisplay' of ':unit' has no effect without notation=compact",
            "{\$m :unit unit=meter maximumFractionDigits=2 roundingPriority=morePrecision}#option 'roundingPriority' of " +
                "':unit' needs both fraction and significant digit options",
            "{\$m :unit unit=meter roundingIncrement=5}#option 'roundingIncrement' of ':unit' needs 'maximumFractionDigits' " +
                "and no significant digits or 'roundingPriority'",
            "{\$m :unit unit=\$unit usage=road}#option 'usage' of ':unit' needs a literal 'unit'",
        ],
    )
    fun `should reject an option combination the skeleton cannot express`(message: String, problem: String) {
        assertEquals(listOf("RU / solution.a: $problem"), ruErrors("solution.a=$message"))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "{\$m :unit unit=meter notation=compact compactDisplay=long}",
            "{\$m :unit unit=meter maximumFractionDigits=2 maximumSignificantDigits=3 roundingPriority=morePrecision}",
            "{\$m :unit unit=meter maximumFractionDigits=2 roundingIncrement=5}",
            "{\$m :unit unit=meter usage=road}",
        ],
    )
    fun `should accept an option combination the skeleton expresses`(message: String) {
        val problems = OptionCombinationRule.problemsIn(message)

        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun `should reject a rounding mode without a skeleton stem`() {
        assertEquals(
            listOf(
                "RU / solution.a: invalid value 'halfCeil' of option 'roundingMode' of ':unit': expected one of: ceil, floor, " +
                    "expand, trunc, halfExpand, halfTrunc, halfEven",
            ),
            ruErrors("solution.a={\$m :unit unit=meter roundingMode=halfCeil}"),
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["rounding-mode-half-ceiling", "rounding-mode-half-floor"])
    fun `should have no skeleton stem for halfCeil and halfFloor (ICU4J 78_1)`(stem: String) {
        assertThrows(SkeletonSyntaxException::class.java) { NumberFormatter.forSkeleton("unit/meter $stem") }
    }
}
