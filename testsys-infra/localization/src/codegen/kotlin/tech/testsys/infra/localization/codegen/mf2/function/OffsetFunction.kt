package tech.testsys.infra.localization.codegen.mf2.function

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.option.NumberOptions
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.ADD
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.SUBTRACT
import tech.testsys.infra.localization.codegen.mf2.model.Operand
import tech.testsys.infra.localization.codegen.mf2.model.Value

/** `:offset`: a number shifted by `add` or `subtract`; selects like the value it shifts. */
internal data object OffsetFunction : Mf2Function(
    name = "offset",
    options = listOf(NumberOptions.digits(ADD, isVariable = false), NumberOptions.digits(SUBTRACT, isVariable = false)),
) {
    override val isNumeric: Boolean = true
    override val selection: Selection = Selection.NUMERIC
    override val operandFunctions: Set<Mf2Function> by lazy { setOf(IntegerFunction, NumberFunction, OffsetFunction) }

    override fun operandType(options: Map<String, Operand>): ArgumentType = ArgumentType.NUMBER

    override fun producesInteger(base: Value, options: Map<String, Operand>): Boolean = base.isIntegerOperand

    override fun combinationProblems(options: Map<String, Operand>): List<String> {
        val shifts = listOf(ADD, SUBTRACT).count { it in options }
        return if (shifts == 1) emptyList() else listOf(Problems.Options.exactlyOne(name, ADD, SUBTRACT))
    }

    override fun literalProblem(value: String, options: Map<String, Operand>): String? = Literals.numberProblem(value, name)
}
