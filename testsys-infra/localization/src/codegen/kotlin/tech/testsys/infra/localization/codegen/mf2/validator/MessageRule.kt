package tech.testsys.infra.localization.codegen.mf2.validator

import com.ibm.icu.util.ULocale
import tech.testsys.infra.localization.codegen.mf2.function.Mf2Function
import tech.testsys.infra.localization.codegen.mf2.model.Occurrence
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedExpression
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedMessage
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedSelector
import tech.testsys.infra.localization.codegen.mf2.validator.term.Glossary

/**
 * What the rules know about the region of a message.
 *
 * @property locale the region locale.
 * @property glossary the terms of the region.
 */
internal class RuleContext(val locale: ULocale, val glossary: Glossary)

/** A check of a resolved message; it returns the problems without the `<REGION> / <key>` prefix. */
internal fun interface MessageRule {
    /** Returns the problems of [message] in [context]. */
    fun findProblems(message: ResolvedMessage, context: RuleContext): List<String>
}

/** A check of one expression or markup; [EachOccurrence] applies it in text order. */
internal fun interface OccurrenceRule {
    /** Returns the problems of [occurrence] in [context]. */
    fun findProblems(occurrence: Occurrence, context: RuleContext): List<String>
}

/** An [OccurrenceRule] of the expressions whose function is supported; others have no problems of this rule. */
internal abstract class FunctionRule : OccurrenceRule {
    final override fun findProblems(occurrence: Occurrence, context: RuleContext): List<String> {
        val expression = occurrence as? ResolvedExpression ?: return emptyList()
        val function = expression.knownFunction ?: return emptyList()
        return findProblems(expression, function, context)
    }

    /** Returns the problems of [expression], an expression of [function], in [context]. */
    abstract fun findProblems(expression: ResolvedExpression, function: Mf2Function, context: RuleContext): List<String>
}

/** A check of one `.match` selector; [EachSelector] applies it in selector order. */
internal fun interface SelectorRule {
    /** Returns the problems of [selector] in [context]. */
    fun findProblems(selector: ResolvedSelector, context: RuleContext): List<String>
}

/**
 * Applies [rules] to every expression and markup of the [parts] of a message in text order, so the problems of
 * one placeholder stay together.
 */
internal class EachOccurrence(
    private val parts: (ResolvedMessage) -> List<Occurrence>,
    private val rules: List<OccurrenceRule>,
) : MessageRule {
    override fun findProblems(message: ResolvedMessage, context: RuleContext): List<String> =
        parts(message).flatMap { occurrence -> rules.flatMap { rule -> rule.findProblems(occurrence, context) } }
}

/** Applies [rules] to every selector in order, so the problems of one selector stay together. */
internal class EachSelector(private val rules: List<SelectorRule>) : MessageRule {
    override fun findProblems(message: ResolvedMessage, context: RuleContext): List<String> =
        message.selectors.flatMap { selector -> rules.flatMap { rule -> rule.findProblems(selector, context) } }
}
