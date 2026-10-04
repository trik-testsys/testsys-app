package tech.testsys.infra.localization.codegen.mf2.validator.rules

import com.ibm.icu.text.PluralRules
import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.Literals
import tech.testsys.infra.localization.codegen.mf2.function.Selection
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames
import tech.testsys.infra.localization.codegen.mf2.function.option.SelectMode
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedSelector
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext
import tech.testsys.infra.localization.codegen.mf2.validator.SelectorRule

/**
 * Checks the keys of a numeric selector: an exact key is an integer and is not allowed with digit options, and any
 * other key is a category of the selection mode in the region locale.
 */
internal object NumericSelectorKeysRule : SelectorRule {
    override fun findProblems(selector: ResolvedSelector, context: RuleContext): List<String> {
        val options = selector.value?.options
        if (selector.selection != Selection.NUMERIC || options == null) return emptyList()
        val keys = Keys(selector.name, SelectMode.of(options), options.keys.any { it in OptionNames.DIGIT_OPTIONS })
        val rules = keys.mode.pluralRules(context.locale)
        val locale = context.locale.toLanguageTag()
        return selector.literalKeys.mapNotNull { key -> keys.keyProblem(key, rules, locale) }
    }

    // The keys of one selector in [mode]; exact keys are forbidden if the selector has digit options.
    private class Keys(val selector: String, val mode: SelectMode, val hasDigitOptions: Boolean) {
        fun keyProblem(key: String, rules: PluralRules?, locale: String): String? = when {
            Literals.EXACT_KEY.matches(key) && hasDigitOptions -> Problems.Selectors.exactWithDigits(key, selector)
            Literals.EXACT_KEY.matches(key) -> null
            Literals.NUMBER.matches(key) -> Problems.Selectors.exactNotInteger(key, selector)
            rules == null ->
                Problems.Selectors.exactOnly(key = key, selector = selector, option = OptionNames.SELECT, value = SelectMode.EXACT.keyword)
            // Only the plural and ordinal modes have categories, and their keywords name the kind of category.
            key !in rules.keywords -> Problems.Selectors.notCategory(
                key = key,
                selector = selector,
                kind = mode.keyword,
                locale = locale,
                categories = rules.keywords,
            )
            else -> null
        }
    }
}
