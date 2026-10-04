package tech.testsys.infra.localization.codegen.mf2.validator.rules

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.Mf2Function
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedExpression
import tech.testsys.infra.localization.codegen.mf2.validator.FunctionRule
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext

/** Rejects a function over a declared value whose function it does not accept (`Mf2Function.operandFunctions`). */
internal object ReannotationRule : FunctionRule() {
    override fun findProblems(expression: ResolvedExpression, function: Mf2Function, context: RuleContext): List<String> {
        val declared = expression.base.function ?: return emptyList()
        val accepted = function.operandFunctions ?: return emptyList()
        return if (declared in accepted) emptyList() else listOf(Problems.Functions.declaredWith(function.name, declared.name))
    }
}
