// ICU4J 78.1 ignores `fields` without `length` and `length` without `fields` (`dateFields` and `dateLength` of
// `:datetime`): it formats the default date. Pinned by OptionHonouredTests: the "without" samples of the
// `fields`, `length`, `dateFields` and `dateLength` cases of "should change the output if a whitelisted option is set".
package tech.testsys.infra.localization.codegen.mf2.icu

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.DateFunction
import tech.testsys.infra.localization.codegen.mf2.function.DateTimeFunction
import tech.testsys.infra.localization.codegen.mf2.function.Mf2Function
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.DATE_FIELDS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.DATE_LENGTH
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.FIELDS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.LENGTH
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedExpression
import tech.testsys.infra.localization.codegen.mf2.validator.FunctionRule
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext

/** Rejects one option of the date pair of `:date` and `:datetime` without the other. */
internal object DateOptionPairRule : FunctionRule() {
    private val pairs: Map<Mf2Function, Pair<String, String>> = mapOf(
        DateFunction to (FIELDS to LENGTH),
        DateTimeFunction to (DATE_FIELDS to DATE_LENGTH),
    )

    override fun findProblems(expression: ResolvedExpression, function: Mf2Function, context: RuleContext): List<String> {
        val (first, second) = pairs[function] ?: return emptyList()
        val options = expression.value.options
        val problem = when {
            first in options && second !in options -> Problems.Options.notHonoured(first, function.name, Problems.Options.without(second))
            second in options && first !in options -> Problems.Options.notHonoured(second, function.name, Problems.Options.without(first))
            else -> null
        }
        return listOfNotNull(problem)
    }
}
