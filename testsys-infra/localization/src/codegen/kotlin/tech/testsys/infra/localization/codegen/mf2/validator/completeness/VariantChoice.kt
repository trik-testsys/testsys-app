package tech.testsys.infra.localization.codegen.mf2.validator.completeness

import tech.testsys.infra.localization.codegen.mf2.model.CatchAllKey
import tech.testsys.infra.localization.codegen.mf2.model.LiteralKey
import tech.testsys.infra.localization.codegen.mf2.model.Variant

/**
 * The MF2 variant choice: an exact key beats a category key, which beats `*`, and the first selector is the most
 * significant. [isSkipped] tells which literal keys an implementation never matches for a value class; `MF2` is the
 * choice of the MF2 spec, which skips no key.
 */
internal class VariantChoice(private val isSkipped: (key: String, value: ValueClass) -> Boolean) {

    /** Returns the variant chosen for one value class per selector in [combination], or `null` if none matches. */
    fun choose(combination: List<ValueClass>, variants: List<Variant>): Variant? = variants
        .mapNotNull { variant -> preference(variant, combination)?.let { ranks -> variant to ranks } }
        .minWithOrNull { (_, a), (_, b) -> compareLexicographically(a, b) }
        ?.first

    // Preference per position: 0 = exact key, 1 = category key, 2 = `*`; `null` if the variant does not match.
    private fun preference(variant: Variant, combination: List<ValueClass>): List<Int>? = variant.keys.mapIndexed { index, key ->
        val value = combination[index]
        when (key) {
            CatchAllKey -> CATCH_ALL
            is LiteralKey -> when {
                key.value !in value.matching || isSkipped(key.value, value) -> return null
                key.value == value.category -> CATEGORY
                else -> EXACT
            }
        }
    }

    private fun compareLexicographically(a: List<Int>, b: List<Int>): Int =
        a.zip(b).firstOrNull { (x, y) -> x != y }?.let { (x, y) -> x.compareTo(y) } ?: 0

    companion object {
        private const val EXACT = 0
        private const val CATEGORY = 1
        private const val CATCH_ALL = 2

        val MF2 = VariantChoice { _, _ -> false }
    }
}
