package tech.testsys.infra.localization.codegen.mf2.validator.term

import tech.testsys.infra.localization.codegen.mf2.model.ResolvedMessage
import tech.testsys.infra.localization.codegen.mf2.validator.MessageRule
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext

/**
 * Checks every well-formed `:term` reference against the glossary of the region; references to a term that has
 * problems of its own are not checked.
 */
internal object TermReferenceRule : MessageRule {
    override fun findProblems(message: ResolvedMessage, context: RuleContext): List<String> = TermReferences.of(message)
        .filter { it.term !in context.glossary.brokenTerms }
        .flatMap(context.glossary::referenceProblems)
}
