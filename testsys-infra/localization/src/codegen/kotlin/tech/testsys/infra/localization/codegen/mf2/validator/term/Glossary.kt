package tech.testsys.infra.localization.codegen.mf2.validator.term

import com.ibm.icu.text.PluralRules
import com.ibm.icu.util.ULocale
import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.messageError
import tech.testsys.infra.localization.codegen.mf2.function.TermFunction
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames
import tech.testsys.infra.localization.codegen.mf2.model.Mf2Message
import tech.testsys.infra.localization.codegen.mf2.validator.completeness.integerCategories
import tech.testsys.infra.localization.codegen.source.PropertiesEntry
import tech.testsys.infra.localization.codegen.source.RegionDefinition

/**
 * The glossary terms of one region and the checks of the references to them. `GLOSSARY_PREFIX` is the key prefix
 * reserved for terms; `collect` builds the glossary of a region from its entries.
 *
 * @property terms the terms with a valid name and shape, by name.
 * @property brokenTerms the names of the terms that have problems; references to them are not checked.
 * @property patterns the MF2 message of every term, by name.
 */
internal class Glossary(
    locale: ULocale,
    val terms: Map<String, TermDefinition>,
    val brokenTerms: Set<String>,
    val patterns: Map<String, String>,
) {
    private val countedForms = integerCategories(PluralRules.forLocale(locale)).sorted()

    /** Returns the problems of [reference] against the terms. */
    fun referenceProblems(reference: TermReference): List<String> {
        val term = terms[reference.term] ?: return listOf(Problems.Terms.unknownTerm(TermFunction.name, reference.term))
        if (reference.case !in term.cases) {
            val known = term.cases.sorted()
            val problem = Problems.Terms.unknownCase(
                function = TermFunction.name,
                option = OptionNames.CASE,
                case = reference.case,
                term = term.name,
                known = known,
            )
            return listOf(problem)
        }
        val forms = reference.number?.let(::listOf) ?: countedForms
        return forms
            .filter { (reference.case to it) !in term.cells }
            .map { form -> Problems.Terms.missingVariant(term = term.name, case = reference.case, form = form) }
    }

    /** Returns the errors of the terms of [region] that no message mentions among [mentioned]. */
    fun unusedErrors(region: String, mentioned: Set<String>): List<String> =
        (terms.keys - mentioned).map { name -> messageError(region, GLOSSARY_PREFIX + name, Problems.Terms.unused(name)) }

    companion object {
        const val GLOSSARY_PREFIX = "glossary."

        /**
         * Collects the terms among [entries] of [region] with their [parsed] messages (`null` for a syntax error),
         * reporting the problems of their shape into [errors].
         */
        fun collect(
            region: RegionDefinition,
            entries: List<PropertiesEntry>,
            parsed: Map<String, Mf2Message?>,
            errors: MutableList<String>,
        ): Glossary {
            val shape = TermShape(region.locale)
            val terms = linkedMapOf<String, TermDefinition>()
            val broken = mutableSetOf<String>()
            val patterns = sortedMapOf<String, String>()
            entries.filter { it.key.startsWith(GLOSSARY_PREFIX) }.forEach { entry ->
                val name = entry.key.removePrefix(GLOSSARY_PREFIX)
                val (definition, problems) = parsed[entry.key]?.let { shape.parseDefinition(entry.key, it) } ?: (null to emptyList())
                errors += problems.map { messageError(region.id, entry.key, it) }
                if (definition == null || problems.isNotEmpty()) broken += name
                definition?.let { terms[name] = it }
                patterns[name] = entry.value
            }
            return Glossary(region.locale, terms, broken, patterns)
        }
    }
}
