package tech.testsys.infra.localization.codegen.mf2.validator.rules

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.Mf2Function
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionSpec
import tech.testsys.infra.localization.codegen.mf2.model.LiteralOperand
import tech.testsys.infra.localization.codegen.mf2.model.OptionStatus
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedExpression
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedOption
import tech.testsys.infra.localization.codegen.mf2.model.VariableOperand
import tech.testsys.infra.localization.codegen.mf2.validator.FunctionRule
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext

/**
 * Checks every own option of an expression: `u:` options, options ICU ignores and unknown options are rejected;
 * a literal value must pass the check of the option, and a variable value must be a message argument of an option
 * that takes one.
 */
internal object OptionsRule : FunctionRule() {
    override fun findProblems(expression: ResolvedExpression, function: Mf2Function, context: RuleContext): List<String> =
        expression.options.mapNotNull { option -> optionProblem(option, function, context) }

    private fun optionProblem(option: ResolvedOption, function: Mf2Function, context: RuleContext): String? =
        when (val status = option.status) {
            OptionStatus.Unsupported -> Problems.Options.unsupportedNamespace(option.name, OptionNames.U_NAMESPACE)
            OptionStatus.IgnoredByIcu -> Problems.Options.notHonoured(option.name, function.name)
            OptionStatus.Unknown -> Problems.Options.unknown(option.name, function.name)
            is OptionStatus.Supported -> valueProblem(option, status.spec, function, context)
        }

    private fun valueProblem(option: ResolvedOption, spec: OptionSpec, function: Mf2Function, context: RuleContext): String? =
        when (val value = option.value) {
            is LiteralOperand -> spec.literalProblem(value.value, context.locale)?.let { reason ->
                Problems.Values.invalid(value = value.value, option = option.name, function = function.name, reason = reason)
            }
            is VariableOperand -> when {
                spec.variableType == null -> Problems.Options.mustBeLiteral(option = option.name, function = function.name)
                option.isDeclaredVariable ->
                    Problems.Options.mustTakeArgument(option = option.name, function = function.name, variable = value.name)
                else -> null
            }
        }
}
