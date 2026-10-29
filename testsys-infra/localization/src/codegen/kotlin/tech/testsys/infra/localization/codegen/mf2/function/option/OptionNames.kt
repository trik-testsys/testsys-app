package tech.testsys.infra.localization.codegen.mf2.function.option

/**
 * Names of the MF2 options the codegen knows, the `u:` namespace and the option values other code compares with.
 * [DIGIT_OPTIONS] are the options that force visible fraction or significant digits: exact keys are not allowed
 * with them because exact matching is implementation-defined there.
 */
internal object OptionNames {
    const val SELECT = "select"
    const val NOTATION = "notation"
    const val COMPACT_DISPLAY = "compactDisplay"
    const val NUMBERING_SYSTEM = "numberingSystem"
    const val SIGN_DISPLAY = "signDisplay"
    const val USE_GROUPING = "useGrouping"
    const val MINIMUM_INTEGER_DIGITS = "minimumIntegerDigits"
    const val MINIMUM_FRACTION_DIGITS = "minimumFractionDigits"
    const val MAXIMUM_FRACTION_DIGITS = "maximumFractionDigits"
    const val MINIMUM_SIGNIFICANT_DIGITS = "minimumSignificantDigits"
    const val MAXIMUM_SIGNIFICANT_DIGITS = "maximumSignificantDigits"
    const val ROUNDING_PRIORITY = "roundingPriority"
    const val ROUNDING_INCREMENT = "roundingIncrement"
    const val ROUNDING_MODE = "roundingMode"
    const val TRAILING_ZERO_DISPLAY = "trailingZeroDisplay"
    const val FRACTION_DIGITS = "fractionDigits"
    const val CURRENCY = "currency"
    const val CURRENCY_DISPLAY = "currencyDisplay"
    const val CURRENCY_SIGN = "currencySign"
    const val ADD = "add"
    const val SUBTRACT = "subtract"
    const val FIELDS = "fields"
    const val LENGTH = "length"
    const val DATE_FIELDS = "dateFields"
    const val DATE_LENGTH = "dateLength"
    const val PRECISION = "precision"
    const val TIME_PRECISION = "timePrecision"
    const val HOUR12 = "hour12"
    const val TIME_ZONE_STYLE = "timeZoneStyle"
    const val CALENDAR = "calendar"
    const val TIME_ZONE = "timeZone"
    const val NAME = "name"
    const val CASE = "case"
    const val NUMBER = "number"
    const val RULES = "rules"
    const val UNIT = "unit"
    const val USAGE = "usage"
    const val UNIT_DISPLAY = "unitDisplay"
    const val ICU_SKELETON = "icu:skeleton"

    // The namespace of the Unicode options `u:id`, `u:dir` and `u:locale`.
    const val U_NAMESPACE = "u:"

    // `timeZone=input` formats a zoned operand in its own zone.
    const val INPUT = "input"

    // `notation=compact`.
    const val COMPACT = "compact"

    val DIGIT_OPTIONS: Set<String> = setOf(
        MINIMUM_FRACTION_DIGITS,
        MAXIMUM_FRACTION_DIGITS,
        MINIMUM_SIGNIFICANT_DIGITS,
        MAXIMUM_SIGNIFICANT_DIGITS,
        MINIMUM_INTEGER_DIGITS,
        ICU_SKELETON,
    )
}
