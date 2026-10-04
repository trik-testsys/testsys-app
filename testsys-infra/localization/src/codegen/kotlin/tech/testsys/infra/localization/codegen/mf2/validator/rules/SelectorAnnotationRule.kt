package tech.testsys.infra.localization.codegen.mf2.validator.rules

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.Functions
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedSelector
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext
import tech.testsys.infra.localization.codegen.mf2.validator.SelectorRule

/**
 * Rejects a selector that is not declared with a function or whose function cannot select. A selector declared with
 * a function the codegen does not support passes: the unknown function rule reports that function.
 */
internal object SelectorAnnotationRule : SelectorRule {
    override fun findProblems(selector: ResolvedSelector, context: RuleContext): List<String> {
        if (selector.hasUnknownFunction) return emptyList()
        val function = selector.value?.function ?: return listOf(Problems.Selectors.noAnnotation(selector.name))
        if (function.selection != null) return emptyList()
        return listOf(Problems.Selectors.notSelectable(function.name, Functions.selectors.map { it.name }))
    }
}
