package tech.testsys.infra.localization.codegen.mf2.validator.rules

import tech.testsys.infra.localization.codegen.mf2.function.Mf2Function
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedExpression
import tech.testsys.infra.localization.codegen.mf2.validator.FunctionRule
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext

/** Checks the operand of an expression against the operand rule of its function. */
internal object OperandRule : FunctionRule() {
    override fun findProblems(expression: ResolvedExpression, function: Mf2Function, context: RuleContext): List<String> =
        function.operandProblems(expression.source.operand, expression.ownValues, expression.value.options)
}
