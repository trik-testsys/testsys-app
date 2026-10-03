package tech.testsys.infra.localization.codegen.mf2.function

import tech.testsys.infra.localization.codegen.mf2.function.option.NumberOptions
import tech.testsys.infra.localization.codegen.mf2.model.Operand

/** `:number`: a number; selects by exact value or plural category of the formatted value. */
internal data object NumberFunction : Mf2Function(
    name = "number",
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
        NumberOptions.skeleton,
    ),
) {
    override val isNumeric: Boolean = true
    override val selection: Selection = Selection.NUMERIC

    override fun operandType(options: Map<String, Operand>): ArgumentType = ArgumentType.NUMBER

    override fun literalProblem(value: String, options: Map<String, Operand>): String? = Literals.numberProblem(value, name)
}
