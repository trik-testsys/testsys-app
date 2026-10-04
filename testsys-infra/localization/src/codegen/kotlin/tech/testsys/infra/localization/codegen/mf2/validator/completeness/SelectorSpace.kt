package tech.testsys.infra.localization.codegen.mf2.validator.completeness

import com.ibm.icu.text.PluralRules

/**
 * A `.match` selector as seen by [PluralCompleteness].
 *
 * @property name the selector variable name without `$`.
 */
internal sealed interface SelectorSpace {
    val name: String

    /** A `:string` selector. */
    data class Text(override val name: String) : SelectorSpace

    /**
     * A numeric selector.
     *
     * @property rules the plural rules (cardinal or ordinal), or `null` for `select=exact`.
     * @property isIntegerOnly whether the selected value always formats as an integer (`:integer` and its offsets).
     */
    data class Numeric(override val name: String, val rules: PluralRules?, val isIntegerOnly: Boolean) : SelectorSpace
}
