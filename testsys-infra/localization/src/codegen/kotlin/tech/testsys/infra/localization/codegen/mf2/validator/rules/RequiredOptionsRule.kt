package tech.testsys.infra.localization.codegen.mf2.validator.rules

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.Mf2Function
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedExpression
import tech.testsys.infra.localization.codegen.mf2.validator.FunctionRule
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext

/** Rejects an expression whose combined options lack a required option of its function. */
internal object RequiredOptionsRule : FunctionRule() {
    override fun findProblems(expression: ResolvedExpression, function: Mf2Function, context: RuleContext): List<String> {
        val missing = function.requiredOptions.filter { it !in expression.value.options }
        return missing.map { Problems.Options.required(function.name, it) }
    }
}
