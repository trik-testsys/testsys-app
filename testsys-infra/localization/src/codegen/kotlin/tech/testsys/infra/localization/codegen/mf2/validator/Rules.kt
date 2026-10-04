package tech.testsys.infra.localization.codegen.mf2.validator

import tech.testsys.infra.localization.codegen.mf2.icu.CompactDisplayRule
import tech.testsys.infra.localization.codegen.mf2.icu.DateOptionPairRule
import tech.testsys.infra.localization.codegen.mf2.icu.LeakingArgumentsRule
import tech.testsys.infra.localization.codegen.mf2.icu.PrecisionOptionsRule
import tech.testsys.infra.localization.codegen.mf2.icu.ReannotationLimitsRule
import tech.testsys.infra.localization.codegen.mf2.icu.SkeletonOptionsRule
import tech.testsys.infra.localization.codegen.mf2.icu.TimePrecisionRule
import tech.testsys.infra.localization.codegen.mf2.icu.UnitUsageRule
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedMessage
import tech.testsys.infra.localization.codegen.mf2.validator.rules.DateZoneRule
import tech.testsys.infra.localization.codegen.mf2.validator.rules.DeclarationOrderRule
import tech.testsys.infra.localization.codegen.mf2.validator.rules.MarkupRule
import tech.testsys.infra.localization.codegen.mf2.validator.rules.NumericSelectorKeysRule
import tech.testsys.infra.localization.codegen.mf2.validator.rules.OperandRule
import tech.testsys.infra.localization.codegen.mf2.validator.rules.OptionCombinationRule
import tech.testsys.infra.localization.codegen.mf2.validator.rules.OptionsRule
import tech.testsys.infra.localization.codegen.mf2.validator.rules.PluralCompletenessRule
import tech.testsys.infra.localization.codegen.mf2.validator.rules.ReannotationRule
import tech.testsys.infra.localization.codegen.mf2.validator.rules.RequiredOptionsRule
import tech.testsys.infra.localization.codegen.mf2.validator.rules.SelectorAnnotationRule
import tech.testsys.infra.localization.codegen.mf2.validator.rules.StringSelectorKeysRule
import tech.testsys.infra.localization.codegen.mf2.validator.rules.UnknownFunctionRule
import tech.testsys.infra.localization.codegen.mf2.validator.rules.UnusedInputRule
import tech.testsys.infra.localization.codegen.mf2.validator.rules.VariableNamesRule
import tech.testsys.infra.localization.codegen.mf2.validator.term.TermReferenceRule

/**
 * The rules of a message in the order their problems are reported: [beforeInference] come before the problems of
 * type inference and [afterInference] after them. Problems follow the message text: the declarations, the
 * selectors, the patterns, then the checks of the message as a whole.
 */
internal object Rules {
    // One expression gets at most one problem of the unknown function; the checks after it need a known one.
    private val occurrenceRules: List<OccurrenceRule> = listOf(
        MarkupRule,
        DeclarationOrderRule,
        UnknownFunctionRule,
        ReannotationRule,
        ReannotationLimitsRule,
        OptionsRule,
        OptionCombinationRule,
        CompactDisplayRule,
        PrecisionOptionsRule,
        DateOptionPairRule,
        TimePrecisionRule,
        SkeletonOptionsRule,
        UnitUsageRule,
        OperandRule,
        RequiredOptionsRule,
    )

    // The key checks need a selector that selects.
    private val selectorRules: List<SelectorRule> = listOf(
        SelectorAnnotationRule,
        StringSelectorKeysRule,
        NumericSelectorKeysRule,
    )

    val beforeInference: List<MessageRule> = listOf(
        EachOccurrence(ResolvedMessage::declarationParts, occurrenceRules),
        EachSelector(selectorRules),
        EachOccurrence(ResolvedMessage::patternParts, occurrenceRules),
        PluralCompletenessRule,
        UnusedInputRule,
        LeakingArgumentsRule,
        VariableNamesRule,
    )

    val afterInference: List<MessageRule> = listOf(DateZoneRule, TermReferenceRule)
}
