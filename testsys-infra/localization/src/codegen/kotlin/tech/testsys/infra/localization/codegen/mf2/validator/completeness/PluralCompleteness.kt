package tech.testsys.infra.localization.codegen.mf2.validator.completeness

import com.ibm.icu.text.PluralRules
import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.Literals
import tech.testsys.infra.localization.codegen.mf2.model.CatchAllKey
import tech.testsys.infra.localization.codegen.mf2.model.LiteralKey
import tech.testsys.infra.localization.codegen.mf2.model.Variant
import tech.testsys.infra.localization.codegen.mf2.model.VariantKey

/**
 * Strict plural completeness of a `.match` message: no reachable plural category may fall back to `*`, and the
 * variant that [icuChoice] picks must render the same pattern as the MF2 choice.
 */
internal class PluralCompleteness(
    private val icuChoice: VariantChoice = VariantChoice.MF2,
    private val maxCombinations: Int = MAX_COMBINATIONS,
) {
    // Every value class of every selector is enumerated: each literal key plus "any other value" for a `:string`
    // selector, each exact key plus each reachable plural category for a numeric one. For every combination the
    // variant is chosen with the MF2 algorithm and, separately, with icuChoice.

    /** Returns the completeness problems of [variants] selected by [selectors]. */
    fun findProblems(selectors: List<SelectorSpace>, variants: List<Variant>): List<String> {
        val classes = selectors.mapIndexed { index, selector -> valueClasses(selector, variants.map { variant -> variant.keys[index] }) }
        classes.fold(1L) { product, values ->
            val count = product * values.size
            if (count > maxCombinations) return listOf(Problems.Completeness.tooManyCombinations(maxCombinations))
            count
        }
        val problems = linkedSetOf<String>()
        combinations(classes).forEach { combination -> problems += combinationProblems(selectors, combination, variants) }
        return problems.toList()
    }

    private fun combinationProblems(selectors: List<SelectorSpace>, combination: List<ValueClass>, variants: List<Variant>): List<String> {
        val chosen = VariantChoice.MF2.choose(combination, variants)
        val icuChosen = icuChoice.choose(combination, variants)
        val isRenderedDifferently = chosen != null && icuChosen != null && chosen.pattern != icuChosen.pattern
        val icuProblem = if (isRenderedDifferently) {
            Problems.Completeness.otherNeverMatched(
                values = combination.map { it.description },
                icuKeys = keysOf(icuChosen),
                keys = keysOf(chosen),
            )
        } else {
            null
        }
        return fallbackProblems(selectors, combination, chosen) + listOfNotNull(icuProblem)
    }

    private fun fallbackProblems(selectors: List<SelectorSpace>, combination: List<ValueClass>, chosen: Variant?): List<String> =
        selectors.indices.mapNotNull { index ->
            val category = combination[index].category
            val isNumeric = (selectors[index] as? SelectorSpace.Numeric)?.rules != null
            if (!isNumeric || category == null || chosen?.keys?.get(index) != CatchAllKey) return@mapNotNull null
            val others = combination.filterIndexed { other, _ -> other != index }.map { value -> value.description }
            Problems.Completeness.fallback(category = category, selector = selectors[index].name, others = others)
        }

    private fun valueClasses(selector: SelectorSpace, keys: List<VariantKey>): List<ValueClass> {
        val literals = keys.filterIsInstance<LiteralKey>().map { it.value }.distinct()
        return when (selector) {
            is SelectorSpace.Text ->
                literals.map { TextValue(key = it, selector = selector.name) } + TextValue(key = null, selector = selector.name)
            is SelectorSpace.Numeric -> numericClasses(selector, literals)
        }
    }

    private fun numericClasses(selector: SelectorSpace.Numeric, literals: List<String>): List<ValueClass> {
        val exact = literals.filter { Literals.EXACT_KEY.matches(it) }
        val rules = selector.rules
            ?: return exact.map { ExactValue(key = it, category = null, selector = selector.name) } + OtherExactValue(selector.name)
        val exactValues = exact.map { it.toDouble() }.toSet()
        val categories = rules.keywords.sorted().filter { isReachable(rules, it, selector.isIntegerOnly, exactValues) }
        val exactClasses = exact.map { ExactValue(key = it, category = rules.select(it.toDouble()), selector = selector.name) }
        return exactClasses + categories.map { CategoryValue(category = it, selector = selector.name) }
    }

    // A category is reachable if it has a value not covered by exact keys: a limited integer category such as
    // English `one` = {1} is fully covered by the exact key `1`.
    private fun isReachable(rules: PluralRules, category: String, isIntegerOnly: Boolean, exact: Set<Double>): Boolean {
        val integerValues = rules.getAllKeywordValues(category)
        val isIntegerReachable = integerValues?.any { it !in exact } ?: (category in integerCategories(rules))
        return isIntegerReachable || !isIntegerOnly && category in decimalCategories(rules)
    }

    private fun combinations(classes: List<List<ValueClass>>): Sequence<List<ValueClass>> =
        classes.fold(sequenceOf(emptyList())) { prefixes, values ->
            prefixes.flatMap { prefix -> values.asSequence().map { value -> prefix + value } }
        }

    private fun keysOf(variant: Variant): String = variant.keys.joinToString(" ") { key ->
        when (key) {
            CatchAllKey -> "*"
            is LiteralKey -> key.value
        }
    }

    private companion object {
        const val MAX_COMBINATIONS = 10_000
    }
}
