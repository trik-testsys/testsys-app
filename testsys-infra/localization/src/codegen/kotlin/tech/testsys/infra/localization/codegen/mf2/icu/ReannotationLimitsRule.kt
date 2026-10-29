// ICU4J 78.1 applies a function that annotates a declared value to the declaration's operand: over an `:offset`
// value it formats the unshifted operand, an `:offset` of an `:offset` keeps only the last shift, and a built-in
// function over a value declared with another function applies the declaration's options too. An `:offset` of a
// `:number` value inherits its options, but ICU applies notation, compact display and the fraction and minimum
// significant digits only to `:number` and drops them. Pinned by OptionHonouredTests ("should format an offset value
// re-annotated with integer unshifted (ICU4J 78_1)", "should keep only the last shift of an offset of an offset
// (ICU4J 78_1)", "should apply the declaration options to a built-in function over a value declared with another
// function (ICU4J 78_1)", "should drop number-only options of the value an offset shifts (ICU4J 78_1)").
package tech.testsys.infra.localization.codegen.mf2.icu

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.Mf2Function
import tech.testsys.infra.localization.codegen.mf2.function.NumberFunction
import tech.testsys.infra.localization.codegen.mf2.function.OffsetFunction
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.COMPACT_DISPLAY
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MAXIMUM_FRACTION_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MINIMUM_FRACTION_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MINIMUM_SIGNIFICANT_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.NOTATION
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedExpression
import tech.testsys.infra.localization.codegen.mf2.validator.FunctionRule
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext

/**
 * Rejects the re-annotations of declared values that ICU4J 78.1 formats wrongly and the options of a `:number` value
 * that an `:offset` of it drops. Functions whose operands MF2 restricts (`Mf2Function.operandFunctions`) are checked
 * by the MF2 rule instead, except for an `:offset` of an `:offset`.
 */
internal object ReannotationLimitsRule : FunctionRule() {
    // The options ICU4J 78.1 applies only when the function is `:number`.
    private val numberOnlyOptions = listOf(
        NOTATION,
        COMPACT_DISPLAY,
        MINIMUM_FRACTION_DIGITS,
        MAXIMUM_FRACTION_DIGITS,
        MINIMUM_SIGNIFICANT_DIGITS,
    )

    override fun findProblems(expression: ResolvedExpression, function: Mf2Function, context: RuleContext): List<String> {
        val declared = expression.base.function ?: return emptyList()
        return listOfNotNull(reannotationProblem(function, declared)) + droppedOptionProblems(expression, function, declared)
    }

    private fun reannotationProblem(function: Mf2Function, declared: Mf2Function): String? = when {
        function == OffsetFunction && declared == OffsetFunction -> Problems.Functions.offsetOfOffset(OffsetFunction.name)
        function.operandFunctions != null -> null
        declared == OffsetFunction -> Problems.Functions.overOffset(function.name, OffsetFunction.name)
        declared != function -> Problems.Functions.declaredWith(function.name, declared.name)
        else -> null
    }

    private fun droppedOptionProblems(expression: ResolvedExpression, function: Mf2Function, declared: Mf2Function): List<String> {
        if (function != OffsetFunction || declared != NumberFunction) return emptyList()
        return numberOnlyOptions
            .filter { it in expression.value.options }
            .map { Problems.Functions.offsetDropsOption(OffsetFunction.name, NumberFunction.name, it) }
    }
}
