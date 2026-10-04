package tech.testsys.infra.localization.codegen.mf2.validator.rules

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.model.ReferenceKind
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedMessage
import tech.testsys.infra.localization.codegen.mf2.validator.MessageRule
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext

/** Rejects an `.input` that no expression, option or selector uses. */
internal object UnusedInputRule : MessageRule {
    private val uses = setOf(ReferenceKind.OPERAND, ReferenceKind.OPTION_VALUE, ReferenceKind.SELECTOR)

    override fun findProblems(message: ResolvedMessage, context: RuleContext): List<String> {
        val used = message.references.filter { it.kind in uses }.map { it.name }.toSet()
        return message.declarations
            .filter { it.isInput && it.name !in used }
            .map { Problems.Variables.unusedInput(it.name) }
    }
}
