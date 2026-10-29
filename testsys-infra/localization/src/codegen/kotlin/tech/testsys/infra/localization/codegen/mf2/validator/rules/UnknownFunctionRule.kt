package tech.testsys.infra.localization.codegen.mf2.validator.rules

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.Functions
import tech.testsys.infra.localization.codegen.mf2.function.StringFunction
import tech.testsys.infra.localization.codegen.mf2.model.FunctionRef
import tech.testsys.infra.localization.codegen.mf2.model.Occurrence
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedExpression
import tech.testsys.infra.localization.codegen.mf2.validator.OccurrenceRule
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext

/** Rejects a function the codegen does not support; the other checks of such an expression do not run. */
internal object UnknownFunctionRule : OccurrenceRule {
    private const val NAMESPACE_SEPARATOR = ':'

    override fun findProblems(occurrence: Occurrence, context: RuleContext): List<String> {
        val unknown = (occurrence as? ResolvedExpression)?.function as? FunctionRef.Unknown ?: return emptyList()
        return listOf(unknownFunctionProblem(unknown.name))
    }

    private fun unknownFunctionProblem(name: String): String = when {
        name == Functions.ICU_GENDER -> Problems.Functions.duplicate(name, StringFunction.name)
        NAMESPACE_SEPARATOR in name -> Problems.Functions.namespaced(name)
        else -> Problems.Functions.unknown(name)
    }
}
