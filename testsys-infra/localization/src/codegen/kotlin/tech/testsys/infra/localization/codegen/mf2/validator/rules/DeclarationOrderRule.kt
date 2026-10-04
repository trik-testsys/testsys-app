package tech.testsys.infra.localization.codegen.mf2.validator.rules

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.model.Occurrence
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedExpression
import tech.testsys.infra.localization.codegen.mf2.validator.OccurrenceRule
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext

/** Rejects an operand that a later declaration declares. */
internal object DeclarationOrderRule : OccurrenceRule {
    override fun findProblems(occurrence: Occurrence, context: RuleContext): List<String> {
        val reference = (occurrence as? ResolvedExpression)?.operandReference ?: return emptyList()
        return if (reference.isUsedBeforeDeclaration) listOf(Problems.Variables.usedBeforeDeclaration(reference.name)) else emptyList()
    }
}
