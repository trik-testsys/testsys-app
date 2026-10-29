package tech.testsys.infra.localization.codegen.mf2.validator.rules

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.model.MarkupOccurrence
import tech.testsys.infra.localization.codegen.mf2.model.Occurrence
import tech.testsys.infra.localization.codegen.mf2.validator.OccurrenceRule
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext

/** Rejects markup: the API returns plain text and ICU drops markup silently. */
internal object MarkupRule : OccurrenceRule {
    override fun findProblems(occurrence: Occurrence, context: RuleContext): List<String> {
        val markup = occurrence as? MarkupOccurrence ?: return emptyList()
        return listOf(Problems.Syntax.markup(markup.markup.name))
    }
}
