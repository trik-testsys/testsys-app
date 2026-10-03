package tech.testsys.infra.localization.codegen.mf2.validator.rules

import tech.testsys.infra.localization.codegen.mf2.function.Mf2Function
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedExpression
import tech.testsys.infra.localization.codegen.mf2.validator.FunctionRule
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext

/** Checks the combined options of an expression against the MF2 combination rules of its function. */
internal object OptionCombinationRule : FunctionRule() {
    override fun findProblems(expression: ResolvedExpression, function: Mf2Function, context: RuleContext): List<String> =
        function.combinationProblems(expression.value.options)
}
