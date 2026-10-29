package tech.testsys.infra.localization.codegen.mf2.validator.completeness

import tech.testsys.infra.localization.codegen.Problems

/**
 * A class of values of one selector that every variant matches the same way.
 *
 * @property matching the literal keys a value of the class matches.
 * @property category the plural category of the values, or `null` if they have none.
 * @property isNumeric whether the values belong to a numeric selector.
 * @property description how an error names the class.
 */
internal sealed interface ValueClass {
    val matching: Set<String>
    val category: String?
    val isNumeric: Boolean
    val description: String
}

/** A value of a `:string` selector: the literal [key], or any other value if [key] is `null`. */
internal data class TextValue(val key: String?, val selector: String) : ValueClass {
    override val matching: Set<String> = setOfNotNull(key)
    override val category: String? = null
    override val isNumeric: Boolean = false
    override val description: String =
        if (key == null) Problems.Completeness.anyOtherValue(selector) else Problems.Completeness.equalTo(selector, key)
}

/** The value of the exact numeric [key], which also matches its [category]. */
internal data class ExactValue(val key: String, override val category: String?, val selector: String) : ValueClass {
    override val matching: Set<String> = setOfNotNull(key, category)
    override val isNumeric: Boolean = true
    override val description: String = Problems.Completeness.equalTo(selector, key)
}

/** The values of the plural [category] that no exact key covers. */
internal data class CategoryValue(override val category: String, val selector: String) : ValueClass {
    override val matching: Set<String> = setOf(category)
    override val isNumeric: Boolean = true
    override val description: String = Problems.Completeness.inCategory(selector, category)
}

/** Any value other than the exact keys of a `select=exact` selector. */
internal data class OtherExactValue(val selector: String) : ValueClass {
    override val matching: Set<String> = emptySet()
    override val category: String? = null
    override val isNumeric: Boolean = true
    override val description: String = Problems.Completeness.anyOtherValue(selector)
}
