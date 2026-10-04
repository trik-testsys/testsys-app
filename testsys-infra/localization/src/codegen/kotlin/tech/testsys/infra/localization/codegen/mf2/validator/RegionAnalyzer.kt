package tech.testsys.infra.localization.codegen.mf2.validator

import tech.testsys.infra.localization.codegen.messageError
import tech.testsys.infra.localization.codegen.mf2.model.Mf2Message
import tech.testsys.infra.localization.codegen.mf2.parser.Mf2Parser
import tech.testsys.infra.localization.codegen.mf2.parser.Mf2SyntaxException
import tech.testsys.infra.localization.codegen.mf2.validator.term.Glossary
import tech.testsys.infra.localization.codegen.signature.RegionMessage
import tech.testsys.infra.localization.codegen.source.PropertiesEntry
import tech.testsys.infra.localization.codegen.source.RegionDefinition

/**
 * The analysis of the messages of one region.
 *
 * @property messages the analysis of every key in file order, or `null` for a key with a syntax error.
 * @property terms the MF2 message of every term, by name.
 */
internal class RegionAnalysis(val messages: Map<String, RegionMessage?>, val terms: Map<String, String>)

/**
 * Analyzes the entries of one region and reports its errors in this order: syntax errors, problems of the terms,
 * problems of the other messages, terms that no message uses.
 */
internal object RegionAnalyzer {

    /** Analyzes the [entries] of [region], reporting problems into [errors]. */
    fun analyze(region: RegionDefinition, entries: List<PropertiesEntry>, errors: MutableList<String>): RegionAnalysis {
        val parsed = entries.associate { entry -> entry.key to parse(region, entry, errors) }
        val glossary = Glossary.collect(region, entries, parsed, errors)
        val analyzer = MessageAnalyzer(RuleContext(region.locale, glossary))
        val analyses = entries
            .filterNot(::isTerm)
            .mapNotNull { entry -> parsed[entry.key]?.let { message -> entry.key to analyzer.analyze(message) } }
            .toMap()
        analyses.forEach { (key, analysis) -> errors += analysis.problems.map { problem -> messageError(region.id, key, problem) } }
        errors += glossary.unusedErrors(region.id, analyses.values.flatMap { it.mentionedTerms }.toSet())
        val messages = entries.associate { entry -> entry.key to regionMessage(entry, analyses) }
        return RegionAnalysis(messages, glossary.patterns)
    }

    private fun parse(region: RegionDefinition, entry: PropertiesEntry, errors: MutableList<String>): Mf2Message? = try {
        Mf2Parser.parse(entry.value)
    } catch (e: Mf2SyntaxException) {
        errors += messageError(region.id, entry.key, e.message.orEmpty())
        null
    }

    // A term has no signature of its own; its message only goes into the generated tables.
    private fun regionMessage(entry: PropertiesEntry, analyses: Map<String, MessageAnalysis>): RegionMessage? {
        if (isTerm(entry)) return RegionMessage(entry.value, emptyMap())
        val analysis = analyses[entry.key] ?: return null
        return RegionMessage(entry.value, analysis.placeholders, analysis.dateZones)
    }

    private fun isTerm(entry: PropertiesEntry): Boolean = entry.key.startsWith(Glossary.GLOSSARY_PREFIX)
}
