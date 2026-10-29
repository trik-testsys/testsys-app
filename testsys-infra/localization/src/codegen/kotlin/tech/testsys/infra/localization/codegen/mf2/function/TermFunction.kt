package tech.testsys.infra.localization.codegen.mf2.function

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.option.EnumValues
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.CASE
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.NAME
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.NUMBER
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionSpec
import tech.testsys.infra.localization.codegen.mf2.function.option.PatternCheck
import tech.testsys.infra.localization.codegen.mf2.model.LiteralOperand
import tech.testsys.infra.localization.codegen.mf2.model.Operand

/** The shape of a term name and of a case key of a term. */
internal val termIdentifier: Regex = Regex("[a-z][a-z0-9_]*")

/**
 * `:term`: a glossary term in the case of the option `case` and in the form the integer operand selects, or the
 * form `number=sg|pl` without an operand. [SINGULAR] and [PLURAL] are the values of `number`.
 */
internal data object TermFunction : Mf2Function(
    name = "term",
    options = listOf(
        OptionSpec(NAME, check = PatternCheck(termIdentifier, Problems.Values.TERM_NAME)),
        OptionSpec(CASE, check = PatternCheck(termIdentifier, Problems.Values.CASE_KEY)),
        OptionSpec(NUMBER, check = EnumValues(TermFunction.SINGULAR, TermFunction.PLURAL)),
    ),
) {
    const val SINGULAR = "sg"
    const val PLURAL = "pl"

    override val isCustom: Boolean = true
    override val requiredOptions: List<String> = listOf(NAME, CASE)
    override val operandFunctions: Set<Mf2Function> by lazy { setOf(IntegerFunction, OffsetFunction) }

    override fun operandType(options: Map<String, Operand>): ArgumentType = ArgumentType.INT

    override fun operandProblems(operand: Operand?, own: Map<String, Operand>, options: Map<String, Operand>): List<String> {
        val problem = when {
            operand is LiteralOperand -> Problems.Operands.termLiteral(name)
            operand == null && literal(own, NUMBER) == null -> Problems.Operands.termWithoutNumber(name, NUMBER, SINGULAR, PLURAL)
            operand != null && NUMBER in own -> Problems.Operands.termWithNumber(name, NUMBER)
            else -> null
        }
        return listOfNotNull(problem)
    }

    /** Returns the literal value of the [option] among the [own] options of an expression, or `null`. */
    fun literal(own: Map<String, Operand>, option: String): String? = (own[option] as? LiteralOperand)?.value
}
