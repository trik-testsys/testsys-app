package tech.testsys.infra.localization.codegen.mf2.validator.rules

import com.ibm.icu.util.ULocale
import tech.testsys.infra.localization.codegen.mf2.function.Selection
import tech.testsys.infra.localization.codegen.mf2.function.option.SelectMode
import tech.testsys.infra.localization.codegen.mf2.icu.OtherKeySelection
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedMessage
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedSelector
import tech.testsys.infra.localization.codegen.mf2.model.SelectMessage
import tech.testsys.infra.localization.codegen.mf2.validator.MessageRule
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext
import tech.testsys.infra.localization.codegen.mf2.validator.completeness.PluralCompleteness
import tech.testsys.infra.localization.codegen.mf2.validator.completeness.SelectorSpace
import tech.testsys.infra.localization.codegen.mf2.validator.completeness.VariantChoice

/** Runs the strict plural completeness check on a `.match` message whose selectors all select. */
internal object PluralCompletenessRule : MessageRule {
    private val completeness = PluralCompleteness(
        icuChoice = VariantChoice { key, value -> OtherKeySelection.isSkipped(key, value.category, value.isNumeric) },
    )

    override fun findProblems(message: ResolvedMessage, context: RuleContext): List<String> {
        val select = message.source as? SelectMessage ?: return emptyList()
        val spaces = message.selectors.mapNotNull { space(it, context.locale) }
        if (spaces.size != message.selectors.size) return emptyList()
        return completeness.findProblems(spaces, select.variants)
    }

    private fun space(selector: ResolvedSelector, locale: ULocale): SelectorSpace? {
        val value = selector.value ?: return null
        return when (selector.selection) {
            Selection.TEXT -> SelectorSpace.Text(selector.name)
            Selection.NUMERIC -> {
                val rules = SelectMode.of(value.options).pluralRules(locale)
                SelectorSpace.Numeric(selector.name, rules, value.isIntegerOperand)
            }
            null -> null
        }
    }
}
