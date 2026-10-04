// ICU4J 78.1 passes every message argument to the built-in number functions as an option, so an argument named
// like a number option silently changes the formatting of every number in the message. Pinned by
// OptionHonouredTests ("should pass message arguments to number functions as options (ICU4J 78_1)").
package tech.testsys.infra.localization.codegen.mf2.icu

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.Functions
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.ADD
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.COMPACT_DISPLAY
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.CURRENCY
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.CURRENCY_DISPLAY
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.CURRENCY_SIGN
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MAXIMUM_FRACTION_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MAXIMUM_SIGNIFICANT_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MINIMUM_FRACTION_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MINIMUM_INTEGER_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MINIMUM_SIGNIFICANT_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.NOTATION
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.NUMBERING_SYSTEM
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.SIGN_DISPLAY
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.SUBTRACT
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.USE_GROUPING
import tech.testsys.infra.localization.codegen.mf2.model.ReferenceKind
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedExpression
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedMessage
import tech.testsys.infra.localization.codegen.mf2.model.VariableOperand
import tech.testsys.infra.localization.codegen.mf2.validator.MessageRule
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext

/**
 * Rejects an argument named like a number option in a message with a built-in number function, unless the argument
 * is only the value of the option of the same name (`currency=$currency`) and every number expression of the message
 * takes that option from it.
 */
internal object LeakingArgumentsRule : MessageRule {
    // The options ICU4J 78.1 number functions read from the map of all message arguments.
    private val leakingNames = setOf(
        NOTATION, COMPACT_DISPLAY, IgnoredOptions.STYLE, MINIMUM_FRACTION_DIGITS, MAXIMUM_FRACTION_DIGITS,
        MINIMUM_SIGNIFICANT_DIGITS, MAXIMUM_SIGNIFICANT_DIGITS, MINIMUM_INTEGER_DIGITS, NUMBERING_SYSTEM, SIGN_DISPLAY,
        USE_GROUPING, CURRENCY, CURRENCY_SIGN, CURRENCY_DISPLAY, ADD, SUBTRACT,
    )
    private val operandKinds = setOf(ReferenceKind.INPUT_OPERAND, ReferenceKind.OPERAND)

    override fun findProblems(message: ResolvedMessage, context: RuleContext): List<String> {
        val numberExpressions = message.expressions.filter(::usesNumberFunction)
        if (numberExpressions.isEmpty()) return emptyList()
        val operands = message.references.filter { it.kind in operandKinds && !it.isBound }.map { it.name }.toSet()
        val options = optionsByArgument(message)
        return message.arguments
            .filter { it in leakingNames }
            .filterNot { argument ->
                argument !in operands && options[argument].orEmpty() == setOf(argument) && isOwnValueOfAll(argument, numberExpressions)
            }
            .map(Problems.Variables::leakingArgument)
    }

    private fun usesNumberFunction(expression: ResolvedExpression): Boolean {
        val function = expression.knownFunction ?: return false
        return listOfNotNull(function, expression.base.function).any { it in Functions.numericBuiltIns }
    }

    // The argument overrides the option of its name in every number expression, so it is harmless only where it is
    // already the value of that option.
    private fun isOwnValueOfAll(argument: String, numberExpressions: List<ResolvedExpression>): Boolean =
        numberExpressions.all { expression -> expression.value.options[argument] == VariableOperand(argument) }

    // The options each argument is the value of.
    private fun optionsByArgument(message: ResolvedMessage): Map<String, Set<String>> = message.expressions
        .flatMap { expression ->
            expression.options.mapNotNull { option -> option.argument?.let { argument -> argument.name to option.name } }
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, options) -> options.toSet() }
}
