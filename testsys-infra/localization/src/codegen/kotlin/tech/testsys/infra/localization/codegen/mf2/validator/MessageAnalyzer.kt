package tech.testsys.infra.localization.codegen.mf2.validator

import tech.testsys.infra.localization.codegen.mf2.model.Mf2Message
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedMessage
import tech.testsys.infra.localization.codegen.mf2.validator.term.TermReferences
import tech.testsys.infra.localization.codegen.signature.DateZoneSpec
import tech.testsys.infra.localization.codegen.signature.Placeholder
import tech.testsys.infra.localization.codegen.signature.TypeInference

/**
 * The result of analyzing one message of one region.
 *
 * @property placeholders the typed arguments in first-use order.
 * @property dateZones the zone of every date argument that does not use the context zone.
 * @property mentionedTerms the names of all terms the message refers to, including in invalid references.
 * @property problems the problems found, without the `<REGION> / <key>` prefix.
 */
internal data class MessageAnalysis(
    val placeholders: Map<String, Placeholder>,
    val dateZones: Map<String, DateZoneSpec>,
    val mentionedTerms: Set<String>,
    val problems: List<String>,
)

/** Analyzes the non-glossary messages of one region: resolve, check with [Rules], infer the argument types. */
internal class MessageAnalyzer(private val context: RuleContext) {

    /** Analyzes [message]. */
    fun analyze(message: Mf2Message): MessageAnalysis {
        val resolved = Resolver.resolve(message)
        val inference = TypeInference.infer(resolved)
        val problems = ruleProblems(Rules.beforeInference, resolved) + inference.problems + ruleProblems(Rules.afterInference, resolved)
        return MessageAnalysis(inference.placeholders, inference.dateZones, TermReferences.mentioned(resolved), problems.distinct())
    }

    private fun ruleProblems(rules: List<MessageRule>, message: ResolvedMessage): List<String> =
        rules.flatMap { it.findProblems(message, context) }
}
