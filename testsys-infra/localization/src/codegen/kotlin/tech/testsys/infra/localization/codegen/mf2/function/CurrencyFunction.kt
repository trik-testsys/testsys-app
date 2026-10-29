package tech.testsys.infra.localization.codegen.mf2.function

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.option.CurrencyCodeCheck
import tech.testsys.infra.localization.codegen.mf2.function.option.EnumValues
import tech.testsys.infra.localization.codegen.mf2.function.option.NumberOptions
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.CURRENCY
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.CURRENCY_DISPLAY
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.CURRENCY_SIGN
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.FRACTION_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionSpec
import tech.testsys.infra.localization.codegen.mf2.model.Operand

/**
 * `:currency`: an amount of money. Without the option `currency` the operand is a `CurrencyAmount` that carries
 * its currency.
 */
internal data object CurrencyFunction : Mf2Function(
    name = "currency",
    options = listOf(
        OptionSpec(CURRENCY, ArgumentType.STRING, CurrencyCodeCheck),
        OptionSpec(CURRENCY_DISPLAY, check = EnumValues("narrowSymbol", "symbol", "name", "code", "formalSymbol", "never")),
        OptionSpec(CURRENCY_SIGN, check = EnumValues("standard", "accounting")),
        NumberOptions.notation,
        NumberOptions.compactDisplay,
        NumberOptions.numberingSystem,
        NumberOptions.signDisplay,
        NumberOptions.useGrouping,
        NumberOptions.minimumIntegerDigits,
        NumberOptions.digits(FRACTION_DIGITS),
        NumberOptions.minimumFractionDigits,
        NumberOptions.maximumFractionDigits,
        NumberOptions.minimumSignificantDigits,
        NumberOptions.maximumSignificantDigits,
        NumberOptions.roundingPriority,
        NumberOptions.roundingIncrement,
        NumberOptions.roundingMode,
        NumberOptions.trailingZeroDisplay,
    ),
) {
    override val isNumeric: Boolean = true

    override fun operandType(options: Map<String, Operand>): ArgumentType =
        if (CURRENCY in options) ArgumentType.NUMBER else ArgumentType.CURRENCY_AMOUNT

    override fun literalProblem(value: String, options: Map<String, Operand>): String? =
        Literals.numberProblem(value, name) ?: Problems.Operands.literalNeedsOption(name, CURRENCY).takeIf { CURRENCY !in options }
}
