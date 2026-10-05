package tech.testsys.infra.localization.codegen.mf2.validator.term

import com.ibm.icu.text.PluralRules
import com.ibm.icu.util.ULocale
import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.StringFunction
import tech.testsys.infra.localization.codegen.mf2.function.TermFunction
import tech.testsys.infra.localization.codegen.mf2.function.termIdentifier
import tech.testsys.infra.localization.codegen.mf2.model.FunctionCall
import tech.testsys.infra.localization.codegen.mf2.model.InputDeclaration
import tech.testsys.infra.localization.codegen.mf2.model.LiteralKey
import tech.testsys.infra.localization.codegen.mf2.model.Mf2Message
import tech.testsys.infra.localization.codegen.mf2.model.SelectMessage
import tech.testsys.infra.localization.codegen.mf2.model.TextPart
import tech.testsys.infra.localization.codegen.mf2.model.Variant

/**
 * A glossary term of one region.
 *
 * @property cases the case keys of the term.
 * @property cells every explicit `(case, form)` variant.
 */
internal data class TermDefinition(val name: String, val cases: Set<String>, val cells: Set<Pair<String, String>>)

/**
 * The shape of a term message `glossary.<name>` in the region [locale]: it declares `$case` and `$form`, selects on
 * them and has plain-text variants whose form keys are plural categories of the locale or `sg`/`pl`.
 */
internal class TermShape(locale: ULocale) {
    private val forms = PluralRules.forLocale(locale).keywords + setOf(TermFunction.SINGULAR, TermFunction.PLURAL)

    /** Returns the term of the key [key] with the [message], or `null`, and the problems found. */
    fun parseDefinition(key: String, message: Mf2Message): Pair<TermDefinition?, List<String>> {
        val name = key.removePrefix(Glossary.GLOSSARY_PREFIX)
        if (!termIdentifier.matches(name)) return null to listOf(Problems.Terms.GLOSSARY_KEY)
        if (message !is SelectMessage || !isShaped(message)) return null to listOf(SHAPE)
        val problems = message.variants.flatMap(::variantProblems)
        val cells = message.variants.mapNotNull(::cell).toSet()
        val cases = message.variants.mapNotNull { (it.keys.first() as? LiteralKey)?.value }.toSet()
        return TermDefinition(name, cases, cells) to problems
    }

    private fun isShaped(message: SelectMessage): Boolean {
        val inputs = message.declarations.map { declaration ->
            (declaration as? InputDeclaration)?.takeIf { input ->
                input.expression.function == FunctionCall(StringFunction.name, emptyMap()) && input.expression.attributes.isEmpty()
            }?.name
        }
        return inputs.sortedBy { it } == listOf(CASE, FORM) && message.selectors == listOf(CASE, FORM)
    }

    private fun variantProblems(variant: Variant): List<String> {
        val (case, form) = variant.keys
        return listOfNotNull(
            (case as? LiteralKey)?.value?.takeUnless(termIdentifier::matches)?.let(Problems.Terms::caseKey),
            (form as? LiteralKey)?.value?.takeUnless { it in forms }?.let { Problems.Terms.formKey(it, forms.sorted()) },
            Problems.Terms.plainText(keysOf(variant)).takeIf { variant.pattern.parts.any { part -> part !is TextPart } },
        )
    }

    private fun cell(variant: Variant): Pair<String, String>? {
        val (case, form) = variant.keys
        return if (case is LiteralKey && form is LiteralKey) case.value to form.value else null
    }

    private fun keysOf(variant: Variant): String = variant.keys.joinToString(" ") { (it as? LiteralKey)?.value ?: "*" }

    private companion object {
        // The input variables of a term message.
        const val CASE = "case"
        const val FORM = "form"

        val SHAPE = Problems.Terms.shape(CASE, FORM, StringFunction.name)
    }
}
