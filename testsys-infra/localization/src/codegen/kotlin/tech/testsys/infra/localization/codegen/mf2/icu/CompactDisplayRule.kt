// ICU4J 78.1 ignores `compactDisplay` of `:number` unless `notation=compact` is set. Pinned by OptionHonouredTests
// ("should ignore compactDisplay without compact notation (ICU4J 78_1)").
package tech.testsys.infra.localization.codegen.mf2.icu

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.Mf2Function
import tech.testsys.infra.localization.codegen.mf2.function.NumberFunction
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.COMPACT
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.COMPACT_DISPLAY
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.NOTATION
import tech.testsys.infra.localization.codegen.mf2.model.LiteralOperand
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedExpression
import tech.testsys.infra.localization.codegen.mf2.validator.FunctionRule
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext

/** Rejects `compactDisplay` of `:number` without `notation=compact`. */
internal object CompactDisplayRule : FunctionRule() {
    override fun findProblems(expression: ResolvedExpression, function: Mf2Function, context: RuleContext): List<String> {
        val options = expression.value.options
        val isIgnored = function == NumberFunction && COMPACT_DISPLAY in options && options[NOTATION] != LiteralOperand(COMPACT)
        if (!isIgnored) return emptyList()
        return listOf(Problems.Options.notHonoured(COMPACT_DISPLAY, function.name, Problems.Options.withoutValue(NOTATION, COMPACT)))
    }
}
