package tech.testsys.infra.localization.codegen.mf2.validator

import com.ibm.icu.util.ULocale
import tech.testsys.infra.localization.codegen.mf2.model.Mf2Message
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedMessage
import tech.testsys.infra.localization.codegen.mf2.parser.Mf2Parser
import tech.testsys.infra.localization.codegen.mf2.validator.term.Glossary
import tech.testsys.infra.localization.codegen.mf2.validator.term.TermDefinition

/** The locale of the region RU. */
internal val RU_LOCALE: ULocale = ULocale.forLanguageTag("ru-RU")

/** Returns the context of the region RU whose glossary holds [terms]. */
internal fun ruContext(terms: Map<String, TermDefinition> = emptyMap()): RuleContext =
    RuleContext(RU_LOCALE, Glossary(RU_LOCALE, terms, brokenTerms = emptySet(), patterns = emptyMap()))

/** Returns the problems this rule finds in the MF2 [message] in [context]. */
internal fun MessageRule.problemsIn(message: Mf2Message, context: RuleContext = ruContext()): List<String> =
    findProblems(Resolver.resolve(message), context)

/** Returns the problems this rule finds in the MF2 message [source] in [context]. */
internal fun MessageRule.problemsIn(source: String, context: RuleContext = ruContext()): List<String> =
    problemsIn(Mf2Parser.parse(source), context)

/** Returns the problems this rule finds in the declarations and patterns of the MF2 [message] in [context]. */
internal fun OccurrenceRule.problemsIn(message: Mf2Message, context: RuleContext = ruContext()): List<String> =
    EachOccurrence(ResolvedMessage::declarationParts, listOf(this)).problemsIn(message, context) +
        EachOccurrence(ResolvedMessage::patternParts, listOf(this)).problemsIn(message, context)

/** Returns the problems this rule finds in the declarations and patterns of the MF2 message [source] in [context]. */
internal fun OccurrenceRule.problemsIn(source: String, context: RuleContext = ruContext()): List<String> =
    problemsIn(Mf2Parser.parse(source), context)

/** Returns the problems this rule finds in the selectors of the MF2 message [source] in [context]. */
internal fun SelectorRule.problemsIn(source: String, context: RuleContext = ruContext()): List<String> =
    EachSelector(listOf(this)).problemsIn(source, context)
