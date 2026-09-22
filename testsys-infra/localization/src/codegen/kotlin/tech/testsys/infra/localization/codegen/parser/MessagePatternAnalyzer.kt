package tech.testsys.infra.localization.codegen.parser

import com.ibm.icu.text.MessagePattern
import tech.testsys.infra.localization.codegen.parser.Placeholder.InstantPlaceholder
import tech.testsys.infra.localization.codegen.parser.Placeholder.IntPlaceholder
import tech.testsys.infra.localization.codegen.parser.Placeholder.NumberPlaceholder
import tech.testsys.infra.localization.codegen.parser.Placeholder.SelectPlaceholder
import tech.testsys.infra.localization.codegen.parser.Placeholder.StringPlaceholder

/**
 * Extracts placeholders from a single ICU MessageFormat pattern.
 *
 * Walks ICU's flat [MessagePattern] part array and collapses every argument
 * occurrence into a [Placeholder]. Repeated names within one pattern (e.g.
 * `{n, plural, …}` referenced twice) are reduced via [mergePlaceholders] using
 * the same compatibility rules applied across regions.
 */
internal class MessagePatternAnalyzer {

    fun analyze(pattern: String): Map<String, Placeholder> {
        val mp = MessagePattern(pattern)
        return (0 until mp.countParts())
            .filter { mp.getPart(it).type == MessagePattern.Part.Type.ARG_START }
            .map { placeholderAt(mp, it) }
            // groupBy preserves first-seen order, so emitted parameter lists
            // follow pattern order rather than alphabetical name order.
            .groupBy(Placeholder::name)
            .mapValues { (_, group) -> group.reduce(::mergePlaceholders) }
    }

    /**
     * Builds a [Placeholder] from the ARG_START at [partIndex].
     *
     * ICU lays each argument out as a flat run of parts:
     * `[ARG_START][ARG_NAME][optional ARG_TYPE]…[ARG_LIMIT]`. The substring of the
     * ARG_NAME part is the placeholder identifier; how the rest is interpreted
     * depends on [MessagePattern.Part.argType].
     */
    private fun placeholderAt(mp: MessagePattern, partIndex: Int): Placeholder {
        val argType = mp.getPart(partIndex).argType
        val nameIdx = partIndex + 1
        val name = mp.getSubstring(mp.getPart(nameIdx))
        return when (argType) {
            // Bare `{name}` — caller passes any value, ICU coerces via toString().
            MessagePattern.ArgType.NONE -> StringPlaceholder(name)

            MessagePattern.ArgType.SIMPLE ->
                simplePlaceholder(name, mp.getSubstring(mp.getPart(nameIdx + 1)))

            // Plural and select-ordinal rules dispatch on integer values.
            MessagePattern.ArgType.PLURAL,
            MessagePattern.ArgType.SELECTORDINAL -> IntPlaceholder(name)

            MessagePattern.ArgType.SELECT ->
                SelectPlaceholder(name, collectSelectVariants(mp, partIndex, nameIdx))

            else -> StringPlaceholder(name)
        }
    }

    private fun simplePlaceholder(name: String, typeStr: String): Placeholder = when (typeStr) {
        "number" -> NumberPlaceholder(name)
        "date", "time" -> InstantPlaceholder(name)
        // RBNF rules ("one", "first") require integer input.
        "spellout", "ordinal" -> IntPlaceholder(name)
        else -> StringPlaceholder(name)
    }

    /**
     * Returns every `{… , select, X {…} Y {…} …}` branch label that belongs to
     * the outer select at [argStartIndex] — i.e. selectors of nested
     * `plural`/`select` arguments are skipped.
     *
     * ICU lays parts out flat, so a naive scan between the outer ARG_START and
     * its ARG_LIMIT also picks up ARG_SELECTOR parts from nested arguments
     * (e.g. the `one/few/many/other` of an inner `{count, plural, …}`). We jump
     * over nested arguments using [MessagePattern.getLimitPartIndex] to keep
     * only the labels of the current level.
     */
    private fun collectSelectVariants(
        mp: MessagePattern,
        argStartIndex: Int,
        nameIdx: Int,
    ): Set<String> {
        val outerLimit = mp.getLimitPartIndex(argStartIndex)
        val variants = sortedSetOf<String>()
        var i = nameIdx + 1
        while (i < outerLimit) {
            val part = mp.getPart(i)
            when (part.type) {
                MessagePattern.Part.Type.ARG_SELECTOR -> variants += mp.getSubstring(part)
                MessagePattern.Part.Type.ARG_START -> i = mp.getLimitPartIndex(i)
                else -> Unit
            }
            i++
        }
        return variants
    }
}

/**
 * Reconciles two [Placeholder]s with the same name, either from different regions
 * or different positions in one pattern.
 *
 * Compatibility rules:
 *  - identical subtypes collapse trivially (SELECT unions its variants);
 *  - NUMBER widens to INT when one side requires integer input — INT is the safer
 *    Kotlin parameter since Int is assignable to Number but not vice versa;
 *  - all other pairings are an error (e.g. STRING vs INSTANT).
 */
internal fun mergePlaceholders(a: Placeholder, b: Placeholder): Placeholder {
    require(a.name == b.name) { "merge expects same name" }
    return when {
        a is SelectPlaceholder && b is SelectPlaceholder ->
            SelectPlaceholder(a.name, a.selectVariants + b.selectVariants)
        a::class == b::class -> a
        a is NumberPlaceholder && b is IntPlaceholder -> b
        a is IntPlaceholder && b is NumberPlaceholder -> a
        else -> error(
            "conflicting kinds for arg '${a.name}': " +
                "${a::class.simpleName} vs ${b::class.simpleName}"
        )
    }
}
