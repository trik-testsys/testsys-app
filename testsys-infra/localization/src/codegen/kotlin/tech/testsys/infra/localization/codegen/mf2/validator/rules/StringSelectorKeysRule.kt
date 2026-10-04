package tech.testsys.infra.localization.codegen.mf2.validator.rules

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.Selection
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedSelector
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext
import tech.testsys.infra.localization.codegen.mf2.validator.SelectorRule

/**
 * Checks the keys of a `:string` selector: each becomes a constant of the generated enum, and the key `other`
 * would collide with the constant `OTHER` that selects `*`.
 */
internal object StringSelectorKeysRule : SelectorRule {
    private const val OTHER = "other"
    private val keyShape = Regex("[a-z][a-z0-9_]*")

    override fun findProblems(selector: ResolvedSelector, context: RuleContext): List<String> {
        if (selector.selection != Selection.TEXT) return emptyList()
        return selector.literalKeys.mapNotNull { key -> keyProblem(key, selector.name) }
    }

    private fun keyProblem(key: String, selector: String): String? = when {
        key == OTHER -> Problems.Selectors.otherKey(key, selector)
        !keyShape.matches(key) -> Problems.Selectors.stringKey(key, selector)
        else -> null
    }
}
