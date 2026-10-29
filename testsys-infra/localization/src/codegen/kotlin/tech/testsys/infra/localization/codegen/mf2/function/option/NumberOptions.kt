package tech.testsys.infra.localization.codegen.mf2.function.option

import tech.testsys.infra.localization.codegen.mf2.function.ArgumentType
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.COMPACT
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.COMPACT_DISPLAY
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.ICU_SKELETON
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MAXIMUM_FRACTION_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MAXIMUM_SIGNIFICANT_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MINIMUM_FRACTION_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MINIMUM_INTEGER_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MINIMUM_SIGNIFICANT_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.NOTATION
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.NUMBERING_SYSTEM
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.ROUNDING_INCREMENT
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.ROUNDING_MODE
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.ROUNDING_PRIORITY
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.SELECT
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.SIGN_DISPLAY
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.TRAILING_ZERO_DISPLAY
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.USE_GROUPING

/** The options that the MF2 number functions share, with the values MF2 allows. */
internal object NumberOptions {
    val select = OptionSpec(SELECT, check = SelectMode.CHECK)
    val notation = OptionSpec(NOTATION, check = EnumValues("standard", "scientific", "engineering", COMPACT))
    val compactDisplay = OptionSpec(COMPACT_DISPLAY, check = EnumValues("short", "long"))
    val numberingSystem = OptionSpec(NUMBERING_SYSTEM, check = NumberingSystemCheck)
    val signDisplay = OptionSpec(SIGN_DISPLAY, check = EnumValues("auto", "always", "exceptZero", "negative", "never"))
    val useGrouping = OptionSpec(USE_GROUPING, check = EnumValues("auto", "always", "never", "min2"))
    val skeleton = OptionSpec(ICU_SKELETON, check = NumberSkeletonCheck)
    val minimumIntegerDigits = digits(MINIMUM_INTEGER_DIGITS, minimum = 1)
    val minimumFractionDigits = digits(MINIMUM_FRACTION_DIGITS)
    val maximumFractionDigits = digits(MAXIMUM_FRACTION_DIGITS)
    val minimumSignificantDigits = digits(MINIMUM_SIGNIFICANT_DIGITS, minimum = 1)
    val maximumSignificantDigits = digits(MAXIMUM_SIGNIFICANT_DIGITS, minimum = 1)
    val roundingPriority = OptionSpec(ROUNDING_PRIORITY, check = EnumValues("auto", "morePrecision", "lessPrecision"))
    val roundingIncrement = OptionSpec(
        ROUNDING_INCREMENT,
        check = EnumValues(
            "1", "2", "5", "10", "20", "25", "50", "100", "200", "250", "500", "1000", "2000", "2500", "5000",
        ),
    )
    val roundingMode = OptionSpec(
        ROUNDING_MODE,
        check = EnumValues(
            "ceil", "floor", "expand", "trunc", "halfCeil", "halfFloor", "halfExpand", "halfTrunc", "halfEven",
        ),
    )
    val trailingZeroDisplay = OptionSpec(TRAILING_ZERO_DISPLAY, check = EnumValues("auto", "stripIfInteger"))

    /** A digit size option [name] from [minimum]; its value may be an `Int` argument if [isVariable]. */
    fun digits(name: String, minimum: Int = 0, isVariable: Boolean = true): OptionSpec =
        OptionSpec(name, ArgumentType.INT.takeIf { isVariable }, DigitSize(minimum))
}
