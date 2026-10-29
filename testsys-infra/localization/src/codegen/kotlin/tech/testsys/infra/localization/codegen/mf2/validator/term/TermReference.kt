package tech.testsys.infra.localization.codegen.mf2.validator.term

import tech.testsys.infra.localization.codegen.mf2.function.TermFunction
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.CASE
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.NAME
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.NUMBER
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedExpression
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedMessage

/**
 * A well-formed `:term` reference in a message.
 *
 * @property number the `number=sg|pl` literal of a reference without an operand, or `null` for a counted reference.
 */
internal data class TermReference(val term: String, val case: String, val number: String?)

/** The `:term` expressions of a message: their references and the terms they mention. */
internal object TermReferences {

    /** Returns the well-formed references of [message] in text order. */
    fun of(message: ResolvedMessage): List<TermReference> = termExpressions(message).mapNotNull(::reference)

    /** Returns the names of the terms [message] mentions, including in malformed references. */
    fun mentioned(message: ResolvedMessage): Set<String> =
        termExpressions(message).mapNotNull { TermFunction.literal(it.ownValues, NAME) }.toSet()

    private fun termExpressions(message: ResolvedMessage): List<ResolvedExpression> =
        message.expressions.filter { it.knownFunction == TermFunction }

    private fun reference(expression: ResolvedExpression): TermReference? {
        val own = expression.ownValues
        val operand = expression.source.operand
        val term = TermFunction.literal(own, NAME) ?: return null
        val case = TermFunction.literal(own, CASE) ?: return null
        if (TermFunction.operandProblems(operand, own, expression.value.options).isNotEmpty()) return null
        return TermReference(term = term, case = case, number = TermFunction.literal(own, NUMBER).takeIf { operand == null })
    }
}
