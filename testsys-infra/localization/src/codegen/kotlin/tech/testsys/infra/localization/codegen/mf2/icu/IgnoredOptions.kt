// ICU4J 78.1 silently ignores these options of its built-in functions: the output does not change when they are
// set. `calendar` of `:time` is applied, but the time alone never shows it, so it has no effect either. Pinned by
// OptionHonouredTests ("should still ignore an option rejected as not honoured", "should cover every option rejected
// as not honoured" and "should cover exactly the whitelisted options of the built-in functions").
package tech.testsys.infra.localization.codegen.mf2.icu

import tech.testsys.infra.localization.codegen.mf2.function.CurrencyFunction
import tech.testsys.infra.localization.codegen.mf2.function.IntegerFunction
import tech.testsys.infra.localization.codegen.mf2.function.Mf2Function
import tech.testsys.infra.localization.codegen.mf2.function.NumberFunction
import tech.testsys.infra.localization.codegen.mf2.function.PercentFunction
import tech.testsys.infra.localization.codegen.mf2.function.TimeFunction
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.CALENDAR
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.COMPACT_DISPLAY
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.CURRENCY_SIGN
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.FRACTION_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.ICU_SKELETON
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MAXIMUM_FRACTION_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MAXIMUM_SIGNIFICANT_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MINIMUM_FRACTION_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MINIMUM_INTEGER_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MINIMUM_SIGNIFICANT_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.NOTATION
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.ROUNDING_INCREMENT
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.ROUNDING_MODE
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.ROUNDING_PRIORITY
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.TRAILING_ZERO_DISPLAY

/**
 * The options ICU4J 78.1 ignores or applies without an effect on the output, per function. [STYLE] is not an MF2
 * option of `:number`, but ICU reads and ignores it; `icu:skeleton` of `:percent` and `:currency` is applied wrongly
 * rather than ignored, and rejected the same way.
 */
internal object IgnoredOptions {
    const val STYLE = "style"

    private val rounding = setOf(ROUNDING_PRIORITY, ROUNDING_INCREMENT, ROUNDING_MODE, TRAILING_ZERO_DISPLAY)
    private val byFunction: Map<Mf2Function, Set<String>> = mapOf(
        IntegerFunction to setOf(MINIMUM_INTEGER_DIGITS, MAXIMUM_SIGNIFICANT_DIGITS),
        NumberFunction to setOf(MINIMUM_INTEGER_DIGITS) + rounding + STYLE,
        PercentFunction to setOf(
            MINIMUM_INTEGER_DIGITS, MINIMUM_FRACTION_DIGITS, MAXIMUM_FRACTION_DIGITS, MINIMUM_SIGNIFICANT_DIGITS,
        ) + rounding + setOf(NOTATION, COMPACT_DISPLAY, ICU_SKELETON),
        CurrencyFunction to setOf(
            CURRENCY_SIGN, NOTATION, COMPACT_DISPLAY, MINIMUM_INTEGER_DIGITS, FRACTION_DIGITS, MINIMUM_FRACTION_DIGITS,
            MAXIMUM_FRACTION_DIGITS, MINIMUM_SIGNIFICANT_DIGITS,
        ) + rounding + ICU_SKELETON,
        TimeFunction to setOf(CALENDAR),
    )

    /** Returns the options of [function] that ICU4J 78.1 ignores. */
    fun of(function: Mf2Function): Set<String> = byFunction[function].orEmpty()

    /** Returns the options of [function] that ICU4J 78.1 applies: its MF2 options minus the ignored ones. */
    fun honouredOptions(function: Mf2Function): Set<String> = function.options.keys - of(function)
}
