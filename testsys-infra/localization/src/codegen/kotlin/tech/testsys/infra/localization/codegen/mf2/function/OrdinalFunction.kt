package tech.testsys.infra.localization.codegen.mf2.function

import com.ibm.icu.text.RuleBasedNumberFormat
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.RULES
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionSpec
import tech.testsys.infra.localization.codegen.mf2.function.option.RuleSetCheck
import tech.testsys.infra.localization.codegen.mf2.model.Operand

/** `:ordinal`: an ordinal number by an RBNF ordinal rule set of the region locale. */
internal data object OrdinalFunction : Mf2Function(
    name = "ordinal",
    options = listOf(OptionSpec(RULES, check = RuleSetCheck(RuleBasedNumberFormat.ORDINAL))),
) {
    override val isCustom: Boolean = true
    override val isNumeric: Boolean = true
    override val requiredOptions: List<String> = listOf(RULES)
    override val operandFunctions: Set<Mf2Function> by lazy { setOf(IntegerFunction, OffsetFunction) }

    override fun operandType(options: Map<String, Operand>): ArgumentType = ArgumentType.INT

    override fun literalProblem(value: String, options: Map<String, Operand>): String? = Literals.integerProblem(value, name)
}
