package tech.testsys.infra.localization.codegen.mf2.function

import tech.testsys.infra.localization.codegen.mf2.function.option.NumberOptions
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames
import tech.testsys.infra.localization.codegen.mf2.model.Operand
import tech.testsys.infra.localization.codegen.mf2.model.Value

/** `:integer`: a number formatted as an integer; selects by exact value or plural category. */
internal data object IntegerFunction : Mf2Function(
    name = "integer",
    options = listOf(
        NumberOptions.select,
        NumberOptions.numberingSystem,
        NumberOptions.signDisplay,
        NumberOptions.useGrouping,
        NumberOptions.minimumIntegerDigits,
        NumberOptions.maximumSignificantDigits,
        NumberOptions.skeleton,
    ),
) {
    override val isNumeric: Boolean = true
    override val selection: Selection = Selection.NUMERIC

    override fun operandType(options: Map<String, Operand>): ArgumentType = ArgumentType.INT

    // A skeleton can show fraction digits.
    override fun producesInteger(base: Value, options: Map<String, Operand>): Boolean = OptionNames.ICU_SKELETON !in options

    override fun literalProblem(value: String, options: Map<String, Operand>): String? = Literals.numberProblem(value, name)
}
