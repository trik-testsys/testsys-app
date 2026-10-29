// ICU4J 78.1 applies only the last of the precision options of `:number`: with `minimumFractionDigits` and
// `maximumFractionDigits` the minimum is lost. Pinned by OptionHonouredTests ("should apply only the last precision
// option (ICU4J 78_1)").
package tech.testsys.infra.localization.codegen.mf2.icu

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.Mf2Function
import tech.testsys.infra.localization.codegen.mf2.function.NumberFunction
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MAXIMUM_FRACTION_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MAXIMUM_SIGNIFICANT_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MINIMUM_FRACTION_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MINIMUM_SIGNIFICANT_DIGITS
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedExpression
import tech.testsys.infra.localization.codegen.mf2.validator.FunctionRule
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext

/** Rejects every precision option of `:number` but the last one, in the order ICU reads them. */
internal object PrecisionOptionsRule : FunctionRule() {
    private val precisionOptions = listOf(
        MINIMUM_FRACTION_DIGITS,
        MAXIMUM_FRACTION_DIGITS,
        MINIMUM_SIGNIFICANT_DIGITS,
        MAXIMUM_SIGNIFICANT_DIGITS,
    )

    override fun findProblems(expression: ResolvedExpression, function: Mf2Function, context: RuleContext): List<String> {
        if (function != NumberFunction) return emptyList()
        val precision = precisionOptions.filter { it in expression.value.options }
        val condition = precision.lastOrNull()?.let(Problems.Options::togetherWith)
        return precision.dropLast(1).map { Problems.Options.notHonoured(it, function.name, condition) }
    }
}
