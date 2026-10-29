package tech.testsys.infra.localization.codegen.mf2.function

import com.ibm.icu.text.RuleBasedNumberFormat
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.RULES
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionSpec
import tech.testsys.infra.localization.codegen.mf2.function.option.RuleSetCheck
import tech.testsys.infra.localization.codegen.mf2.model.Operand

/** `:spellout`: a number in words by an RBNF spellout rule set of the region locale. */
internal data object SpelloutFunction : Mf2Function(
    name = "spellout",
    options = listOf(OptionSpec(RULES, check = RuleSetCheck(RuleBasedNumberFormat.SPELLOUT))),
) {
    override val isCustom: Boolean = true
    override val isNumeric: Boolean = true
    override val requiredOptions: List<String> = listOf(RULES)
    override val operandFunctions: Set<Mf2Function> by lazy { setOf(IntegerFunction, NumberFunction, OffsetFunction) }

    override fun operandType(options: Map<String, Operand>): ArgumentType = ArgumentType.NUMBER

    override fun literalProblem(value: String, options: Map<String, Operand>): String? = Literals.numberProblem(value, name)
}
