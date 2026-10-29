package tech.testsys.infra.localization.codegen.mf2.function

import tech.testsys.infra.localization.codegen.mf2.function.option.NumberOptions
import tech.testsys.infra.localization.codegen.mf2.model.Operand

/** `:percent`: a number multiplied by 100 and shown as a percentage; selects on the multiplied value. */
internal data object PercentFunction : Mf2Function(
    name = "percent",
    options = listOf(
        NumberOptions.select,
        NumberOptions.notation,
        NumberOptions.compactDisplay,
        NumberOptions.numberingSystem,
        NumberOptions.signDisplay,
        NumberOptions.useGrouping,
        NumberOptions.minimumIntegerDigits,
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
    override val selection: Selection = Selection.NUMERIC

    override fun operandType(options: Map<String, Operand>): ArgumentType = ArgumentType.NUMBER

    override fun literalProblem(value: String, options: Map<String, Operand>): String? = Literals.numberProblem(value, name)
}
